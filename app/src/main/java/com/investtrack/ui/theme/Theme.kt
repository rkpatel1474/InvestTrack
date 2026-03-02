package com.investtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
val GreenPrimary = Color(0xFF1B5E20)
val GreenLight = Color(0xFF4CAF50)
val GreenContainer = Color(0xFFC8E6C9)
val RedNegative = Color(0xFFD32F2F)
val RedContainer = Color(0xFFFFCDD2)
val BlueAccent = Color(0xFF1565C0)
val GoldAccent = Color(0xFFF9A825)
val SurfaceLight = Color(0xFFF5F7FA)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1E1E2E)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    secondary = BlueAccent,
    onSecondary = Color.White,
    tertiary = GoldAccent,
    background = SurfaceLight,
    surface = CardLight,
    surfaceVariant = Color(0xFFEFF2F5),
    error = RedNegative
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF90CAF9),
    tertiary = GoldAccent,
    background = Color(0xFF0F0F1A),
    surface = CardDark,
    error = Color(0xFFEF9A9A)
)

@Composable
fun InvestTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}

val GainColor = Color(0xFF2E7D32)
val LossColor = Color(0xFFC62828)
val NeutralColor = Color(0xFF546E7A)
