package com.mutabei.workout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticColors(
    val ok: Color,
    val warn: Color,
    val low: Color,
    val muted: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        ok = LightOk,
        warn = LightWarn,
        low = LightLow,
        muted = LightMuted
    )
}

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E8FF),
    onPrimaryContainer = Color(0xFF0E2366),
    secondary = LightOk,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F2E2),
    onSecondaryContainer = Color(0xFF0E3B21),
    tertiary = LightWarn,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightInk,
    surface = LightCard,
    onSurface = LightInk,
    surfaceVariant = LightBg,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    outlineVariant = LightLine,
    error = LightLow,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color(0xFF0A1433),
    primaryContainer = Color(0xFF213163),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = DarkOk,
    onSecondary = Color(0xFF072B17),
    secondaryContainer = Color(0xFF173D28),
    onSecondaryContainer = Color(0xFFC8F5D9),
    tertiary = DarkWarn,
    onTertiary = Color(0xFF332000),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkCard,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF23293D),
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = DarkLow,
    onError = Color(0xFF3D0C04)
)

private val MutabeiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MutabeiTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (isDark) DarkColorScheme else LightColorScheme
    val semantic = if (isDark) {
        SemanticColors(ok = DarkOk, warn = DarkWarn, low = DarkLow, muted = DarkMuted)
    } else {
        SemanticColors(ok = LightOk, warn = LightWarn, low = LightLow, muted = LightMuted)
    }

    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = colors,
            typography = MutabeiTypography,
            shapes = MutabeiShapes,
            content = content
        )
    }
}
