package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CalmingDarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color(0xFF0D0F14),
    primaryContainer = Color(0xFF1E2433),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = SecondaryLavender,
    onSecondary = Color(0xFF0D0F14),
    secondaryContainer = Color(0xFF161A24),
    onSecondaryContainer = Color(0xFF9CA3AF),
    tertiary = TertiaryBlue,
    onTertiary = Color(0xFF0D0F14),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = CrisisRed,
    onError = Color.White
)

private val CalmingLightColorScheme = lightColorScheme(
    primary = Color(0xFF5352ED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E7FF),
    onPrimaryContainer = Color(0xFF1F1C62),
    secondary = Color(0xFF2ED573),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5FFF0),
    onSecondaryContainer = Color(0xFF0C5424),
    tertiary = Color(0xFF1E90FF),
    onTertiary = Color.White,
    background = Color(0xFFF1F2F6),
    onBackground = Color(0xFF2F3542),
    surface = Color.White,
    onSurface = Color(0xFF2F3542),
    surfaceVariant = Color(0xFFF1F2F6),
    onSurfaceVariant = Color(0xFF747D8C),
    outline = Color(0xFFCED6E0),
    error = Color(0xFFFF4757),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force comforting dark theme by default for exam study health
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CalmingDarkColorScheme else CalmingLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
