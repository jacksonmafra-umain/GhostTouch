package com.wcdonalds.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * WcDonald's brand colors — a parody of McDonald's for demo purposes.
 * Red and yellow palette with white text on dark backgrounds.
 */
object WcColors {
    val Red = Color(0xFFDA291C)
    val RedDark = Color(0xFFB71C1C)
    val Yellow = Color(0xFFFFC72C)
    val YellowLight = Color(0xFFFFD54F)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF1A1A1A)
    val Gray = Color(0xFFF5F5F5)
    val GrayDark = Color(0xFF757575)
    val GrayMedium = Color(0xFFE0E0E0)
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val Success = Color(0xFF388E3C)
}

private val WcDonaldsColorScheme = lightColorScheme(
    primary = WcColors.Red,
    onPrimary = WcColors.White,
    primaryContainer = WcColors.Yellow,
    onPrimaryContainer = WcColors.Black,
    secondary = WcColors.Yellow,
    onSecondary = WcColors.Black,
    background = WcColors.Background,
    onBackground = WcColors.Black,
    surface = WcColors.Surface,
    onSurface = WcColors.Black,
    error = WcColors.Error,
    onError = WcColors.White,
)

/**
 * WcDonald's Material3 theme wrapper.
 * Uses a light color scheme with the brand's red/yellow palette.
 */
@Composable
fun WcDonaldsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WcDonaldsColorScheme,
        content = content
    )
}
