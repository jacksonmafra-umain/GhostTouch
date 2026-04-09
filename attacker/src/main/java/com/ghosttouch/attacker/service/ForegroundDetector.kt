package com.ghosttouch.attacker.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

/**
 * Detects which app is currently in the foreground using UsageStatsManager.
 *
 * ## Stability mechanism
 * To prevent flickering caused by transient empty results from UsageStatsManager,
 * this detector caches the last known foreground package and only updates it when
 * a new non-empty result is obtained. This prevents false "target left foreground"
 * events caused by polling gaps.
 */
class ForegroundDetector(private val context: Context) {

    companion object {
        private const val TAG = "ForegroundDetector"
        /** Time window for queryEvents scan (10 seconds for wider coverage). */
        private const val QUERY_WINDOW_MS = 10_000L
        /** Time window for fallback queryUsageStats (1 day). */
        private const val FALLBACK_WINDOW_MS = 24 * 60 * 60 * 1000L
        /** Recency threshold for fallback — app must have been used in last 5 seconds. */
        private const val RECENCY_THRESHOLD_MS = 5_000L
    }

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Caches the last known foreground package to prevent flickering
     * from transient empty UsageStatsManager results.
     */
    private var lastKnownForeground: String = ""

    /**
     * Returns the package name of the app currently in the foreground.
     *
     * Uses a caching strategy: only updates the cached result when a
     * non-empty package name is obtained. This prevents flickering caused
     * by transient empty results from UsageStatsManager.
     *
     * @return Package name of the foreground app, or the last known foreground
     *         if current detection returns empty.
     */
    fun getCurrentForegroundPackage(): String {
        val detected = detectForeground()

        if (detected.isNotEmpty()) {
            if (detected != lastKnownForeground) {
                Log.d(TAG, "Foreground changed: $lastKnownForeground -> $detected")
            }
            lastKnownForeground = detected
        } else {
            Log.d(TAG, "Detection returned empty — keeping last known: $lastKnownForeground")
        }

        return lastKnownForeground
    }

    /**
     * Core detection logic — queries UsageStatsManager for the foreground app.
     */
    private fun detectForeground(): String {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - QUERY_WINDOW_MS

        // Primary: queryEvents for fine-grained detection
        try {
            val events = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var foregroundPackage = ""

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                @Suppress("DEPRECATION")
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundPackage = event.packageName
                }
            }

            if (foregroundPackage.isNotEmpty()) {
                return foregroundPackage
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "queryEvents failed: ${e.message}")
        }

        // Fallback: queryUsageStats
        return fallbackDetection(endTime)
    }

    /**
     * Fallback detection using queryUsageStats when queryEvents is restricted.
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
        } catch (e: SecurityException) {
            Log.w(TAG, "queryUsageStats failed: ${e.message}")
            return ""
        }
    }
}
