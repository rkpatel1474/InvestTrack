package com.investtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Premium deep navy + emerald palette
val Navy900    = Color(0xFF0A0E1A)
val Navy800    = Color(0xFF0F1629)
val Navy700    = Color(0xFF151D36)
val Navy600    = Color(0xFF1C2642)
val Navy500    = Color(0xFF243052)

val Emerald500 = Color(0xFF00C896)
val Emerald400 = Color(0xFF26D9A8)
val Emerald300 = Color(0xFF4DEDC0)
val EmeraldDim = Color(0xFF00C89620)

val Amber400   = Color(0xFFFFBB33)
val Rose500    = Color(0xFFFF4D6A)
val Rose400    = Color(0xFFFF6B84)
val Sapphire   = Color(0xFF4A90E2)

val SurfaceDark  = Color(0xFF111827)
val CardDark2    = Color(0xFF1A2235)
val CardDark3    = Color(0xFF1E293B)
val BorderColor  = Color(0xFF243052)

val GainColor  = Color(0xFF00C896)
val LossColor  = Color(0xFFFF4D6A)
val NeutralColor = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary           = Emerald500,
    onPrimary         = Navy900,
    primaryContainer  = EmeraldDim,
    onPrimaryContainer = Emerald300,
    secondary         = Sapphire,
    onSecondary       = Color.White,
    secondaryContainer = Color(0xFF4A90E220),
    tertiary          = Amber400,
    background        = Navy900,
    surface           = CardDark2,
    surfaceVariant    = CardDark3,
    onBackground      = Color(0xFFF0F4FF),
    onSurface         = Color(0xFFF0F4FF),
    onSurfaceVariant  = Color(0xFF94A3B8),
    error             = Rose500,
    outline           = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary           = Color(0xFF00A878),
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFE0FFF5),
    secondary         = Color(0xFF3B82F6),
    onSecondary       = Color.White,
    tertiary          = Color(0xFFF59E0B),
    background        = Color(0xFFF0F4F8),
    surface           = Color.White,
    surfaceVariant    = Color(0xFFF8FAFC),
    onSurfaceVariant  = Color(0xFF64748B),
    error             = Rose500,
    outline           = Color(0xFFE2E8F0)
)

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Black,  fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

@Composable
fun InvestTrackTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
