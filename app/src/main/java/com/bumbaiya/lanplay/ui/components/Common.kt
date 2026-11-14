package com.bumbaiya.lanplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.theme.Cream

/** Player names are at most this long, everywhere. */
const val NameMax = com.bumbaiya.lanplay.game.combinemaths.PlayerName.MAX

/** The system-bar / camera cut-out insets. Screens draw their background edge to edge and pad their content by this. */
val LocalSafePadding = compositionLocalOf { PaddingValues(0.dp) }

/**
 * The frame every screen sits in: cream, edge to edge, with the soft drifting colour glows. Content is padded
 * clear of the camera cut-out, and nothing scrolls. [cards] adds the floating number cards, which belong to
 * the Combine game's own screens only.
 */
@Composable
fun Frame(cards: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Cream)) {
        Backdrop(cards)
        Box(Modifier.fillMaxSize().padding(LocalSafePadding.current).padding(horizontal = 28.dp, vertical = 10.dp), content = content)
    }
}

/** The line under a name field: the plain rule, or in red which characters can't be used. */
@Composable
fun NameHint(raw: String, modifier: Modifier = Modifier) {
    val problem = com.bumbaiya.lanplay.game.combinemaths.PlayerName.problem(raw)
    androidx.compose.material3.Text(
        problem ?: "Letters only, up to $NameMax. No numbers, spaces or symbols.", modifier,
        fontFamily = com.bumbaiya.lanplay.ui.theme.Inter, fontSize = 10.sp,
        fontWeight = if (problem != null) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        color = if (problem != null) com.bumbaiya.lanplay.ui.theme.Terra else com.bumbaiya.lanplay.ui.theme.Muted,
    )
}
