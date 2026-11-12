package com.bumbaiya.lanplay.ui.game

import androidx.compose.runtime.mutableStateListOf
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.CardKind

/**
 * One card in the builder. [glyph] is the ACTIVE face. Two-in-one cards also have [alt],
 * the face on the other side. [free] cards are unlimited cards that do not come from the hand.
 */
data class BCard(
    val id: Int,
    val glyph: String,
    val kind: CardKind,
    val alt: String? = null,
    val op: Op? = null,
    val free: Boolean = false,
    /** The wild "*" card: tapping cycles which operator it stands for. */
    val wild: Boolean = false,
) {
    val canTilt get() = alt != null || wild

    /** Tap: a two-in-one card swaps its operators, a wild card moves to the next operator. */
    fun tilted(): BCard = when {
        wild -> copy(glyph = WILD_CYCLE[(WILD_CYCLE.indexOf(glyph) + 1) % WILD_CYCLE.size])
        alt != null -> copy(glyph = alt, alt = glyph)
        else -> this
    }

    companion object {
        val WILD_CYCLE = listOf("+", "−", "×", "÷", "Σ", "Π", "√")
    }
}

/** One position in the expression row. [exp] is a number card raised on top (the power). */
data class Slot(val id: Int, val card: BCard, val exp: BCard? = null)

/** All the builder's data and the rules for changing it. The screen just draws it. */
class BuilderState(initialHand: List<BCard>) {
    val hand = mutableStateListOf<BCard>().apply { addAll(initialHand) }
    val slots = mutableStateListOf<Slot>()
    private var nextSlotId = 1

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

    /** Every real (non-free) card the player owns, in the hand or placed in the row. */
    fun allCards(): List<BCard> = hand + slots.flatMap { listOfNotNull(it.card, it.exp) }.filter { !it.free }

    fun addToHand(card: BCard) { hand.add(card) }

    /** Every real card the player owns in a stable order, whether it is in the hand or placed in the row. */
    fun owned(): List<BCard> = allCards().sortedBy { it.id }

    fun isUsed(card: BCard) = slots.any { it.card.id == card.id || it.exp?.id == card.id }

    /** Takes a placed card back to the hand (whether it sits in the row or is raised as a power). */
    fun giveBack(card: BCard) {
        val i = slots.indexOfFirst { it.card.id == card.id }
        if (i >= 0) { returnToHand(i); return }
        val j = slots.indexOfFirst { it.exp?.id == card.id }
        if (j >= 0) { val e = slots[j].exp!!; slots[j] = slots[j].copy(exp = null); hand.add(e) }
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
