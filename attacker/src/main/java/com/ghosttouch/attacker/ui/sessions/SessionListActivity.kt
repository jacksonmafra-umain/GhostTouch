package com.ghosttouch.attacker.ui.sessions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghosttouch.attacker.capture.SessionRepository
import com.ghosttouch.attacker.exfil.DataExfiltrator
import com.ghosttouch.attacker.ui.theme.GhostColors
import com.ghosttouch.attacker.ui.theme.GhostTouchTheme
import com.ghosttouch.common.model.CaptureSession

/**
 * Displays all captured sessions from overlay attacks.
 *
 * Shows a list of sessions with timestamps, captured data, and exfiltration status.
 * Each session can be expanded to view:
 * - The raw captured credentials
 * - The encoded (disguised) exfiltration payload
 * - The decoded payload for comparison
 *
 * This screen demonstrates the data an attacker would have access to.
 */
class SessionListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GhostTouchTheme {
                SessionListScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(onBackClick: () -> Unit) {
    val sessions by SessionRepository.sessions.collectAsState()

    Scaffold(
        containerColor = GhostColors.NavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CAPTURED SESSIONS",
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sessions.isNotEmpty()) {
                        IconButton(onClick = { SessionRepository.clear() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GhostColors.SlateCard,
                    titleContentColor = GhostColors.CyanAccent,
                    navigationIconContentColor = GhostColors.TextPrimary,
                    actionIconContentColor = GhostColors.DangerRed
                )
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = GhostColors.TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No captured sessions",
                        color = GhostColors.TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        "Start the overlay service and interact with the target app",
                        color = GhostColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${sessions.size} session(s) captured",
                        color = GhostColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

/**
 * Expandable card showing captured session details.
 */
@Composable
fun SessionCard(session: CaptureSession) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = GhostColors.SlateCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (session.overlayType) {
                            "payment" -> Icons.Default.CreditCard
                            "tapjacking" -> Icons.Default.TouchApp
                            "capture_all" -> Icons.Default.Security
                            else -> Icons.AutoMirrored.Filled.Login
                        },
                        contentDescription = null,
                        tint = GhostColors.CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        session.overlayType.uppercase(),
                        color = GhostColors.CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Exfil status badge
                Box(
                    modifier = Modifier
                        .background(
                            when (session.exfilStatus) {
                                "sent (simulated)" -> GhostColors.WarnAmber.copy(alpha = 0.2f)
                                else -> GhostColors.TextMuted.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        session.exfilStatus,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when (session.exfilStatus) {
                            "sent (simulated)" -> GhostColors.WarnAmber
                            else -> GhostColors.TextMuted
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target and timestamp
            Text(
                "Target: ${session.targetApp}",
                fontSize = 11.sp,
                color = GhostColors.TextTertiary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                session.timestamp,
                fontSize = 11.sp,
                color = GhostColors.TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Captured data preview
            DataRow("Email/User", session.email)
            DataRow("Password", session.password)

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = GhostColors.TextMuted.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (session.encodedPayload.isNotEmpty()) {
                        Text(
                            "DISGUISED PAYLOAD (as analytics event):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostColors.WarnAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    GhostColors.TerminalBlack,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                session.encodedPayload,
                                fontSize = 9.sp,
                                color = GhostColors.SuccessGreen,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "DECODED (what attacker actually receives):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostColors.DangerRed,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    GhostColors.TerminalBlack,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                DataExfiltrator.decodePayload(session.encodedPayload),
                                fontSize = 9.sp,
                                color = GhostColors.DangerRed,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Expand indicator
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Toggle details",
                tint = GhostColors.TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                "$label: ",
                fontSize = 12.sp,
                color = GhostColors.TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                value,
                fontSize = 12.sp,
                color = GhostColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
