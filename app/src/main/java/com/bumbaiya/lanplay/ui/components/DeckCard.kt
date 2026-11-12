package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk

/** Number cards have blue ink, operator cards red ink, like the physical deck. */
enum class CardKind { Number, Operator }

/** Sizes: opponent mini cards, the building row, your hand, the target, and the big catch view. */
enum class DeckCardSize(val width: Dp, val height: Dp, val glyph: TextUnit) {
    Small(28.dp, 40.dp, 18.sp),
    Row(44.dp, 60.dp, 28.sp),
    Target(42.dp, 58.dp, 28.sp),
    Proof(48.dp, 66.dp, 30.sp),
    Medium(52.dp, 72.dp, 34.sp),
    Large(62.dp, 88.dp, 42.sp),
}

private val NumberInk = Color(0xFF1F6FBF)
private val OperatorInk = Color(0xFFC2402F)
private val WildInk = Color(0xFF8A5A00)
private val FaceTop = Color(0xFFFFFFFF)
private val FaceBottom = Color(0xFFE8EDF5)
private val CardEdge = Color(0xFFB4BDCB)

@Composable
fun DeckCard(
    glyph: String,
    kind: CardKind,
    modifier: Modifier = Modifier,
    size: DeckCardSize = DeckCardSize.Medium,
    dimmed: Boolean = false,
    /**
     * For two-in-one cards (+ with x, - with ÷). The [glyph] is the ACTIVE action, drawn big.
     * This one is the inactive action, drawn small at the bottom-left.
     * To "tilt" the card, the caller simply swaps the two values.
     */
    altGlyph: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val wild = glyph == "*"
    val ink = when { wild -> WildInk; kind == CardKind.Number -> NumberInk; else -> OperatorInk }
    val shown = if (wild) "★" else glyph
    val small = size == DeckCardSize.Small
    val radius = if (small) 6.dp else 10.dp

    // Tilt animation: whenever the active glyph changes (the player tapped), the card swings over and springs back.
    val tilt = remember { Animatable(0f) }
    var firstDraw by remember { mutableStateOf(true) }
    LaunchedEffect(glyph) {
        if (firstDraw) { firstDraw = false } else {
            tilt.snapTo(-35f)
            tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    // Pop-in: a new card settles into place from slightly small.
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) }

    Box(
        modifier = modifier
            .size(size.width, size.height)
            .graphicsLayer { rotationZ = tilt.value; scaleX = pop.value; scaleY = pop.value }
            .alpha(if (dimmed) 0.4f else 1f)
            .combinedClickable(enabled = onClick != null || onLongClick != null, onClick = { onClick?.invoke() }, onLongClick = onLongClick)
            .semantics { contentDescription = "${kind.name} card $glyph" }
            .drawBehind {
                val r = CornerRadius(radius.toPx())
                // a soft shadow under the card
                drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, 2.dp.toPx()), size = this.size, cornerRadius = r)
                drawRoundRect(Brush.verticalGradient(listOf(FaceTop, FaceBottom)), cornerRadius = r)
                drawRoundRect(CardEdge, cornerRadius = r, style = Stroke(1.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!small) {
            Text(
                shown, color = ink.copy(alpha = 0.75f), fontFamily = SpaceGrotesk, fontSize = size.glyph * 0.34f, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 2.dp),
            )
        }
        Text(shown, color = ink, fontFamily = SpaceGrotesk, fontSize = size.glyph, fontWeight = FontWeight.Bold)
        if (altGlyph != null) {
            Text(
                altGlyph, color = ink.copy(alpha = 0.45f), fontFamily = SpaceGrotesk, fontSize = size.glyph * 0.5f, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = if (small) 2.dp else 6.dp, bottom = 1.dp),
            )
        }
    }
}

