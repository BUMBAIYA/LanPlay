package com.bumbaiya.lanplay.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.game.combinemaths.EvalResult
import com.bumbaiya.lanplay.game.combinemaths.ExpressionEvaluator
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import com.bumbaiya.lanplay.ui.components.fitScale
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** A drag in progress: which card, and where its top-left corner is (in window coordinates). */
private class Drag(val card: BCard, var topLeft: Offset)

/** Where the builder's colors and wording differ between the table, the catch screen and solo. */
enum class BuilderStyle { Table, Catch }

@Composable
fun ExpressionBuilder(
    state: BuilderState,
    engineHand: () -> Hand,
    target: Long,
    modifier: Modifier = Modifier,
    onHit: () -> Unit = {},
    penaltyMs: Long = 2000,
    /** When set, Submit hands the expression to this (the host judges it) instead of checking locally. */
    onSubmit: ((String) -> Unit)? = null,
    submitLabel: String = "Submit answer",
    /** Shows the running result under the row. Off by default: the game is about mental maths. */
    liveResult: Boolean = false,
    enabled: Boolean = true,
    /** How tall the [actions] area is, so the cards can use all the rest of the space. */
    actionsHeight: Dp = 58.dp,
    style: BuilderStyle = BuilderStyle.Table,
    /** Replaces the default Submit/Clear row. Receives submit and clear actions and whether Submit is locked. */
    actions: (@Composable (submit: () -> Unit, clear: () -> Unit, locked: Boolean) -> Unit)? = null,
) {
    var drag by remember { mutableStateOf<Drag?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // A wrong submission locks the Submit button for a short penalty.
    var locked by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(locked) {
        if (locked) { kotlinx.coroutines.delay(penaltyMs.milliseconds); locked = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }       // where this whole Box sits
    var trayBounds by remember { mutableStateOf(Rect.Zero) }      // the expression strip area
    val slotBounds = remember { mutableMapOf<Int, Rect>() }      // each slot's area, by slot id
    var cardPx by remember { mutableStateOf(Size(48f, 66f)) }
    val colors = MaterialTheme.lanPlayColors
    val stripColor = if (style == BuilderStyle.Catch) colors.danger else MaterialTheme.colorScheme.primary

    /** The slot under the dragged card's center, if the card is a number and could be raised there. */
    fun hoveredSlotIndex(d: Drag?): Int {
        if (d == null || d.card.kind != CardKind.Number) return -1
        val centre = d.topLeft + Offset(cardPx.width / 2, cardPx.height / 2)
        return state.slots.indexOfFirst { s ->
            s.card.kind == CardKind.Number && s.exp == null && slotBounds[s.id]?.contains(centre) == true
        }
    }

    fun drop(d: Drag) {
        val centre = d.topLeft + Offset(cardPx.width / 2, cardPx.height / 2)
        val hover = hoveredSlotIndex(d)
        when {
            hover >= 0 -> state.raise(d.card, hover)
            trayBounds.contains(centre) -> state.addSlot(d.card)
        }
        message = null
    }

    fun clear() { state.clear(); message = null }
    fun submit() {
        if (onSubmit != null) { onSubmit(state.expression()); locked = true; message = null; return }
        val r = ExpressionEvaluator.evaluate(state.expression(), engineHand(), com.bumbaiya.lanplay.game.combinemaths.Rational.of(target))
        if (r is EvalResult.Value && r.value.toString() == target.toString()) {
            message = null
            onHit()
        } else {
            locked = true
            message = when (r) {
                is EvalResult.Invalid -> "Not valid: ${r.reason}"
                is EvalResult.Value -> "That equals ${r.value}, not $target"
            } + " (+${penaltyMs / 1000}s penalty)"
        }
    }

    BoxWithConstraints(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        val proof = style == BuilderStyle.Catch
        // Nothing here scrolls. However many cards a player holds, they shrink and wrap to fit the
        // space the screen gives us (the buttons and the strip keep their room).
        val reserved = actionsHeight + 16.dp + (if (liveResult) 34.dp else 0.dp) + (if (message != null) 20.dp else 0.dp)
        val cardsBudget = (maxHeight - reserved).coerceAtLeast(110.dp)
        val ownedCount = state.owned().size
        val stripW = maxWidth - 20.dp - (if (style == BuilderStyle.Table) 70.dp else 0.dp)
        val stripCardW = if (proof) DeckCardSize.Proof.width else DeckCardSize.Row.width
        val stripCardH = if (proof) DeckCardSize.Proof.height else DeckCardSize.Row.height
        val stripScale = fitScale(state.slots.size + 1, stripCardW, stripCardH, 8.dp, stripW, cardsBudget * 0.5f - 8.dp, extraW = 10.dp, extraH = 10.dp, maxScale = 1.5f)
        // A short expression leaves most of the room to the hand; a long one splits it evenly.
        val handBudget = if (state.slots.size <= 4) cardsBudget - 84.dp else cardsBudget * 0.5f - 2.dp
        val handScale = fitScale(ownedCount.coerceAtLeast(1), DeckCardSize.Medium.width, DeckCardSize.Medium.height, 8.dp, maxWidth, handBudget.coerceAtLeast(60.dp), maxScale = 1.5f)
        Column(verticalArrangement = Arrangement.spacedBy(if (proof) 8.dp else 6.dp)) {
            // The victim's cards, when proving a catch.
            if (proof) CardRow(null, colors.numberLabel, state.owned(), state, drag, { drag = it }, { drop(it) }, { cardPx = it },
                DeckCardSize.Medium, 8.dp, handScale)

            // The expression strip: tap or drop cards here. Long expressions shrink and wrap.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    modifier = Modifier
                        .weight(1f).heightIn(min = if (proof) 80.dp else 76.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, stripColor, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { trayBounds = it.boundsInRoot() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hovered = hoveredSlotIndex(drag)
                    state.slots.forEachIndexed { index, slot ->
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, end = 10.dp) // always reserve room for a raised card so the row never jumps
                                .onGloballyPositioned { slotBounds[slot.id] = it.boundsInRoot() }
                                .then(if (index == hovered) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier),
                        ) {
                            val c = slot.card
                            DeckCard(
                                glyph = c.glyph, altGlyph = c.alt, kind = c.kind, size = if (proof) DeckCardSize.Proof else DeckCardSize.Row, scale = stripScale,
                                onClick = { if (c.canTilt) state.tiltInSlot(index) else state.returnToHand(index) },
                                onLongClick = { state.returnToHand(index) },
                            )
                            slot.exp?.let { e ->
                                // The raised card: small, up and to the right. Tap it to un-raise.
                                DeckCard(
                                    glyph = e.glyph, kind = e.kind, size = DeckCardSize.Small, scale = stripScale.coerceAtLeast(0.7f),
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp),
                                    onClick = { state.unraise(index) },
                                )
                            }
                        }
                    }
                    // The empty dashed slot at the end: where the next card goes.
                    val dash = MaterialTheme.lanPlayColors.cardBorder
                    Box(Modifier.padding(top = 10.dp).size((if (proof) 48.dp else 44.dp) * stripScale, (if (proof) 66.dp else 60.dp) * stripScale).drawBehind {
                        drawRoundRect(dash, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
                    })
                }
                if (style == BuilderStyle.Table) androidx.compose.material3.TextButton(onClick = ::clear, modifier = Modifier.heightIn(min = 36.dp)) {
                    Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (liveResult) {
                // The admin turned on the live result: always show a clear box so players can see it is on.
                val r = if (state.slots.isEmpty()) null else ExpressionEvaluator.evaluate(state.expression(), engineHand())
                val value = (r as? EvalResult.Value)?.value?.toString()
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.lanPlayColors.successContainer, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Result", fontSize = 12.sp, color = MaterialTheme.lanPlayColors.onSuccessContainer)
                    Text(
                        when { value != null -> "= $value"; state.slots.isEmpty() -> "Build a row to see its result"; else -> "= ?  (not complete yet)" },
                        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = if (value != null) 18.sp else 13.sp,
                        color = MaterialTheme.lanPlayColors.onSuccessContainer,
                    )
                }
            }
            message?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1) }

            if (style == BuilderStyle.Table) {
                val owned = state.owned()
                val numbers = owned.filter { it.kind == CardKind.Number }
                val operators = owned.filter { it.kind == CardKind.Operator }
                CardRow(null, colors.numberLabel, numbers + operators, state, drag, { drag = it }, { drop(it) }, { cardPx = it }, scale = handScale)
            }

            if (actions != null) actions(::submit, ::clear, locked || !enabled)
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(if (locked) "Wait..." else submitLabel, ::submit, Modifier.weight(1f), icon = DI.Check, enabled = !locked && enabled, height = 48.dp)
                BigButton("Clear", ::clear, icon = DI.Close, primary = false, height = 48.dp, modifier = Modifier.width(120.dp))
            }
        }

        // The card being dragged floats above everything, following the finger.
        drag?.let { d ->
            Box(Modifier.offset { IntOffset((d.topLeft.x - origin.x).roundToInt(), (d.topLeft.y - origin.y).roundToInt()) }) {
                DeckCard(d.card.glyph, d.card.kind, altGlyph = d.card.alt, scale = handScale)
            }
        }
    }
}

