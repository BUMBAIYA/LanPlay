package com.bumbaiya.lanplay.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.random.Random

/** Number cards are colourful, operator cards are dark with gold symbols, the wild card is rainbow. */
enum class CardKind { Number, Operator }

/** Sizes: opponent mini cards, the building row, your hand, and the big catch view. */
enum class DeckCardSize(val width: Dp, val height: Dp, val glyph: TextUnit) {
    Small(28.dp, 40.dp, 18.sp),
    Row(44.dp, 60.dp, 28.sp),
    Target(54.dp, 76.dp, 38.sp),
    Proof(52.dp, 70.dp, 34.sp),
    Medium(52.dp, 72.dp, 34.sp),
    Large(62.dp, 88.dp, 42.sp),
}

/** Face colours for one digit: top, bottom, the solid bottom edge, and the symbol colour. */
private class Look(val top: Color, val bottom: Color, val edge: Color, val ink: Color = Color.White)

private val DigitLooks = listOf(
    Look(Color(0xFF8B95A9), Color(0xFF5A6378), Color(0xFF394055)),
    Look(Color(0xFFFF7A7A), Color(0xFFE83F5B), Color(0xFFA8223D)),
    Look(Color(0xFFFFB04A), Color(0xFFF2761C), Color(0xFFA94A0A)),
    Look(Color(0xFFFFE14D), Color(0xFFF5B800), Color(0xFFA47A00), Color(0xFF4A3300)),
    Look(Color(0xFFA6EE5C), Color(0xFF4CC43A), Color(0xFF2A8A22)),
    Look(Color(0xFF4DE3B0), Color(0xFF12B587), Color(0xFF0A7A5C)),
    Look(Color(0xFF5FD3FF), Color(0xFF1F9DE8), Color(0xFF126AA6)),
    Look(Color(0xFF7A8BFF), Color(0xFF4A55E0), Color(0xFF2C3399)),
    Look(Color(0xFFC58BFF), Color(0xFF8D4DE0), Color(0xFF5F2CA3)),
    Look(Color(0xFFFF8BD1), Color(0xFFE0449F), Color(0xFF992270)),
)
private val OperatorLook = Look(Color(0xFF2B2F4A), Color(0xFF12142A), Color(0xFF05060F), Color(0xFFFFD54A))
private val Rainbow = listOf(Color(0xFFFF5C7A), Color(0xFFFFC83D), Color(0xFFB4F24D), Color(0xFF4DA3FF), Color(0xFF9B6CFF), Color(0xFFFF5C7A))

@OptIn(ExperimentalFoundationApi::class)
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
    val look = if (kind == CardKind.Number) DigitLooks[(glyph.toIntOrNull() ?: 0).coerceIn(0, 9)] else OperatorLook
    val shown = if (wild) "★" else glyph
    val small = size == DeckCardSize.Small
    val radius = if (small) 7.dp else 12.dp

    // Tilt animation: whenever the active glyph changes (the player tapped), the card swings over and springs back.
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

    // Foil shine: one glint sweeps across shortly after the card appears and again each time it is tilted.
    val shine = remember { Animatable(-1f) }
    LaunchedEffect(glyph) {
        if (small) return@LaunchedEffect
        kotlinx.coroutines.delay(150L + Random.nextLong(900))
        shine.snapTo(-1f)
        shine.animateTo(2f, tween(900, easing = LinearEasing))
    }

    val edge = if (small) 2.dp else 4.dp
    Box(
        modifier = modifier
            .size(size.width, size.height)
            .graphicsLayer { rotationZ = tilt.value; scaleX = pop.value; scaleY = pop.value }
            .alpha(if (dimmed) 0.4f else 1f)
            .combinedClickable(enabled = onClick != null || onLongClick != null, onClick = { onClick?.invoke() }, onLongClick = onLongClick)
            .semantics { contentDescription = "${kind.name} card $glyph" },
    ) {
        // The thick bottom edge gives the card depth.
        Box(
            Modifier.matchParentSize().drawBehind { drawRoundRect(look.edge, cornerRadius = CornerRadius(radius.toPx())) },
        )
        Box(
            Modifier
                .size(size.width, size.height - edge)
                .drawWithContent {
                    val r = CornerRadius(radius.toPx())
                    val face = if (wild) Brush.sweepGradient(Rainbow) else Brush.verticalGradient(listOf(look.top, look.bottom))
                    drawRoundRect(face, cornerRadius = r)
                    // gloss: a soft white highlight in the top-left
                    drawRoundRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), Offset(this.size.width * 0.2f, this.size.height * 0.15f), this.size.width * 0.8f), cornerRadius = r)
                    drawContent()
                    if (shine.value in 0f..1f) {
                        val x = this.size.width * (shine.value * 1.6f - 0.3f)
                        drawRoundRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(x - 18f, 0f), Offset(x + 18f, this.size.height * 0.6f)), cornerRadius = r)
                    }
                    drawRoundRect(if (kind == CardKind.Operator && !wild) Color(0xFFFFD54A) else Color.White, cornerRadius = r, style = Stroke(if (small) 1.5.dp.toPx() else 2.5.dp.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!small) {
                Text(
                    shown, color = look.ink.copy(alpha = 0.9f), fontFamily = SpaceGrotesk, fontSize = size.glyph * 0.36f, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 2.dp),
                )
            }
            Text(
                shown, color = look.ink, fontFamily = SpaceGrotesk, fontSize = size.glyph, fontWeight = FontWeight.Bold,
                modifier = Modifier.drawBehind { },
            )
            if (altGlyph != null) {
                Text(
                    altGlyph, color = look.ink.copy(alpha = 0.6f), fontFamily = SpaceGrotesk, fontSize = size.glyph * 0.5f, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = if (small) 2.dp else 6.dp, bottom = 1.dp),
                )
            }
        }
    }
}

/** A face-down card: the deck. */
@Composable
fun CardBack(modifier: Modifier = Modifier, size: DeckCardSize = DeckCardSize.Medium) {
    val radius = if (size == DeckCardSize.Small) 7.dp else 12.dp
    Box(
        modifier.size(size.width, size.height).drawBehind {
            val r = CornerRadius(radius.toPx())
            drawRoundRect(Color(0xFF161A45), cornerRadius = r)
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFF2A2F6B), Color(0xFF3F46A0), Color(0xFF2A2F6B)), Offset.Zero, Offset(this.size.width, this.size.height)), topLeft = Offset.Zero, size = Size(this.size.width, this.size.height - 4.dp.toPx()), cornerRadius = r)
            drawRoundRect(Color(0xFFB6F24A), topLeft = Offset.Zero, size = Size(this.size.width, this.size.height - 4.dp.toPx()), cornerRadius = r, style = Stroke(2.5.dp.toPx()))
        },
        contentAlignment = Alignment.Center,
    ) { Text("L", color = Color(0xFFB6F24A), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = size.glyph) }
}
