package com.investtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── App Theme Enum ───────────────────────────────────────────────────────────
enum class AppTheme(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    OCEAN("Ocean Blue"),
    FOREST("Forest Green"),
    SUNSET("Sunset"),
    MIDNIGHT("Midnight Purple")
}

// ─── Shared Semantic Colors ───────────────────────────────────────────────────
val GainColor    = Color(0xFF00C853)
val LossColor    = Color(0xFFFF1744)
val NeutralColor = Color(0xFF90A4AE)
val GoldColor    = Color(0xFFFFAB00)

// ─── DARK THEME ───────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary           = Color(0xFF00E5A0),
    onPrimary         = Color(0xFF00382A),
    primaryContainer  = Color(0xFF00503C),
    onPrimaryContainer= Color(0xFF7EFFD4),
    secondary         = Color(0xFF4DB6FF),
    onSecondary       = Color(0xFF003352),
    background        = Color(0xFF0D1117),
    surface           = Color(0xFF161B22),
    surfaceVariant    = Color(0xFF1E2530),
    onBackground      = Color(0xFFE6EDF3),
    onSurface         = Color(0xFFE6EDF3),
    onSurfaceVariant  = Color(0xFF8B949E),
    error             = Color(0xFFFF6B6B),
    outline           = Color(0xFF30363D)
)

// ─── LIGHT THEME ──────────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary           = Color(0xFF006C4C),
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFCDF5E1),
    onPrimaryContainer= Color(0xFF002116),
    secondary         = Color(0xFF1565C0),
    onSecondary       = Color.White,
    background        = Color(0xFFF6F8FA),
    surface           = Color(0xFFFFFFFF),
    surfaceVariant    = Color(0xFFF0F3F6),
    onBackground      = Color(0xFF1A1A2E),
    onSurface         = Color(0xFF1A1A2E),
    onSurfaceVariant  = Color(0xFF57606A),
    error             = Color(0xFFCF1322),
    outline           = Color(0xFFD0D7DE)
)

// ─── OCEAN THEME ──────────────────────────────────────────────────────────────
private val OceanColors = darkColorScheme(
    primary           = Color(0xFF00B4D8),
    onPrimary         = Color(0xFF001E2B),
    primaryContainer  = Color(0xFF003547),
    onPrimaryContainer= Color(0xFF7FD8F5),
    secondary         = Color(0xFF48CAE4),
    onSecondary       = Color(0xFF002B38),
    background        = Color(0xFF03045E),
    surface           = Color(0xFF023E8A),
    surfaceVariant    = Color(0xFF0077B6),
    onBackground      = Color(0xFFCAF0F8),
    onSurface         = Color(0xFFCAF0F8),
    onSurfaceVariant  = Color(0xFF90E0EF),
    error             = Color(0xFFFF6B6B),
    outline           = Color(0xFF0096C7)
)

// ─── FOREST THEME ─────────────────────────────────────────────────────────────
private val ForestColors = darkColorScheme(
    primary           = Color(0xFF52B788),
    onPrimary         = Color(0xFF0A2E1A),
    primaryContainer  = Color(0xFF1B4332),
    onPrimaryContainer= Color(0xFFB7E4C7),
    secondary         = Color(0xFF95D5B2),
    onSecondary       = Color(0xFF0A2E1A),
    background        = Color(0xFF081C15),
    surface           = Color(0xFF1B4332),
    surfaceVariant    = Color(0xFF2D6A4F),
    onBackground      = Color(0xFFD8F3DC),
    onSurface         = Color(0xFFD8F3DC),
    onSurfaceVariant  = Color(0xFF95D5B2),
    error             = Color(0xFFFF6B6B),
    outline           = Color(0xFF40916C)
)

// ─── SUNSET THEME ─────────────────────────────────────────────────────────────
private val SunsetColors = darkColorScheme(
    primary           = Color(0xFFFF8C42),
    onPrimary         = Color(0xFF2D1200),
    primaryContainer  = Color(0xFF4A1E00),
    onPrimaryContainer= Color(0xFFFFD0B0),
    secondary         = Color(0xFFFFBF69),
    onSecondary       = Color(0xFF2D1A00),
    background        = Color(0xFF1A0A00),
    surface           = Color(0xFF2D1500),
    surfaceVariant    = Color(0xFF3D2000),
    onBackground      = Color(0xFFFFF0E6),
    onSurface         = Color(0xFFFFF0E6),
    onSurfaceVariant  = Color(0xFFFFCC99),
    error             = Color(0xFFFF6B6B),
    outline           = Color(0xFF8B4513)
)

// ─── MIDNIGHT THEME ───────────────────────────────────────────────────────────
private val MidnightColors = darkColorScheme(
    primary           = Color(0xFFBB86FC),
    onPrimary         = Color(0xFF21005D),
    primaryContainer  = Color(0xFF380093),
    onPrimaryContainer= Color(0xFFEADDFF),
    secondary         = Color(0xFF9ECAFF),
    onSecondary       = Color(0xFF003258),
    background        = Color(0xFF0A0010),
    surface           = Color(0xFF12001E),
    surfaceVariant    = Color(0xFF1E0033),
    onBackground      = Color(0xFFEADDFF),
    onSurface         = Color(0xFFEADDFF),
    onSurfaceVariant  = Color(0xFFCCC2DC),
    error             = Color(0xFFFF6B6B),
    outline           = Color(0xFF4A0080)
)

// ─── Typography ───────────────────────────────────────────────────────────────
val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Black,     fontSize = 57.sp, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 18.sp, letterSpacing = (-0.1).sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 15.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 13.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 10.sp, letterSpacing = 0.5.sp),
)

@Composable
fun InvestTrackTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM  -> if (isDark) DarkColors else LightColors
        AppTheme.LIGHT   -> LightColors
        AppTheme.DARK    -> DarkColors
        AppTheme.OCEAN   -> OceanColors
        AppTheme.FOREST  -> ForestColors
        AppTheme.SUNSET  -> SunsetColors
        AppTheme.MIDNIGHT-> MidnightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
