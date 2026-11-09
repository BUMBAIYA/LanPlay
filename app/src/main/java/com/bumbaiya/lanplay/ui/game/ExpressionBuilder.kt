package com.bumbaiya.lanplay.ui.game

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
    submitLabel: String = "Submit",
    /** Shows the running result under the row. Off by default: the game is about mental maths. */
    liveResult: Boolean = false,
    enabled: Boolean = true,
) {
    var drag by remember { mutableStateOf<Drag?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    // A wrong submission locks the Submit button for a short penalty.
    var locked by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(locked) {
        if (locked) { kotlinx.coroutines.delay(penaltyMs); locked = false }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }       // where this whole Box sits
    var trayBounds by remember { mutableStateOf(Rect.Zero) }      // the slot row area
    val slotBounds = remember { mutableMapOf<Int, Rect>() }      // each slot's area, by slot id
    val cardSize = Size(48f, 66f) // approximate; replaced by real pixel size below
    var cardPx by remember { mutableStateOf(cardSize) }

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

    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("YOUR ROW", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // The tray: drop cards here to place them. Cards already here can be tapped.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .onGloballyPositioned { trayBounds = it.boundsInRoot() },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.slots.isEmpty()) {
                    Text("Drag cards here. Hold a number over another to raise it (power).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val hovered = hoveredSlotIndex(drag)
                state.slots.forEachIndexed { index, slot ->
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, end = 12.dp) // always reserve room for a raised card so rows never jump
                            .onGloballyPositioned { slotBounds[slot.id] = it.boundsInRoot() }
                            .then(if (index == hovered) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(11.dp)) else Modifier),
                    ) {
                        val c = slot.card
                        DeckCard(
                            glyph = c.glyph, altGlyph = c.alt, kind = c.kind,
                            onClick = { if (c.canTilt) state.tiltInSlot(index) else state.returnToHand(index) },
                            onLongClick = { state.returnToHand(index) },
                        )
                        slot.exp?.let { e ->
                            // The raised card: small, up and to the right. Tap it to un-raise.
                            DeckCard(
                                glyph = e.glyph, kind = e.kind, size = DeckCardSize.Small,
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp),
                                onClick = { state.unraise(index) },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = !locked && enabled,
                    onClick = {
                        if (onSubmit != null) { onSubmit(state.expression()); locked = true; message = null; return@Button }
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
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { IconLabel(Icons.Filled.Check, if (locked) "Wait..." else submitLabel, style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(
                    onClick = { state.clear(); message = null },
                    modifier = Modifier.heightIn(min = 52.dp),
                ) { IconLabel(Icons.Filled.Close, "Clear") }
            }
            if (liveResult && state.slots.isNotEmpty()) {
                val r = ExpressionEvaluator.evaluate(state.expression(), engineHand())
                if (r is EvalResult.Value) Text("= ${r.value}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }

            Text("YOUR CARDS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.hand.forEach { card -> HandCard(card, drag, state, { drag = it }, { drop(it) }, { cardPx = it }, origin) }
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
private fun HandCard(
    card: BCard,
    drag: Drag?,
    state: BuilderState,
    setDrag: (Drag?) -> Unit,
    onDrop: (Drag) -> Unit,
    reportSize: (Size) -> Unit,
    origin: Offset,
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
            glyph = card.glyph, altGlyph = card.alt, kind = card.kind, dimmed = isDragged,
            onClick = { if (card.canTilt) state.tiltInHand(card) },
        )
    }
}
