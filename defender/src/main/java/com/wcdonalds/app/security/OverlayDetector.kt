package com.wcdonalds.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Detects whether other apps on the device have overlay permission.
 *
 * ## Detection approaches
 *
 * ### 1. Settings.canDrawOverlays (self-check)
 * Only reports whether the CALLING app has overlay permission — not useful
 * for detecting OTHER apps. Included for completeness.
 *
 * ### 2. PackageManager scan
 * Queries installed packages for those that request SYSTEM_ALERT_WINDOW.
 * This is an approximation: having the permission doesn't mean the app
 * is currently drawing an overlay, but it indicates potential risk.
 *
 * ### Limitations
 * - Cannot detect if an overlay is CURRENTLY being drawn (no API for this)
 * - System apps (keyboard, system UI) also request overlay permission
 * - On Android 11+, package visibility restrictions may hide some apps
 */
object OverlayDetector {

    private const val TAG = "OverlayDetector"

    /**
     * Checks if ANY non-system app on the device has overlay permission.
     *
     * Scans installed packages and filters out known system packages.
     * Returns a list of suspicious apps that could potentially draw overlays.
     *
     * @param context Application context.
     * @return List of package names with overlay permission (excluding known system apps).
     */
    fun getAppsWithOverlayPermission(context: Context): List<String> {
        val pm = context.packageManager
        val overlayApps = mutableListOf<String>()

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

            for (pkg in packages) {
                val permissions = pkg.requestedPermissions ?: continue
                if (android.Manifest.permission.SYSTEM_ALERT_WINDOW in permissions) {
                    // Filter out known system packages
                    if (!isKnownSystemPackage(pkg.packageName)) {
                        overlayApps.add(pkg.packageName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning packages: ${e.message}")
        }

        return overlayApps
    }

    /**
     * Checks if a specific package is a known system app that legitimately
     * uses overlay permission (keyboards, system UI, etc.).
     */
    private fun isKnownSystemPackage(packageName: String): Boolean {
        val systemPrefixes = listOf(
            "com.android.",
            "com.google.android.",
            "com.samsung.",
            "com.sec.",
            "com.lge.",
            "com.huawei.",
        )
        return systemPrefixes.any { packageName.startsWith(it) }
    }

    /**
     * Quick check: returns true if any non-system app has overlay permission.
     * Use this for simple "show warning or not" decisions.
     */
    fun hasOverlayRisk(context: Context): Boolean {
        return getAppsWithOverlayPermission(context).isNotEmpty()
    }
}
