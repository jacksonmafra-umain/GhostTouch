package com.wcdonalds.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wcdonalds.app.ui.login.LoginActivity
import com.wcdonalds.app.ui.payment.PaymentActivity
import com.wcdonalds.app.ui.settings.SettingsActivity
import com.wcdonalds.app.ui.theme.WcColors
import com.wcdonalds.app.ui.theme.WcDonaldsTheme

/**
 * Main entry point for the WcDonald's app.
 *
 * Displays the home screen with navigation buttons to login, payment, and settings.
 * This is the screen that the attacker overlay targets — when the user opens this app,
 * the attacker's overlay service detects it and displays a fake UI on top.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WcDonaldsTheme {
                MainScreen(
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onPaymentClick = {
                        startActivity(Intent(this, PaymentActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

/**
 * Main screen composable with WcDonald's branding and navigation options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLoginClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "WcDonald's",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WcColors.Red,
                    titleContentColor = WcColors.Yellow,
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = WcColors.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo area
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(WcColors.Yellow, RoundedCornerShape(60.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "W",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = WcColors.Red
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Welcome to WcDonald's",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Order your favorite meals",
                style = MaterialTheme.typography.bodyLarge,
                color = WcColors.GrayDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            MenuButton(
                icon = Icons.AutoMirrored.Filled.Login,
                title = "Sign In",
                subtitle = "Access your account",
                onClick = onLoginClick
            )

            MenuButton(
                icon = Icons.Default.Payment,
                title = "Quick Pay",
                subtitle = "Fast checkout",
                onClick = onPaymentClick
            )

            MenuButton(
                icon = Icons.Default.Fastfood,
                title = "Browse Menu",
                subtitle = "Explore our items",
                onClick = { /* Demo placeholder */ }
            )
        }
    }
}

/**
 * Styled menu button with icon, title, and subtitle.
 */
@Composable
private fun MenuButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WcColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(WcColors.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WcColors.Red)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(subtitle, color = WcColors.GrayDark, fontSize = 14.sp)
            }
        }
    }
}
