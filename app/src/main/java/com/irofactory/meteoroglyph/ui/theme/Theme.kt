package com.irofactory.meteoroglyph.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Esquemas ──────────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = AccentGreen,
    onPrimary        = Background,
    secondary        = PurpleEvent,
    onSecondary      = Background,
    tertiary         = InfoBlue,
    background       = Background,
    onBackground     = TextPrimary,
    surface          = Surface1,
    onSurface        = TextPrimary,
    surfaceVariant   = Surface2,
    onSurfaceVariant = TextSecondary,
    outline          = Border,
    error            = DangerRed,
    onError          = Background,
)

private val LightColorScheme = lightColorScheme(
    primary          = LightAccent,
    onPrimary        = LightBackground,
    secondary        = LightPurple,
    onSecondary      = LightBackground,
    tertiary         = LightBlue,
    background       = LightBackground,
    onBackground     = LightTextPrimary,
    surface          = LightSurface1,
    onSurface        = LightTextPrimary,
    surfaceVariant   = LightSurface2,
    onSurfaceVariant = LightTextSecondary,
    outline          = LightBorder,
    error            = LightDanger,
    onError          = LightBackground,
)

// ── CompositionLocal para colores semánticos extra ────────────────────────────
data class MetroColors(
    val background:     androidx.compose.ui.graphics.Color,
    val surface1:       androidx.compose.ui.graphics.Color,
    val surface2:       androidx.compose.ui.graphics.Color,
    val border:         androidx.compose.ui.graphics.Color,
    val textPrimary:    androidx.compose.ui.graphics.Color,
    val textSecondary:  androidx.compose.ui.graphics.Color,
    val textMuted:      androidx.compose.ui.graphics.Color,
    val accent:         androidx.compose.ui.graphics.Color,
    val warn:           androidx.compose.ui.graphics.Color,
    val danger:         androidx.compose.ui.graphics.Color,
    val blue:           androidx.compose.ui.graphics.Color,
    val purple:         androidx.compose.ui.graphics.Color,
    val orange:         androidx.compose.ui.graphics.Color,
    val greenSurface:   androidx.compose.ui.graphics.Color,
    val amberSurface:   androidx.compose.ui.graphics.Color,
    val redSurface:     androidx.compose.ui.graphics.Color,
    val blueSurface:    androidx.compose.ui.graphics.Color,
    val purpleSurface:  androidx.compose.ui.graphics.Color,
    val orangeSurface:  androidx.compose.ui.graphics.Color,
    val isDark:         Boolean
)

val DarkMetroColors = MetroColors(
    background    = Background,
    surface1      = Surface1,
    surface2      = Surface2,
    border        = Border,
    textPrimary   = TextPrimary,
    textSecondary = TextSecondary,
    textMuted     = TextMuted,
    accent        = AccentGreen,
    warn          = WarnAmber,
    danger        = DangerRed,
    blue          = InfoBlue,
    purple        = PurpleEvent,
    orange        = OrangeTransit,
    greenSurface  = GreenSurface,
    amberSurface  = AmberSurface,
    redSurface    = RedSurface,
    blueSurface   = BlueSurface,
    purpleSurface = PurpleSurface,
    orangeSurface = OrangeSurface,
    isDark        = true
)

val LightMetroColors = MetroColors(
    background    = LightBackground,
    surface1      = LightSurface1,
    surface2      = LightSurface2,
    border        = LightBorder,
    textPrimary   = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted     = LightTextMuted,
    accent        = LightAccent,
    warn          = LightWarn,
    danger        = LightDanger,
    blue          = LightBlue,
    purple        = LightPurple,
    orange        = LightOrange,
    greenSurface  = LightGreenSurface,
    amberSurface  = LightAmberSurface,
    redSurface    = LightRedSurface,
    blueSurface   = LightBlueSurface,
    purpleSurface = LightPurpleSurface,
    orangeSurface = LightOrangeSurface,
    isDark        = false
)

val LocalMetroColors = staticCompositionLocalOf { DarkMetroColors }

// Acceso fácil desde cualquier composable
val metroColors: MetroColors
    @Composable get() = LocalMetroColors.current

// ── Modo de tema ──────────────────────────────────────────────────────────────
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun MeteoroglyphTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark   = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme  = if (isDark) DarkColorScheme  else LightColorScheme
    val metroColors  = if (isDark) DarkMetroColors  else LightMetroColors
    val view         = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalMetroColors provides metroColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}