package com.ghosttouch.attacker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives BOOT_COMPLETED broadcast to restart the overlay service after reboot.
 *
 * Disguised as "daily rewards check" — the user granted RECEIVE_BOOT_COMPLETED
 * thinking it was for game notifications. In reality, it ensures the malicious
 * service survives device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted — overlay service auto-restart available")
            // In a real attack, this would auto-start the OverlayService.
            // For the demo, we just log that it's possible.
        }
    }
}
