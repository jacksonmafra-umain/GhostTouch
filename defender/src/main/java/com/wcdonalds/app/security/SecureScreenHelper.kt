package com.wcdonalds.app.security

import android.app.Activity
import android.view.WindowManager

/**
 * Helper for applying FLAG_SECURE to Activity windows.
 *
 * ## What FLAG_SECURE does
 * - Prevents screenshots of the Activity
 * - Prevents screen recording (appears as black/blank)
 * - Prevents the Activity from appearing in the recent apps thumbnail
 *
 * ## Why it matters for overlay defense
 * Even if an overlay successfully captures credentials, FLAG_SECURE prevents
 * the attacker from recording the real app's screen to verify the attack
 * or capture additional information.
 *
 * ## Limitations
 * - Does NOT prevent overlays from being drawn on top
 * - Does NOT prevent input capture via overlays
 * - Should be used in combination with touch filtering, not as a standalone defense
 *
 * ## When to apply
 * Must be called BEFORE `setContentView()` or `setContent {}` in `onCreate()`.
 * Applying it after the content is set may not take effect on all devices.
 */
object SecureScreenHelper {

    /**
     * Applies FLAG_SECURE to an Activity's window.
     * Call this in `onCreate()` before `setContent {}`.
     *
     * @param activity The Activity to secure.
     * @param enabled Whether to apply or remove the flag.
     */
    fun applySecureFlag(activity: Activity, enabled: Boolean) {
        if (enabled) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
