package com.bumbaiya.lanplay.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bumbaiya.lanplay.ui.components.BigButton
import com.bumbaiya.lanplay.ui.components.DI
import com.bumbaiya.lanplay.ui.components.SectionLabel
import com.bumbaiya.lanplay.ui.theme.SpaceGrotesk
import com.bumbaiya.lanplay.ui.theme.lanPlayColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.bumbaiya.lanplay.ui.components.IconLabel
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bumbaiya.lanplay.game.combinemaths.EvalResult
import com.bumbaiya.lanplay.game.combinemaths.ExpressionEvaluator
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.ui.components.CardKind
import com.bumbaiya.lanplay.ui.components.DeckCard
import com.bumbaiya.lanplay.ui.components.DeckCardSize
import kotlin.math.roundToInt

/** A drag in progress: which card, and where its top-left corner is (in window coordinates). */
private class Drag(val card: BCard, var topLeft: Offset)

/** Where the builder's colours and wording differ between the table, the catch screen and solo. */
enum class BuilderStyle { Table, Catch }

@OptIn(ExperimentalLayoutApi::class)
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
    style: BuilderStyle = BuilderStyle.Table,
    cardsTitle: String = "", cardsHint: String = "", proofHint: String = "",
    /** Replaces the default Submit/Clear row. Receives submit and clear actions and whether Submit is locked. */
    actions: (@Composable (submit: () -> Unit, clear: () -> Unit, locked: Boolean) -> Unit)? = null,
) {
    var drag by remember { mutableStateOf<Drag?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // A wrong submission locks the Submit button for a short penalty.
    var locked by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(locked) {
        if (locked) { kotlinx.coroutines.delay(penaltyMs); locked = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }       // where this whole Box sits
    var trayBounds by remember { mutableStateOf(Rect.Zero) }      // the expression strip area
    val slotBounds = remember { mutableMapOf<Int, Rect>() }      // each slot's area, by slot id
    var cardPx by remember { mutableStateOf(Size(48f, 66f)) }
    val colors = MaterialTheme.lanPlayColors
    val stripColor = if (style == BuilderStyle.Catch) colors.danger else MaterialTheme.colorScheme.primary

    /** The slot under the dragged card's centre, if the card is a number and could be raised there. */
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

    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(if (style == BuilderStyle.Catch) 20.dp else 12.dp)) {
            if (style == BuilderStyle.Catch) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(cardsTitle)
                CardRow(null, colors.numberLabel, state.owned(), state, drag, { drag = it }, { drop(it) }, { cardPx = it }, origin, DeckCardSize.Large, 10.dp)
                if (cardsHint.isNotEmpty()) Text(cardsHint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(if (style == BuilderStyle.Catch) 10.dp else 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel(if (style == BuilderStyle.Catch) "Your proof" else "Your expression")
                    if (style == BuilderStyle.Table) androidx.compose.material3.TextButton(onClick = ::clear, modifier = Modifier.heightIn(min = 44.dp)) {
                        Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // The expression strip: tap or drop cards here. Scrolls sideways when it gets long.
                val proof = style == BuilderStyle.Catch
                Row(
                    modifier = Modifier
                        .fillMaxWidth().heightIn(min = if (proof) 96.dp else 76.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .border(2.dp, stripColor, RoundedCornerShape(14.dp))
                        .onGloballyPositioned { trayBounds = it.boundsInRoot() }
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = if (proof) 14.dp else 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hovered = hoveredSlotIndex(drag)
                    state.slots.forEachIndexed { index, slot ->
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, end = 10.dp) // always reserve room for a raised card so the row never jumps
                                .onGloballyPositioned { slotBounds[slot.id] = it.boundsInRoot() }
                                .then(if (index == hovered) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier),
                        ) {
                            val c = slot.card
                            DeckCard(
                                glyph = c.glyph, altGlyph = c.alt, kind = c.kind, size = if (proof) DeckCardSize.Proof else DeckCardSize.Row,
                                onClick = { if (c.canTilt) state.tiltInSlot(index) else state.returnToHand(index) },
                                onLongClick = { state.returnToHand(index) },
                            )
                            slot.exp?.let { e ->
                                // The raised card: small, up and to the right. Tap it to un-raise.
                                DeckCard(
                                    glyph = e.glyph, kind = e.kind, size = DeckCardSize.Small,
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-12).dp),
                                    onClick = { state.unraise(index) },
                                )
                            }
                        }
                    }
                    // The empty dashed slot at the end: where the next card goes.
                    val dash = MaterialTheme.lanPlayColors.cardBorder
                    Box(Modifier.padding(top = 12.dp).size(if (proof) 48.dp else 40.dp, if (proof) 62.dp else 54.dp).drawBehind {
                        drawRoundRect(dash, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
                    })
                }
                val owned = state.owned()
                val used = owned.count { state.isUsed(it) }
                if (style == BuilderStyle.Table) Text(
                    "$used cards used, ${owned.size - used} unused. Using every card is optional.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (liveResult) {
                    // The admin turned on the live result: always show a clear box so players can see it is on.
                    val r = if (state.slots.isEmpty()) null else ExpressionEvaluator.evaluate(state.expression(), engineHand())
                    val value = (r as? EvalResult.Value)?.value?.toString()
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.lanPlayColors.successContainer, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Result", fontSize = 13.sp, color = MaterialTheme.lanPlayColors.onSuccessContainer)
                        Text(
                            when { value != null -> "= $value"; state.slots.isEmpty() -> "Build a row to see its result"; else -> "= ?  (not complete yet)" },
                            fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = if (value != null) 22.sp else 14.sp,
                            color = MaterialTheme.lanPlayColors.onSuccessContainer,
                        )
                    }
                }
                message?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
                if (style == BuilderStyle.Catch && proofHint.isNotEmpty()) Text(proofHint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (style == BuilderStyle.Table) {
                val owned = state.owned()
                val numbers = owned.filter { it.kind == CardKind.Number }
                val operators = owned.filter { it.kind == CardKind.Operator }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Your cards · ${owned.size}")
                    CardRow("Numbers · ${numbers.size}", colors.numberLabel, numbers, state, drag, { drag = it }, { drop(it) }, { cardPx = it }, origin)
                    CardRow("Operators · ${operators.size}", colors.operatorLabel, operators, state, drag, { drag = it }, { drop(it) }, { cardPx = it }, origin)
                }
            }

            if (actions != null) actions(::submit, ::clear, locked || !enabled)
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton(if (locked) "Wait..." else submitLabel, ::submit, Modifier.weight(1f), icon = DI.Check, enabled = !locked && enabled, height = 52.dp)
                BigButton("Clear", ::clear, icon = DI.Close, primary = false, height = 52.dp, modifier = Modifier.width(120.dp))
            }
        }

        // The card being dragged floats above everything, following the finger.
        drag?.let { d ->
            Box(Modifier.offset { IntOffset((d.topLeft.x - origin.x).roundToInt(), (d.topLeft.y - origin.y).roundToInt()) }) {
                DeckCard(d.card.glyph, d.card.kind, altGlyph = d.card.alt)
            }
        }
    }
}

@Composable
private fun CardRow(
    label: String?, labelColor: androidx.compose.ui.graphics.Color, cards: List<BCard>, state: BuilderState,
    drag: Drag?, setDrag: (Drag?) -> Unit, onDrop: (Drag) -> Unit, reportSize: (Size) -> Unit, origin: Offset,
    size: DeckCardSize = DeckCardSize.Medium, gap: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).heightIn(min = 66.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            cards.forEach { card ->
                if (state.isUsed(card)) {
                    // Used cards stay in place but faded; tap one to take it back.
                    DeckCard(card.glyph, card.kind, altGlyph = card.alt, size = size, dimmed = true, onClick = { state.giveBack(card) })
                } else HandCard(card, drag, state, setDrag, onDrop, reportSize, origin, size)
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
    origin: Offset,
    size: DeckCardSize = DeckCardSize.Medium,
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
            glyph = card.glyph, altGlyph = card.alt, kind = card.kind, size = size, dimmed = isDragged,
            onClick = { state.addSlot(card) },
        )
    }
}
