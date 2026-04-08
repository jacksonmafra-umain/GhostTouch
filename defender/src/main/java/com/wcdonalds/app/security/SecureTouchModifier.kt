package com.wcdonalds.app.security

import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter

/**
 * Compose Modifier that blocks touch events when an overlay is detected.
 *
 * ## How it works
 * When another app draws an overlay on top of this app, Android sets the
 * [MotionEvent.FLAG_WINDOW_IS_OBSCURED] flag on all touch events delivered
 * to the underlying app. This modifier checks for that flag and rejects
 * the touch if present.
 *
 * This is the Compose equivalent of the XML attribute `filterTouchesWhenObscured="true"`.
 *
 * ## Why this is the most important defense
 * - Simple to implement (one modifier)
 * - Catches ALL overlay types (visible, transparent, tapjacking)
 * - Works regardless of how the overlay is created
 * - The flag is set by the OS, so it cannot be spoofed by the attacker
 *
 * ## Trade-offs
 * - May block legitimate overlays (chat bubbles, screen recorders)
 * - Users may not understand why touches are being ignored
 * - Should be paired with a warning banner explaining the situation
 *
 * @see OverlayDetector for checking overlay permission status
 * @see OverlayWarningBanner for user-facing warning UI
 */
object SecureTouchModifier {

    /** Tracks whether an obscured touch was recently detected. */
    var isObscured by mutableStateOf(false)
        private set

    /**
     * Creates a Modifier that filters out touches when the window is obscured.
     *
     * Usage:
     * ```
     * TextField(
     *     modifier = Modifier
     *         .then(SecureTouchModifier.filterObscuredTouches())
     * )
     * ```
     *
     * @param enabled Whether touch filtering is active. When false, all touches pass through.
     * @param onObscuredTouch Callback when an obscured touch is blocked.
     * @return A Modifier that filters obscured touch events.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun filterObscuredTouches(
        enabled: Boolean = true,
        onObscuredTouch: (() -> Unit)? = null
    ): Modifier {
        if (!enabled) return Modifier

        return Modifier.pointerInteropFilter { event ->
            // Check if the FLAG_WINDOW_IS_OBSCURED flag is set
            // This flag indicates another window is partially or fully covering this one
            val obscured = (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0

            if (obscured) {
                isObscured = true
                onObscuredTouch?.invoke()
                // Return true = consume the event (block it)
                true
            } else {
                isObscured = false
                // Return false = don't consume, let it pass through normally
                false
            }
        }
    }
}
