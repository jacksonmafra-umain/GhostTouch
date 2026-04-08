package com.ghosttouch.attacker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ghosttouch.attacker.R

/**
 * Creates and manages the notification required for the foreground service.
 *
 * Android 8.0 (API 26) and above require foreground services to display a
 * persistent notification. This notification is visible in the status bar and
 * notification drawer, providing transparency about background activity.
 *
 * In a real malicious app, this notification would be designed to look innocuous
 * (e.g., "Game running in background"). For our demo, we label it clearly.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "ghost_overlay_channel"
    private const val CHANNEL_NAME = "Overlay Service"
    const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel (required on API 26+).
     * Must be called before starting the foreground service.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Low importance = no sound, minimal visibility
        ).apply {
            description = "Background overlay monitoring service"
            setShowBadge(false)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the persistent notification for the foreground service.
     *
     * In a real attack scenario, this would be disguised as something harmless
     * like "Game synchronizing..." or "Checking for updates...".
     *
     * @return A notification suitable for [android.app.Service.startForeground].
     */
    fun buildServiceNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GhostTouch Demo")
            .setContentText("Overlay monitoring active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
