package com.bumbaiya.lanplay.ui.game

import com.bumbaiya.lanplay.game.combinemaths.Op
import com.bumbaiya.lanplay.ui.components.CardKind
import kotlin.random.Random

/**
 * There is no deck: a player can keep drawing, and every draw is random.
 * Each card gets a fresh id so the builder can tell two "3" cards apart.
 */
class CardPool(private val random: Random = Random.Default) {
    private var nextId = 1000

    fun drawNumber(): BCard = BCard(nextId++, random.nextInt(0, 10).toString(), CardKind.Number)

    fun drawOperator(): BCard {
        val id = nextId++
        return when (random.nextInt(6)) {
            0 -> BCard(id, "+", CardKind.Operator, alt = "×", op = Op.Plus)
            1 -> BCard(id, "−", CardKind.Operator, alt = "÷", op = Op.Minus)
            2 -> BCard(id, "Π", CardKind.Operator, op = Op.Pi)
            3 -> BCard(id, "Σ", CardKind.Operator, op = Op.Sigma)
            4 -> BCard(id, "√", CardKind.Operator, op = Op.Sqrt)
            else -> BCard(id, "*", CardKind.Operator, op = Op.Wild, wild = true)
        }
    }

    /** A number that can sit on the target row (digits only). */
    fun randomDigit(): Int = random.nextInt(0, 10)
}
