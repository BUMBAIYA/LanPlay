package com.bumbaiya.lanplay.ui.game

import androidx.compose.runtime.mutableStateListOf
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.CardKind

/**
 * One card in the builder. [glyph] is the ACTIVE face. Two-in-one cards also have [alt],
 * the face on the other side. [free] cards are brackets/commas: unlimited, not from the hand.
 */
data class BCard(
    val id: Int,
    val glyph: String,
    val kind: CardKind,
    val alt: String? = null,
    val op: Op? = null,
    val free: Boolean = false,
) {
    /** Flip a two-in-one card: swap which operator is active. */
    fun tilted() = if (alt == null) this else copy(glyph = alt, alt = glyph)
}

/** One position in the expression row. [exp] is a number card raised on top (the power). */
data class Slot(val id: Int, val card: BCard, val exp: BCard? = null)

/** All the builder's data and the rules for changing it. The screen just draws it. */
class BuilderState(initialHand: List<BCard>) {
    val hand = mutableStateListOf<BCard>().apply { addAll(initialHand) }
    val slots = mutableStateListOf<Slot>()
    private var nextSlotId = 1

    fun tiltInHand(card: BCard) {
        val i = hand.indexOfFirst { it.id == card.id }
        if (i >= 0) hand[i] = hand[i].tilted()
    }

    fun tiltInSlot(index: Int) {
        slots[index] = slots[index].copy(card = slots[index].card.tilted())
    }

    private fun take(card: BCard) { if (!card.free) hand.removeAll { it.id == card.id } }

    fun addSlot(card: BCard) { take(card); slots.add(Slot(nextSlotId++, card)) }

    /** Put [card] on top of the number in slot [index] as its exponent: base^card. */
    fun raise(card: BCard, index: Int) {
        take(card)
        slots[index] = slots[index].copy(exp = card)
    }

    /** Remove the raise: the exponent card becomes its own slot right after the base. */
    fun unraise(index: Int) {
        val s = slots[index]
        val e = s.exp ?: return
        slots[index] = s.copy(exp = null)
        slots.add(index + 1, Slot(nextSlotId++, e))
    }

    fun returnToHand(index: Int) {
        val s = slots.removeAt(index)
        listOfNotNull(s.card, s.exp).forEach { if (!it.free) hand.add(it) }
    }

    fun clear() { while (slots.isNotEmpty()) returnToHand(slots.lastIndex) }

    /** Turns the slot row into the text the rules engine understands. */
    fun expression(): String = slots.joinToString("") { s ->
        symbol(s.card) + (s.exp?.let { "^" + symbol(it) } ?: "")
    }

    private fun symbol(c: BCard) = when (c.glyph) {
        "×" -> "x"; "÷" -> "/"; "Π" -> "P"; "Σ" -> "S"; "√" -> "r"; "−" -> "-"
        else -> c.glyph
    }
}
