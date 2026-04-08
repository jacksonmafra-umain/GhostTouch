package com.wcdonalds.app.ui.payment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.wcdonalds.app.security.*
import com.wcdonalds.app.ui.settings.DefenseSettings
import com.wcdonalds.app.ui.theme.WcColors
import com.wcdonalds.app.ui.theme.WcDonaldsTheme

/**
 * Payment screen for the WcDonald's app.
 *
 * Collects card number, expiry, and CVV. This is a high-value target for
 * overlay attacks since it handles financial data.
 *
 * ## Defense mechanisms (when enabled):
 * - **filterTouchesWhenObscured**: Blocks input when overlay detected
 * - **FLAG_SECURE**: Prevents screenshots/recording
 * - **Biometric authentication**: System-level prompt that overlays cannot cover
 * - **Focus monitoring**: Detects suspicious focus loss
 */
class PaymentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply FLAG_SECURE before setting content
        if (DefenseSettings.secureScreens) {
            SecureScreenHelper.applySecureFlag(this, true)
        }

        setContent {
            WcDonaldsTheme {
                PaymentScreen(
                    onBackClick = { finish() },
                    onPaymentSubmit = { handlePayment() }
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

    /**
     * Handles payment submission with optional biometric verification.
     *
     * When biometric defense is enabled, the payment is gated behind a
     * system biometric prompt that overlays cannot cover. This ensures
     * the user is physically present and aware of the transaction.
     */
    private fun handlePayment() {
        if (DefenseSettings.biometricAuth && BiometricHelper.isAvailable(this)) {
            BiometricHelper.authenticate(
                activity = this,
                title = "Confirm Payment",
                subtitle = "Authenticate to pay $16.77",
                onSuccess = {
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Payment cancelled: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBackClick: () -> Unit,
    onPaymentSubmit: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var overlayWarning by remember { mutableStateOf(false) }

    val showWarning = DefenseSettings.filterObscuredTouches &&
            (SecureTouchModifier.isObscured || overlayWarning)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
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
            // Defense: Warning banner
            OverlayWarningBanner(visible = showWarning)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        SecureTouchModifier.filterObscuredTouches(
                            enabled = DefenseSettings.filterObscuredTouches,
                            onObscuredTouch = { overlayWarning = true }
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Order Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WcColors.Gray)
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = !showWarning
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it.filter { c -> c.isDigit() } },
                    label = { Text("Card Number") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !showWarning
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 5) expiry = it },
                        label = { Text("MM/YY") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !showWarning
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 3) cvv = it.filter { c -> c.isDigit() } },
                        label = { Text("CVV") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !showWarning
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onPaymentSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WcColors.Red),
                    enabled = cardNumber.length == 16 && cvv.length == 3 &&
                            expiry.isNotBlank() && !showWarning
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
