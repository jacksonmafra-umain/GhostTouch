package com.ghosttouch.attacker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Cyberpunk-inspired dark theme for the GhostTouch attacker app.
 * Uses navy/slate tones with cyan accent — evoking a hacker terminal aesthetic.
 */
object GhostColors {
    val NavyBackground = Color(0xFF0A0F1C)
    val SlateCard = Color(0xFF1E293B)
    val InsetSurface = Color(0xFF0F172A)
    val CyanAccent = Color(0xFF22D3EE)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)
    val TextMuted = Color(0xFF475569)
    val TextInverted = Color(0xFF0A0F1C)
    val DangerRed = Color(0xFFFF6B6B)
    val WarnAmber = Color(0xFFF59E0B)
    val SuccessGreen = Color(0xFF22D3EE)
    val TerminalBlack = Color(0xFF040810)
}

private val GhostTouchColorScheme = darkColorScheme(
    primary = GhostColors.CyanAccent,
    onPrimary = GhostColors.TextInverted,
    primaryContainer = GhostColors.SlateCard,
    onPrimaryContainer = GhostColors.CyanAccent,
    secondary = GhostColors.WarnAmber,
    onSecondary = GhostColors.TextInverted,
    background = GhostColors.NavyBackground,
    onBackground = GhostColors.TextPrimary,
    surface = GhostColors.SlateCard,
    onSurface = GhostColors.TextPrimary,
    surfaceVariant = GhostColors.InsetSurface,
    onSurfaceVariant = GhostColors.TextSecondary,
    error = GhostColors.DangerRed,
    onError = GhostColors.TextPrimary,
)

/**
 * GhostTouch dark theme wrapper — cyberpunk terminal aesthetic.
 */
@Composable
fun GhostTouchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostTouchColorScheme,
        content = content
    )
}
