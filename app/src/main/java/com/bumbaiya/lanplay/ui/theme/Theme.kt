package com.bumbaiya.lanplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF0E1116),
    secondary = Color(0xFF5BA3E6),
    onSecondary = Color(0xFF0E1116),
    tertiary = Color(0xFFF26B5B),
    onTertiary = Color(0xFF0E1116),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF171B22),
    onSurface = Color(0xFFF2F4F7),
    surfaceVariant = Color(0xFF232A35),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF2C3440),
    outlineVariant = Color(0xFF2C3440),
    error = Color(0xFFF26B5B),
    onError = Color(0xFF0E1116),
)

private val LightColorScheme = lightColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF11151C),
    secondary = Color(0xFF1F6FBF),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFC2402F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF11151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFF566071),
    outline = Color(0xFFD5DBE4),
    outlineVariant = Color(0xFFD5DBE4),
    error = Color(0xFFD93F2C),
    onError = Color(0xFFFFFFFF),
)

private val LocalLanPlayColors = staticCompositionLocalOf { DarkLanPlayColors }

/** Extra LanPlay colors that sit alongside [MaterialTheme.colorScheme]. */
val MaterialTheme.lanPlayColors: LanPlayColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLanPlayColors.current

@Composable
fun LanPlayTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // The brand palette is fixed on purpose, so no dynamic (wallpaper) color.
    CompositionLocalProvider(
        LocalLanPlayColors provides if (darkTheme) DarkLanPlayColors else LightLanPlayColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
