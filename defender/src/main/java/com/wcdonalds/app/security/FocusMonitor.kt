package com.wcdonalds.app.security

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Monitors window focus changes to detect potential overlay activity.
 *
 * ## How it works
 * When an overlay is displayed on top of an Activity, the Activity may lose
 * window focus (depending on the overlay's flags). By monitoring
 * [android.app.Activity.onWindowFocusChanged], we can detect when the app
 * loses focus unexpectedly — which may indicate an overlay is present.
 *
 * ## Limitations
 * - Not all overlays cause focus loss (FLAG_NOT_FOCUSABLE overlays don't)
 * - Normal events (notifications, system dialogs) also cause focus loss
 * - Should be used as a supplementary signal, not a primary defense
 *
 * ## Integration
 * Override `onWindowFocusChanged()` in your Activity and call [onFocusChanged]:
 * ```
 * override fun onWindowFocusChanged(hasFocus: Boolean) {
 *     super.onWindowFocusChanged(hasFocus)
 *     FocusMonitor.onFocusChanged(hasFocus)
 * }
 * ```
 */
object FocusMonitor {

    private const val TAG = "FocusMonitor"

    /** Whether the app currently has window focus. */
    var hasFocus by mutableStateOf(true)
        private set

    /** Whether a suspicious focus loss has been detected recently. */
    var suspiciousFocusLoss by mutableStateOf(false)
        private set

    /** Timestamp of the most recent focus loss. */
    var lastFocusLossTime: Long = 0L
        private set

    /**
     * Called when window focus changes. Updates state and logs the event.
     *
     * @param hasFocus true if the window gained focus, false if lost.
     */
    fun onFocusChanged(hasFocus: Boolean) {
        this.hasFocus = hasFocus

        if (!hasFocus) {
            lastFocusLossTime = System.currentTimeMillis()
            suspiciousFocusLoss = true
            Log.w(TAG, "Window focus lost — potential overlay detected at $lastFocusLossTime")
        } else {
            // Reset after a delay to prevent false positives from brief focus changes
            suspiciousFocusLoss = false
            Log.d(TAG, "Window focus regained")
        }
    }

    /** Resets the focus monitor state. */
    fun reset() {
        hasFocus = true
        suspiciousFocusLoss = false
    }
}
