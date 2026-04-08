package com.ghosttouch.attacker.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Transparent tapjacking overlay.
 *
 * ## How tapjacking works
 * Unlike a full fake-UI overlay, tapjacking uses a completely transparent overlay
 * that is invisible to the user. The overlay is displayed with FLAG_NOT_FOCUSABLE,
 * meaning touches pass through to the real app underneath.
 *
 * However, the overlay can still:
 * 1. Observe touch coordinates to map user behavior
 * 2. Position invisible buttons that intercept specific taps
 * 3. Create misleading context (e.g., invisible "Confirm" button over a real "Cancel")
 *
 * This demonstrates that even transparent overlays pose security risks —
 * which is why `filterTouchesWhenObscured` checks for ANY overlay, not just visible ones.
 *
 * @param onTapDetected Callback with (x, y) coordinates when a tap is detected.
 */
@Composable
fun TapjackingOverlay(
    onTapDetected: (x: Float, y: Float) -> Unit
) {
    // Fully transparent overlay — invisible to the user
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0f)
    ) {
        // The overlay itself is invisible, but its presence triggers
        // FLAG_WINDOW_IS_OBSCURED on touch events received by the app underneath.
        // This is what filterTouchesWhenObscured detects.
    }
}
