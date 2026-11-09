package com.bumbaiya.lanplay.game.combinemaths

/**
 * What a player is holding. Digits are the number cards (0-9), operators are the red cards.
 * [wilds] is the number of "*" cards, each of which can stand in for any operator.
 */
data class Hand(
    val digits: List<Int>,
    val operators: List<Op>,
    val wilds: Int = 0,
)

/** The red operator cards. */
enum class Op { Plus, Minus, Pi, Sigma, Sqrt }

sealed interface EvalResult {
    data class Value(val value: Rational) : EvalResult
    data class Invalid(val reason: String) : EvalResult
}

/**
 * Checks and evaluates an expression a player typed.
 *
 * The expression is a plain string using these symbols:
 *   digits 0-9    number cards. Digits side by side join into one number (3 4 = 34).
 *   + -           the "+" card (add) and the "-" card (subtract)
 *   x /           the "+" card tilted (multiply) and the "-" card tilted (divide)
 *   S(a,b,c) P(a,b,c)  the Sigma card (adds two or more numbers) and the Pi card (multiplies
 *                 two or more numbers). Each is one card however many numbers it takes.
 *   r             the square-root card, applied to what follows it: r9 = 3, r(4+5) = 3
 *   ^             power: one number card placed over another, 4^3 = 64. Both must be single
 *                 cards (not joined digits or brackets). Needs no operator card.
 *   ( )           free brackets, no card needed
 *
 * Rules enforced: every digit must be a card you hold, every operator must be a card you
 * hold (a wild "*" can cover any missing one), and at least one operator card must be used.
 */
object ExpressionEvaluator {

    fun evaluate(expression: String, hand: Hand): EvalResult {
        val text = expression.filterNot { it.isWhitespace() }
        if (text.isEmpty()) return EvalResult.Invalid("Empty expression")

        checkCards(text, hand)?.let { return EvalResult.Invalid(it) }

        val parser = Parser(text)
        val value = parser.parseAll() ?: return EvalResult.Invalid(parser.error ?: "Invalid expression")
        return EvalResult.Value(value)
    }

    /** Returns an error message if the cards used are not all in the hand, else null. */
    private fun checkCards(text: String, hand: Hand): String? {
        val digitsLeft = hand.digits.toMutableList()
        val opsNeeded = mutableMapOf<Op, Int>()
        var operatorsUsed = 0

        for (c in text) {
            when (c) {
                in '0'..'9' -> if (!digitsLeft.remove(c - '0')) return "You don't have a $c card"
                '+' -> opsNeeded.merge(Op.Plus, 1, Int::plus)
                '-' -> opsNeeded.merge(Op.Minus, 1, Int::plus)
                'x' -> opsNeeded.merge(Op.Plus, 1, Int::plus)   // tilted +
                '/' -> opsNeeded.merge(Op.Minus, 1, Int::plus)  // tilted -
                'P' -> opsNeeded.merge(Op.Pi, 1, Int::plus)
                'S' -> opsNeeded.merge(Op.Sigma, 1, Int::plus)
                'r' -> opsNeeded.merge(Op.Sqrt, 1, Int::plus)
                '^', '(', ')', ',' -> Unit
                else -> return "Unknown symbol '$c'"
            }
            if (c in "+-x/PSr") operatorsUsed++
        }
        if (operatorsUsed == 0) return "Use at least one operator card"

        // One card, one use: count how many of each operator the text needs, then let wilds cover gaps.
        var wildsLeft = hand.wilds
        for ((op, need) in opsNeeded) {
            val have = hand.operators.count { it == op }
            val missing = need - have
            if (missing > 0) {
                wildsLeft -= missing
                if (wildsLeft < 0) return "You don't have enough ${op.name} cards"
            }
        }
        return null
    }

    /**
     * Recursive-descent parser. Each function handles one precedence level, loosest first:
     * add/subtract, then multiply/divide, then square root, then power, then numbers/brackets.
     * Returns null (and sets [error]) the moment anything is invalid.
     */
    private class Parser(private val s: String) {
        private var i = 0
        var error: String? = null

        fun parseAll(): Rational? {
            val v = sum() ?: return null
            if (i != s.length) return fail("Unexpected '${s[i]}'")
            return v
        }

        private fun fail(msg: String): Rational? { if (error == null) error = msg; return null }

        private fun sum(): Rational? {
            var left = product() ?: return null
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val right = product() ?: return null
                left = if (op == '-') left - right else left + right
            }
            return left
        }

        private fun product(): Rational? {
            var left = unary() ?: return null
            while (i < s.length && (s[i] == 'x' || s[i] == '/')) {
                val op = s[i++]
                val right = unary() ?: return null
                left = if (op == '/') {
                    left.divOrNull(right) ?: return fail("Division by zero")
                } else left * right
            }
            return left
        }

        private fun unary(): Rational? {
            if (i < s.length && s[i] == 'r') {
                i++
                val inner = unary() ?: return null
                return inner.sqrtOrNull() ?: fail("Square root is not a whole fraction")
            }
            return power()
        }

        /** Sigma / Pi: S(1,2,3) adds, P(2,3,4) multiplies. Needs at least two numbers. */
        private fun multiArg(): Rational? {
            val isSum = s[i++] == 'S'
            if (i >= s.length || s[i] != '(') return fail("Write the numbers in brackets, like S(1,2,3)")
            i++
            val first = sum() ?: return null
            var acc = first
            var count = 1
            while (i < s.length && s[i] == ',') {
                i++
                val next = sum() ?: return null
                acc = if (isSum) acc + next else acc * next
                count++
            }
            if (count < 2) return fail("Sigma and Pi need two or more numbers")
            if (i >= s.length || s[i] != ')') return fail("Missing )")
            i++
            lastWasSingleCard = false
            return acc
        }

        private fun power(): Rational? {
            val base = atom() ?: return null
            if (i < s.length && s[i] == '^') {
                if (!lastWasSingleCard) return fail("Power needs a single number card as the base")
                i++
                val exp = atom() ?: return null
                if (!lastWasSingleCard) return fail("Power needs a single number card as the exponent")
                return base.powOrNull(exp) ?: fail("Power not allowed")
            }
            return base
        }

        /** True when the last number read was exactly one digit card (not joined digits or a bracket). */
        private var lastWasSingleCard = false

        private fun atom(): Rational? {
            lastWasSingleCard = false
            if (i >= s.length) return fail("Expression ended too early")
            if (s[i] == 'S' || s[i] == 'P') return multiArg()
            if (s[i] == '(') {
                i++
                val v = sum() ?: return null
                if (i >= s.length || s[i] != ')') return fail("Missing )")
                i++
                return v
            }
            if (s[i].isDigit()) {
                val start = i
                while (i < s.length && s[i].isDigit()) i++
                lastWasSingleCard = i - start == 1
                return Rational.of(java.math.BigInteger(s.substring(start, i)), java.math.BigInteger.ONE)
            }
            return fail("Unexpected '${s[i]}'")
        }
    }
}