@Composable
private fun CardRow(
    label: String?, labelColor: Color, cards: List<BCard>, state: BuilderState,
    drag: Drag?, setDrag: (Drag?) -> Unit, onDrop: (Drag) -> Unit, reportSize: (Size) -> Unit,
    size: DeckCardSize = DeckCardSize.Medium, gap: androidx.compose.ui.unit.Dp = 8.dp, scale: Float = 1f,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        FlowRow(Modifier.fillMaxWidth().heightIn(min = 76.dp * scale).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
            cards.forEach { card ->
                if (state.isUsed(card)) {
                    // Used cards stay in place but faded; tap one to take it back.
                    DeckCard(card.glyph, card.kind, altGlyph = card.alt, size = size, scale = scale, dimmed = true, onClick = { state.giveBack(card) })
                } else HandCard(card, drag, state, setDrag, onDrop, reportSize, size, scale)
            }
        }
    }
}

@Composable
private fun HandCard(
    card: BCard,
    drag: Drag?,
    state: BuilderState,
    setDrag: (Drag?) -> Unit,
    onDrop: (Drag) -> Unit,
    reportSize: (Size) -> Unit,
    size: DeckCardSize = DeckCardSize.Medium,
    scale: Float = 1f,
) {
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    val isDragged = drag?.card?.id == card.id
    Box(
        modifier = Modifier
            .onGloballyPositioned { topLeft = it.positionInRoot(); reportSize(Size(it.size.width.toFloat(), it.size.height.toFloat())) }
            .pointerInput(card.id) {
                var current: Drag? = null
                detectDragGestures(
                    onDragStart = { current = Drag(card, topLeft).also(setDrag) },
                    onDrag = { change, amount ->
                        change.consume()
                        current?.let { d -> val moved = Drag(d.card, d.topLeft + amount); current = moved; setDrag(moved) }
                    },
                    onDragEnd = { current?.let(onDrop); current = null; setDrag(null) },
                    onDragCancel = { current = null; setDrag(null) },
                )
            },
    ) {
        // While dragging, the hand shows the card faded where it came from.
        DeckCard(
            glyph = card.glyph, altGlyph = card.alt, kind = card.kind, size = size, scale = scale, dimmed = isDragged,
            onClick = { state.addSlot(card) },
        )
    }
}
