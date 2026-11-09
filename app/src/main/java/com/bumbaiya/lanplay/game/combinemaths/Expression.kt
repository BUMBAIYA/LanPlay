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
enum class Op { Plus, Minus, Pi, Sigma, Sqrt, Wild }

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
 *   S123 P234     the Sigma card (adds the number cards after it) and the Pi card (multiplies
 *                 them). Each is one card however many numbers it takes, at least two.
 *                 Digits after S or P are separate numbers: S123 = 1+2+3.
 *   r             the square-root card, applied to what follows it: r9 = 3, r(4+5) = 3
 *   ^             power: one number card placed over another, 4^3 = 64. Both must be single
 *                 cards (not joined digits or brackets). Needs no operator card.
 *   ( )           free brackets, no card needed
 *
 * Rules enforced: every digit must be a card you hold, every operator must be a card you
 * hold (a wild "*" can cover any missing one), and at least one operator card must be used.
 */
object ExpressionEvaluator {

    /**
     * Judges an expression. When a [target] is given and the text has no explicit brackets, the
     * rules' "imagine brackets anywhere" applies: the answer counts if ANY grouping of the cards
     * in the order placed reaches the target.
     */
    fun evaluate(expression: String, hand: Hand, target: Rational? = null): EvalResult {
        val text = expression.filterNot { it.isWhitespace() }
        if (text.isEmpty()) return EvalResult.Invalid("Empty expression")

        checkCards(text, hand)?.let { return EvalResult.Invalid(it) }

        if (target != null && '(' !in text) {
            val reachable = Groupings(text).values()
            if (reachable != null && target in reachable) return EvalResult.Value(target)
        }

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
        var wildsLeft = hand.wilds + hand.operators.count { it == Op.Wild }
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
        /**
         * Sigma / Pi: take every number card that follows them, each as its own number
         * (S123 = 1+2+3, P234 = 2x3x4). A number may carry a raised power (S2^3 4).
         */
        private fun multiArg(): Rational? {
            val isSum = s[i++] == 'S'
            var acc: Rational? = null
            var count = 0
            while (i < s.length && s[i].isDigit()) {
                var term = Rational.of((s[i++] - '0').toLong())
                if (i + 1 < s.length && s[i] == '^' && s[i + 1].isDigit()) {
                    i++
                    val e = Rational.of((s[i++] - '0').toLong())
                    term = term.powOrNull(e) ?: return fail("Power not allowed")
                }
                acc = if (acc == null) term else if (isSum) acc + term else acc * term
                count++
            }
            if (count < 2) return fail("Sigma and Pi need two or more number cards after them")
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

    /**
     * Every value the cards can make when brackets may go anywhere ("imagine brackets"). The text
     * is split into pieces (numbers, Sigma/Pi groups, and the + - x / and square-root signs), then
     * every way of grouping neighbouring pieces is tried. Returns null if the text is malformed.
     */
    private class Groupings(private val text: String) {
        private sealed interface Tok
        private class Num(val v: Rational) : Tok
        private class Bin(val c: Char) : Tok
        private object Root : Tok

        private val toks = ArrayList<Tok>()
        private val memo = HashMap<Long, Set<Rational>>()

        private fun digitAt(i: Int) = i < text.length && text[i].isDigit()

        private fun tokenize(): Boolean {
            var i = 0
            while (i < text.length) {
                val c = text[i]
                when {
                    c.isDigit() -> {
                        val start = i
                        while (digitAt(i)) i++
                        var v = Rational.of(java.math.BigInteger(text.substring(start, i)), java.math.BigInteger.ONE)
                        if (i < text.length && text[i] == '^') {
                            if (i - start != 1 || !digitAt(i + 1) || digitAt(i + 2)) return false
                            v = v.powOrNull(Rational.of((text[i + 1] - '0').toLong())) ?: return false
                            i += 2
                        }
                        toks += Num(v)
                    }
                    c == 'S' || c == 'P' -> {
                        i++
                        var acc: Rational? = null
                        var count = 0
                        while (digitAt(i)) {
                            var t = Rational.of((text[i++] - '0').toLong())
                            if (i + 1 < text.length && text[i] == '^' && digitAt(i + 1)) {
                                t = t.powOrNull(Rational.of((text[i + 1] - '0').toLong())) ?: return false
                                i += 2
                            }
                            acc = if (acc == null) t else if (c == 'S') acc + t else acc * t
                            count++
                        }
                        if (count < 2) return false
                        toks += Num(acc!!)
                    }
                    c == 'r' -> { toks += Root; i++ }
                    c in "+-x/" -> { toks += Bin(c); i++ }
                    else -> return false
                }
            }
            return toks.isNotEmpty()
        }

        fun values(): Set<Rational>? {
            if (!tokenize()) return null
            return range(0, toks.size)
        }

        private fun range(from: Int, to: Int): Set<Rational> {
            val key = from.toLong() * 1000 + to
            memo[key]?.let { return it }
            val out = HashSet<Rational>()
            val first = toks[from]
            if (to - from == 1) {
                if (first is Num) out += first.v
            } else {
                if (first is Root) range(from + 1, to).forEach { r -> r.sqrtOrNull()?.let(out::add) }
                for (k in from + 1 until to - 1) {
                    val op = toks[k] as? Bin ?: continue
                    val left = range(from, k)
                    if (left.isEmpty()) continue
                    val right = range(k + 1, to)
                    for (a in left) for (b in right) {
                        when (op.c) {
                            '+' -> out += a + b
                            '-' -> out += a - b
                            'x' -> out += a * b
                            else -> a.divOrNull(b)?.let(out::add)
                        }
                        if (out.size > 2000) break
                    }
                }
            }
            memo[key] = out
            return out
        }
    }
}
