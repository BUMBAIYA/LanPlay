package com.bumbaiya.lanplay.game.combinemaths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun value(expr: String, hand: Hand): Rational? =
        (ExpressionEvaluator.evaluate(expr, hand) as? EvalResult.Value)?.value

    private fun isInvalid(expr: String, hand: Hand) =
        ExpressionEvaluator.evaluate(expr, hand) is EvalResult.Invalid

    private val hand = Hand(digits = listOf(3, 4, 9), operators = listOf(Op.Plus, Op.Minus, Op.Sqrt))

    @Test fun digitsJoinIntoOneNumber() {
        assertEquals(Rational.of(43), value("34+9", hand))
        assertEquals(Rational.of(43 - 9), value("43-9", hand))
    }

    @Test fun tiltedPlusMultipliesAndTiltedMinusDivides() {
        assertEquals(Rational.of(12), value("3x4", hand.copy(operators = listOf(Op.Plus))))
        assertEquals(Rational.of(3), value("9/3", hand))
    }

    @Test fun powerNeedsNoOperatorCardButAnOperatorIsStillRequired() {
        assertTrue(isInvalid("4^3", hand.copy(operators = emptyList())))
        assertEquals(Rational.of(64 + 9 - 9), value("4^3+9-9", hand.copy(digits = listOf(4, 3, 9, 9))))
    }

    @Test fun theTourPowerStepIsSolvable() {
        assertEquals(Rational.of(64), value("4^3+0", Hand(listOf(4, 3, 0), listOf(Op.Plus))))
    }

    @Test fun powerMatchesThePdfExamples() {
        assertEquals(Rational.of(64), value("4^3+r9-r9", Hand(listOf(4, 3, 9, 9), listOf(Op.Plus, Op.Sqrt, Op.Minus, Op.Sqrt))))
        assertEquals(Rational.of(81), value("3^4+3-3", Hand(listOf(3, 4, 3, 3), listOf(Op.Plus, Op.Minus))))
    }

    @Test fun squareRootMustBeExact() {
        assertEquals(Rational.of(3), value("r9+3-3", Hand(listOf(9, 3, 3), listOf(Op.Sqrt, Op.Plus, Op.Minus))))
        assertTrue(isInvalid("r3+3", Hand(listOf(3, 3), listOf(Op.Sqrt, Op.Plus))))
    }

    @Test fun divisionStaysExact() {
        // 1/3 * 3 would be 0.999.. in floating point; with fractions it is exactly 1.
        val h = Hand(listOf(1, 3, 3), listOf(Op.Plus, Op.Minus))
        assertEquals(Rational.of(1), value("1/3x3", h))
    }

    @Test fun divisionByZeroIsInvalid() {
        assertTrue(isInvalid("3/0", Hand(listOf(3, 0), listOf(Op.Minus))))
    }

    @Test fun cannotUseACardYouDoNotHold() {
        assertTrue(isInvalid("5+3", hand))
        assertTrue(isInvalid("3+3", hand)) // only one 3
    }

    @Test fun cannotUseAnOperatorCardTwice() {
        assertTrue(isInvalid("3+4+9", hand)) // only one + card
    }

    @Test fun wildCardCoversAnyOperator() {
        val h = Hand(listOf(3, 4), emptyList(), wilds = 1)
        assertEquals(Rational.of(12), value("3x4", h))
        assertTrue(isInvalid("3x4+3", h.copy(digits = listOf(3, 4, 3)))) // needs two operators, one wild
    }

    @Test fun bracketsAreFree() {
        val h = Hand(listOf(2, 3, 4), listOf(Op.Plus, Op.Minus))
        assertEquals(Rational.of(2), value("(2+3)-3", h.copy(digits = listOf(2, 3, 3))))
    }

    @Test fun sigmaAddsTwoOrMoreWithOneCard() {
        val h = Hand(listOf(1, 2, 3, 4), listOf(Op.Sigma))
        assertEquals(Rational.of(10), value("S1234", h))
        assertEquals(Rational.of(3), value("S12", h))
        assertTrue(isInvalid("S1", h))
    }

    @Test fun piMultipliesTwoOrMoreWithOneCard() {
        val h = Hand(listOf(2, 3, 4), listOf(Op.Pi))
        assertEquals(Rational.of(24), value("P234", h))
        assertTrue(isInvalid("P2", h))
    }

    @Test fun plainOperatorsTakeExactlyTwoNumbers() {
        // one + card cannot chain three numbers
        assertTrue(isInvalid("2+3+4", Hand(listOf(2, 3, 4), listOf(Op.Plus))))
    }

    @Test fun powerNeedsSingleCardsOnBothSides() {
        val h = Hand(listOf(4, 3, 2), listOf(Op.Plus))
        assertTrue(isInvalid("(4+3)^2+0", h))
        assertTrue(isInvalid("43^2+0", h.copy(digits = listOf(4, 3, 2, 0))))
    }

    @Test fun malformedInputIsRejected() {
        assertTrue(isInvalid("", hand))
        assertTrue(isInvalid("3+", hand))
        assertTrue(isInvalid("(3+4", hand))
        assertTrue(isInvalid("3 ? 4", hand))
    }
}
