package com.ghosttouch.common.model

/**
 * Represents a single captured input session from an overlay attack.
 *
 * Each session records the data captured when a user interacts with a fake overlay
 * that was displayed over a target application. This model is used for educational
 * demonstration purposes only.
 *
 * @property id Unique identifier for this session (epoch millis at capture time).
 * @property targetApp Package name of the app that was being overlaid (e.g., "com.wcdonalds.app").
 * @property email The email/username value captured from the fake login form.
 * @property password The password value captured from the fake login form.
 * @property timestamp ISO 8601 formatted timestamp of when the capture occurred.
 * @property overlayType The type of overlay that was used (e.g., "login", "payment", "tapjacking").
 * @property encodedPayload The Base64-encoded exfiltration payload, if generated.
 * @property exfilStatus Status of the simulated exfiltration ("pending", "sent", "failed").
 */
data class CaptureSession(
    val id: Long = System.currentTimeMillis(),
    val targetApp: String,
    val email: String = "",
    val password: String = "",
    val cardNumber: String = "",
    val timestamp: String,
    val overlayType: String = "login",
    val encodedPayload: String = "",
    val exfilStatus: String = "pending"
)
