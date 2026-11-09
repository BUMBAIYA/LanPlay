package com.bumbaiya.lanplay.ui.game

import com.bumbaiya.lanplay.game.combinemaths.GameCard
import com.bumbaiya.lanplay.game.combinemaths.Hand
import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.CardKind

/** Turns a game card into the card the builder draws. */
fun GameCard.toBCard(): BCard = when {
    digit != null -> BCard(id, digit.toString(), CardKind.Number)
    op == Op.Plus -> BCard(id, "+", CardKind.Operator, alt = "×", op = Op.Plus)
    op == Op.Minus -> BCard(id, "−", CardKind.Operator, alt = "÷", op = Op.Minus)
    op == Op.Pi -> BCard(id, "Π", CardKind.Operator, op = Op.Pi)
    op == Op.Sigma -> BCard(id, "Σ", CardKind.Operator, op = Op.Sigma)
    op == Op.Sqrt -> BCard(id, "√", CardKind.Operator, op = Op.Sqrt)
    else -> BCard(id, "*", CardKind.Operator, op = Op.Wild, wild = true)
}

/** The rules-engine view of a set of builder cards (used for the optional live result). */
fun List<BCard>.toEngineHand() = Hand(
    digits = filter { it.kind == CardKind.Number }.mapNotNull { it.glyph.toIntOrNull() },
    operators = mapNotNull { it.op },
)
