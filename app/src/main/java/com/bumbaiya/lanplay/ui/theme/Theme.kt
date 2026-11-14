package com.bumbaiya.lanplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val Scheme = lightColorScheme(
    primary = Amber, onPrimary = Ink,
    secondary = Teal, onSecondary = Ink,
    tertiary = Rose, onTertiary = Ink,
    background = Cream, onBackground = Ink,
    surface = PanelBg, onSurface = Ink,
    surfaceVariant = Sand, onSurfaceVariant = Muted,
    outline = Ink, outlineVariant = Sand2,
    error = Rose, onError = Ink,
)

private val LocalLanPlayColors = staticCompositionLocalOf { LightLanPlayColors }

/** Extra LanPlay colors that sit alongside [MaterialTheme.colorScheme]. */
@Suppress("UnusedReceiverParameter")
val MaterialTheme.lanPlayColors: LanPlayColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLanPlayColors.current

/** Light only for now. The [themeMode] is kept so dark mode can switch on later without touching callers. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun LanPlayTheme(themeMode: ThemeMode = ThemeMode.Light, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLanPlayColors provides LightLanPlayColors) {
        MaterialTheme(colorScheme = Scheme, typography = Typography, content = content)
    }
}
