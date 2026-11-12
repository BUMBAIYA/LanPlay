package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bumbaiya.lanplay.ui.theme.LanPlayTheme
import com.bumbaiya.lanplay.ui.theme.ThemeMode

/**
 * The play area for landscape game screens: always dark, held in landscape, and sized to the screen so
 * nothing inside ever needs to scroll.
 */
@Composable
fun Stage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    LockLandscape()
    LanPlayTheme(ThemeMode.Dark) {
        Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF141A24), Color(0xFF0B0E13))))) {
            CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
        }
    }
}
