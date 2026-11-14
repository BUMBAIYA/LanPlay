package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.platform.Sfx
import com.bumbaiya.lanplay.ui.theme.Amber
import com.bumbaiya.lanplay.ui.theme.Baloo
import com.bumbaiya.lanplay.ui.theme.Ink
import com.bumbaiya.lanplay.ui.theme.Muted
import com.bumbaiya.lanplay.ui.theme.Terra
import com.bumbaiya.lanplay.ui.theme.TealDark

/** Number cards have teal ink, operator cards terracotta ink. */
enum class CardKind { Number, Operator }

/** Sizes: opponent mini cards, the building row, your hand, the target, and the big catch view. */
enum class DeckCardSize(val width: Dp, val height: Dp, val glyph: TextUnit) {
    Small(26.dp, 36.dp, 16.sp),
    Row(40.dp, 56.dp, 26.sp),
    Target(40.dp, 56.dp, 26.sp),
    Proof(40.dp, 56.dp, 26.sp),
    Medium(46.dp, 64.dp, 30.sp),
    Large(52.dp, 72.dp, 34.sp),
}

/**
 * A playing card: white face, ink outline, hard shadow. Two-in-one cards show the active action big and the other
 * one small in the top-right corner, so you can see what you would flip to. A tappable card sinks onto its shadow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckCard(
    glyph: String,
    kind: CardKind,
    modifier: Modifier = Modifier,
    size: DeckCardSize = DeckCardSize.Medium,
    scale: Float = 1f,
    dimmed: Boolean = false,
    altGlyph: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val wild = glyph == "*"
    val ink = when { wild -> Amber.copy(red = 0.55f, green = 0.36f, blue = 0.0f); kind == CardKind.Number -> TealDark; else -> Terra }
    val shown = if (wild) "★" else glyph
    val small = size == DeckCardSize.Small
    val radius = (if (small) 6.dp else 9.dp) * scale
    val border = (if (small) 1.8.dp else 2.5.dp)
    val lift = (if (small) 2.dp else 3.dp)
    val glyphSize = size.glyph * scale

    // Tilt: when the active glyph changes (the player tapped), the card swings over and springs back.
    val tilt = remember { Animatable(0f) }
    var firstDraw by remember { mutableStateOf(true) }
    LaunchedEffect(glyph) {
        if (firstDraw) firstDraw = false else {
            tilt.snapTo(-35f)
            tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    // Pop-in: a new card settles into place from slightly small.
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val active = onClick != null || onLongClick != null
    val depth by animateFloatAsState(if (pressed && active) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 1100f), label = "card-press")
    LaunchedEffect(pressed) { if (pressed && active) Sfx.down() }
    val shape = RoundedCornerShape(radius)

    Box(
        modifier = modifier
            .size(size.width * scale, size.height * scale + lift)
            .alpha(if (dimmed) 0.45f else 1f)
            .semantics { contentDescription = "${kind.name} card $glyph" },
    ) {
        Box(
            Modifier
                .size(size.width * scale, size.height * scale)
                .drawBehind { drawRoundRect(Ink, topLeft = Offset(0f, lift.toPx()), size = this.size, cornerRadius = CornerRadius(radius.toPx())) }
                .graphicsLayer { rotationZ = tilt.value; scaleX = pop.value * (1f - 0.03f * depth); scaleY = pop.value * (1f - 0.03f * depth); translationY = depth * lift.toPx() }
                .background(Color.White, shape)
                .border(border, Ink, shape)
                .combinedClickable(interactionSource = source, indication = null, enabled = active, onClick = { Sfx.up(); onClick?.invoke() }, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(shown, color = ink, fontFamily = Baloo, fontSize = glyphSize, fontWeight = FontWeight.Bold)
            if (altGlyph != null) {
                Text(
                    altGlyph, color = Muted, fontFamily = Baloo, fontSize = (if (small) 9.sp else 13.sp) * scale, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp * scale, top = 1.dp),
                )
            }
        }
    }
}

/** The target number: one amber box showing the whole number, so 24 reads as "24" and never as 2 and 4. */
@Composable
fun TargetBox(text: String, modifier: Modifier = Modifier, scale: Float = 1f) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.drawBehind { drawRoundRect(Ink, topLeft = Offset(0f, 3.dp.toPx()), size = this.size, cornerRadius = CornerRadius(8.dp.toPx())) }
            .background(Amber, shape).border(2.5.dp, Ink, shape).padding(horizontal = 9.dp * scale).size(width = (16.dp * text.length.coerceAtLeast(1) + 18.dp) * scale, height = 46.dp * scale),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 26.sp * scale, color = Ink, maxLines = 1, softWrap = false) }
}
