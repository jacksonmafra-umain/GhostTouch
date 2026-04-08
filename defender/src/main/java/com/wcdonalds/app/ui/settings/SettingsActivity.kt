package com.wcdonalds.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wcdonalds.app.ui.theme.WcColors
import com.wcdonalds.app.ui.theme.WcDonaldsTheme

/**
 * Settings screen with toggles to enable/disable defense mechanisms.
 *
 * This screen allows the demo presenter to toggle defenses on and off
 * to show the difference between a protected and unprotected app.
 * Defense toggles are stored in a shared singleton (DefenseSettings).
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WcDonaldsTheme {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}

/**
 * Singleton holding defense toggle states.
 * In a real app these would be in DataStore/SharedPreferences.
 */
object DefenseSettings {
    var filterObscuredTouches by mutableStateOf(false)
    var detectOverlays by mutableStateOf(false)
    var secureScreens by mutableStateOf(false)
    var focusMonitoring by mutableStateOf(false)
    var biometricAuth by mutableStateOf(false)

    /** Enable all defenses at once for demo purposes. */
    fun enableAll() {
        filterObscuredTouches = true
        detectOverlays = true
        secureScreens = true
        focusMonitoring = true
        biometricAuth = true
    }

    /** Disable all defenses at once for demo purposes. */
    fun disableAll() {
        filterObscuredTouches = false
        detectOverlays = false
        secureScreens = false
        focusMonitoring = false
        biometricAuth = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WcColors.Red,
                    titleContentColor = WcColors.White,
                    navigationIconContentColor = WcColors.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Defense Mechanisms",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            DefenseToggle(
                title = "Filter Obscured Touches",
                description = "Block input when an overlay is detected over this app",
                checked = DefenseSettings.filterObscuredTouches,
                onCheckedChange = { DefenseSettings.filterObscuredTouches = it }
            )

            DefenseToggle(
                title = "Overlay Detection",
                description = "Show warning when apps with overlay permission are detected",
                checked = DefenseSettings.detectOverlays,
                onCheckedChange = { DefenseSettings.detectOverlays = it }
            )

            DefenseToggle(
                title = "Secure Screens (FLAG_SECURE)",
                description = "Prevent screenshots and screen recording on sensitive screens",
                checked = DefenseSettings.secureScreens,
                onCheckedChange = { DefenseSettings.secureScreens = it }
            )

            DefenseToggle(
                title = "Focus Monitoring",
                description = "Detect when app loses focus (potential overlay activity)",
                checked = DefenseSettings.focusMonitoring,
                onCheckedChange = { DefenseSettings.focusMonitoring = it }
            )

            DefenseToggle(
                title = "Biometric Authentication",
                description = "Require biometric verification for payment confirmation",
                checked = DefenseSettings.biometricAuth,
                onCheckedChange = { DefenseSettings.biometricAuth = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { DefenseSettings.enableAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = WcColors.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enable All")
                }
                OutlinedButton(
                    onClick = { DefenseSettings.disableAll() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable All")
                }
            }
        }
    }
}

@Composable
private fun DefenseToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WcColors.GrayDark
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = WcColors.Red)
            )
        }
    }
}
