package com.bumbaiya.lanplay.ui.game

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.RowSlot
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

/** Watching another player work: their row appears card by card as they place it. */
@Composable
fun WatchedRow(row: List<RowSlot>, name: String) = androidx.compose.foundation.layout.BoxWithConstraints {
    // Long rows shrink and wrap instead of running off the screen.
    val fit = com.bumbaiya.lanplay.ui.components.fitScale(
        row.size, DeckCardSize.Row.width, DeckCardSize.Row.height, 8.dp, maxWidth, (maxHeight - 30.dp).coerceAtLeast(80.dp),
        extraW = 10.dp, extraH = 6.dp,
    )
    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            if (row.isEmpty()) "$name is thinking..." else "$name is building:",
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.lanPlayColors.accentText,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEachIndexed { i, slot ->
                // key on position + faces so a card pops in when it appears or changes (like a tilt)
                androidx.compose.runtime.key(i, slot) { PopIn { WatchedSlot(slot, fit) } }
            }
        }
    }
}

@Composable
private fun WatchedSlot(slot: RowSlot, scale: Float) {
    val kind = if (slot.glyph.firstOrNull()?.isDigit() == true) CardKind.Number else CardKind.Operator
    Box(Modifier.padding(top = 6.dp, end = 10.dp)) {
        DeckCard(slot.glyph, kind, size = DeckCardSize.Row, scale = scale)
        slot.raised?.let {
            DeckCard(it, CardKind.Number, size = DeckCardSize.Small, scale = scale.coerceAtLeast(0.7f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-8).dp))
        }
    }
}

/** Springs its content in from small and a little tilted. */
@Composable
private fun PopIn(content: @Composable () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
    Box(Modifier.graphicsLayer { scaleX = 0.5f + 0.5f * t.value; scaleY = 0.5f + 0.5f * t.value; rotationZ = (1f - t.value) * -12f; alpha = t.value.coerceIn(0f, 1f) }) { content() }
}
