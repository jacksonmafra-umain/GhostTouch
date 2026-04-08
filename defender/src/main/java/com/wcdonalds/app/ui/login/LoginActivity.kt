package com.wcdonalds.app.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wcdonalds.app.security.FocusMonitor
import com.wcdonalds.app.security.OverlayWarningBanner
import com.wcdonalds.app.security.SecureScreenHelper
import com.wcdonalds.app.security.SecureTouchModifier
import com.wcdonalds.app.ui.dashboard.DashboardActivity
import com.wcdonalds.app.ui.settings.DefenseSettings
import com.wcdonalds.app.ui.theme.WcColors
import com.wcdonalds.app.ui.theme.WcDonaldsTheme

/**
 * Login screen for the WcDonald's app.
 *
 * This is one of the primary screens targeted by overlay attacks — the attacker
 * displays a fake login form on top of this screen to capture credentials.
 *
 * ## Defense mechanisms (when enabled via Settings):
 * - **filterTouchesWhenObscured**: Blocks touch input when an overlay is detected
 *   via FLAG_WINDOW_IS_OBSCURED check in [SecureTouchModifier]
 * - **FLAG_SECURE**: Prevents screenshots and screen recording of this screen
 *   via [SecureScreenHelper]
 * - **Focus monitoring**: Detects window focus loss via [FocusMonitor]
 * - **Overlay warning banner**: Shows prominent warning when overlay is detected
 *   via [OverlayWarningBanner]
 */
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply FLAG_SECURE before setting content (must be before setContent)
        if (DefenseSettings.secureScreens) {
            SecureScreenHelper.applySecureFlag(this, true)
        }

        setContent {
            WcDonaldsTheme {
                LoginScreen(
                    onBackClick = { finish() },
                    onLoginSuccess = {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (DefenseSettings.focusMonitoring) {
            FocusMonitor.onFocusChanged(hasFocus)
        }
    }
}

/**
 * Login form composable with integrated defense mechanisms.
 *
 * When defenses are enabled, the form applies [SecureTouchModifier.filterObscuredTouches]
 * to all input fields and shows an [OverlayWarningBanner] when an overlay is detected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var overlayWarning by remember { mutableStateOf(false) }

    // Show overlay warning when touch filtering detects an obscured touch
    val showWarning = DefenseSettings.filterObscuredTouches &&
            (SecureTouchModifier.isObscured || overlayWarning)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
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
        ) {
            // Defense: Overlay warning banner
            OverlayWarningBanner(visible = showWarning)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Defense: Apply touch filtering modifier to entire form area
                    .then(
                        SecureTouchModifier.filterObscuredTouches(
                            enabled = DefenseSettings.filterObscuredTouches,
                            onObscuredTouch = { overlayWarning = true }
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(WcColors.Yellow, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "W",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = WcColors.Red
                    )
                }

                Text(
                    "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Sign in to your WcDonald's account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WcColors.GrayDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("your@email.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !showWarning // Defense: disable when overlay detected
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !showWarning // Defense: disable when overlay detected
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sign in button
                Button(
                    onClick = {
                        isLoading = true
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WcColors.Red),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading && !showWarning
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WcColors.White
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(
                    onClick = { /* Demo placeholder */ },
                    enabled = !showWarning
                ) {
                    Text("Forgot Password?", color = WcColors.Red)
                }
            }
        }
    }
}
