package com.ghosttouch.common.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Utility for checking and requesting overlay-related permissions.
 *
 * Android overlay attacks require two key permissions:
 * 1. SYSTEM_ALERT_WINDOW — allows drawing over other apps
 * 2. PACKAGE_USAGE_STATS — allows detecting which app is in the foreground
 *
 * Both require manual user action in system settings (cannot be granted via runtime prompt).
 */
object OverlayPermissionHelper {

    /**
     * Checks whether the app has permission to draw overlays over other apps.
     *
     * On API 23+, this requires the user to explicitly grant the SYSTEM_ALERT_WINDOW
     * permission via Settings > Apps > Special access > Display over other apps.
     *
     * @param context Application or Activity context.
     * @return true if overlay permission is granted.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks whether the app has usage stats access.
     *
     * This permission allows querying UsageStatsManager to determine which app is
     * currently in the foreground — essential for targeting the overlay to appear
     * only when the victim app is active.
     *
     * @param context Application or Activity context.
     * @return true if usage stats access is granted.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Creates an intent to open the system overlay permission settings screen.
     * The user must manually toggle the permission for the calling app.
     */
    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Creates an intent to open the usage access settings screen.
     * The user must manually enable usage access for the calling app.
     */
    fun usageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
}
