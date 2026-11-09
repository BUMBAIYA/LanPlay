package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors

/** Number cards are blue, operator cards are red, like the physical deck. */
enum class CardKind { Number, Operator }

/** Sizes taken from the design: opponent row, your hand, and the big catch view. */
enum class DeckCardSize(val width: Dp, val height: Dp, val glyph: TextUnit) {
    Small(26.dp, 36.dp, 16.sp),
    Row(40.dp, 54.dp, 24.sp),
    Target(44.dp, 62.dp, 32.sp),
    Proof(48.dp, 62.dp, 30.sp),
    Medium(48.dp, 66.dp, 28.sp),
    Large(56.dp, 78.dp, 34.sp),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckCard(
    glyph: String,
    kind: CardKind,
    modifier: Modifier = Modifier,
    size: DeckCardSize = DeckCardSize.Medium,
    dimmed: Boolean = false,
    /**
     * For two-in-one cards (+ with x, - with ÷). The [glyph] is the ACTIVE action, drawn big at
     * the top-right. This one is the inactive action, drawn small at the bottom-left.
     * To "tilt" the card, the caller simply swaps the two values.
     */
    altGlyph: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.lanPlayColors

    // Tilt animation: whenever the active glyph changes (the player tapped), the card swings
    // over to one side and springs back to upright. Animatable holds a number we can animate
    // frame by frame; snapTo jumps instantly, animateTo glides with a bouncy spring.
    val tilt = remember { Animatable(0f) }
    var firstDraw by remember { mutableStateOf(true) }
    LaunchedEffect(glyph) {
        if (firstDraw) { firstDraw = false } else {
            tilt.snapTo(-35f)
            tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    // Pop-in: a new card bounces up from small, so draws and deals feel alive.
    val pop = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }

    Surface(
        modifier = modifier
            .size(size.width, size.height)
            .graphicsLayer { rotationZ = tilt.value; scaleX = pop.value; scaleY = pop.value }
            .alpha(if (dimmed) 0.4f else 1f)
            .combinedClickable(enabled = onClick != null || onLongClick != null, onClick = { onClick?.invoke() }, onLongClick = onLongClick)
            .semantics { contentDescription = "${kind.name} card $glyph" },
        shape = RoundedCornerShape(if (size == DeckCardSize.Small) 6.dp else 9.dp),
        color = colors.cardFace,
        border = BorderStroke(1.dp, colors.cardBorder),
    ) {
        // Surface centers nothing by itself, so the Text is centered with a box-like modifier.
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = if (altGlyph != null) Alignment.TopEnd else Alignment.Center,
        ) {
            if (altGlyph != null) {
                Text(
                    text = altGlyph,
                    color = colors.cardOperator.copy(alpha = 0.45f),
                    fontFamily = SpaceGrotesk,
                    fontSize = size.glyph * 0.55f,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = androidx.compose.ui.Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 5.dp, bottom = 3.dp),
                )
            }
            Text(
                modifier = if (altGlyph != null) androidx.compose.ui.Modifier.padding(end = 6.dp, top = 2.dp) else androidx.compose.ui.Modifier,
                text = glyph,
                color = if (kind == CardKind.Number) colors.cardNumber else colors.cardOperator,
                fontFamily = SpaceGrotesk,
                fontSize = size.glyph,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }
    }
}
