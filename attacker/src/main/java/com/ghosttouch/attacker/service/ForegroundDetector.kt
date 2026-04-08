package com.ghosttouch.attacker.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Detects which app is currently in the foreground using UsageStatsManager.
 *
 * ## How it works
 * Android does not expose a direct API to query the current foreground app.
 * Instead, we use [UsageStatsManager.queryEvents] to scan recent usage events
 * and find the most recent MOVE_TO_FOREGROUND event. This reveals which app
 * the user is currently interacting with.
 *
 * ## Permission requirement
 * Requires the PACKAGE_USAGE_STATS permission, which the user must manually
 * grant via Settings > Apps > Special access > Usage access.
 *
 * ## OEM considerations
 * Some device manufacturers (Xiaomi MIUI, Samsung OneUI) may restrict event
 * granularity. The [getCurrentForegroundPackage] method includes a fallback
 * to [UsageStatsManager.queryUsageStats] which checks `lastTimeUsed` within
 * a recent window.
 */
class ForegroundDetector(private val context: Context) {

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Returns the package name of the app currently in the foreground.
     *
     * Scans the last 5 seconds of usage events for the most recent
     * MOVE_TO_FOREGROUND event. Falls back to queryUsageStats if no
     * events are found (some OEMs restrict event access).
     *
     * @return Package name of the foreground app, or empty string if detection fails.
     */
    fun getCurrentForegroundPackage(): String {
        // Primary approach: queryEvents for fine-grained foreground detection
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - QUERY_WINDOW_MS

        try {
            val events = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var foregroundPackage = ""

            // Iterate all events; the last MOVE_TO_FOREGROUND is the current app
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundPackage = event.packageName
                }
            }

            if (foregroundPackage.isNotEmpty()) {
                return foregroundPackage
            }
        } catch (_: SecurityException) {
            // Usage stats permission not granted — fall through to fallback
        }

        // Fallback: queryUsageStats with daily interval, check lastTimeUsed
        return fallbackDetection(endTime)
    }

    /**
     * Fallback detection using queryUsageStats when queryEvents is restricted.
     *
     * Queries daily usage stats and finds the app with the most recent
     * `lastTimeUsed` value within the last 2 seconds.
     */
    private fun fallbackDetection(currentTime: Long): String {
        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - FALLBACK_WINDOW_MS,
                currentTime
            )

            return stats
                ?.filter { it.lastTimeUsed > currentTime - RECENCY_THRESHOLD_MS }
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
                ?: ""
        } catch (_: SecurityException) {
            return ""
        }
    }

    companion object {
        /** Time window for queryEvents scan (5 seconds). */
        private const val QUERY_WINDOW_MS = 5_000L

        /** Time window for fallback queryUsageStats (1 day). */
        private const val FALLBACK_WINDOW_MS = 24 * 60 * 60 * 1000L

        /** Recency threshold for fallback — app must have been used in last 2 seconds. */
        private const val RECENCY_THRESHOLD_MS = 2_000L
    }
}
