package com.bumbaiya.lanplay.game.combinemaths

import kotlin.math.abs

/**
 * Finds out how close a hand can get to a target, using the same rules and judge as the game
 * (so anything it reports as reachable really is). It tries many arrangements of up to
 * [MAX_DIGITS] number cards with the operator cards held, and stops at a time limit, so it is
 * quick enough to use while a game runs. Not every exotic arrangement is tried, which means
 * "not found" can occasionally be wrong, but "found" never is.
 */
object Solver {
    const val MAX_DIGITS = 4

    /** Smallest distance between the target and anything this hand can make. 0 means it can be hit. */
    fun distance(hand: Hand, target: Long, timeLimitMs: Long = 60): Double {
        val deadline = System.nanoTime() + timeLimitMs * 1_000_000
        val want = Rational.of(target)
        var best = Double.MAX_VALUE
        val counts = IntArray(10).also { c -> hand.digits.forEach { c[it]++ } }
        val wild = hand.operators.count { it == Op.Wild } + hand.wilds > 0
        val canPlus = wild || Op.Plus in hand.operators
        val canMinus = wild || Op.Minus in hand.operators
        val pi = hand.operators.any { it == Op.Pi } || wild
        val sigma = hand.operators.any { it == Op.Sigma } || wild
        val sqrt = hand.operators.any { it == Op.Sqrt } || wild
        val seen = HashSet<String>()

        fun test(expr: String): Boolean {
            if (!seen.add(expr)) return false
            val r = ExpressionEvaluator.evaluate(expr, hand, want)
            if (r is EvalResult.Value) {
                val d = abs((r.value - want).let { it.num.toDouble() / it.den.toDouble() })
                if (d < best) best = d
            }
            return best == 0.0
        }
        fun timeUp() = System.nanoTime() > deadline

        // Digits with +, -, x, / between them, or placed side by side (joined) or stacked (power).
        fun dfs(expr: String, used: Int): Boolean {
            if (timeUp()) return true
            if (used >= 1 && test(expr)) return true
            if (used >= MAX_DIGITS) return false
            for (d in 0..9) {
                if (counts[d] == 0) continue
                counts[d]--
                var ok = if (used == 0) dfs("$d", 1) else dfs("$expr$d", used + 1) || dfs("$expr^$d", used + 1)
                if (used > 0 && !ok && canPlus) ok = dfs("$expr+$d", used + 1) || dfs("$expr" + "x$d", used + 1)
                if (used > 0 && !ok && canMinus) ok = dfs("$expr-$d", used + 1) || dfs("$expr/$d", used + 1)
                counts[d]++
                if (ok) return true
            }
            return false
        }
        if (dfs("", 0)) return best

        // Sigma / Pi over two to four separate numbers, and a square root of a joined number.
        fun groups(prefix: String, n: Int, expr: String = prefix, picked: Int = 0): Boolean {
            if (timeUp()) return true
            if (picked >= 2 && test(expr)) return true
            if (picked >= n) return false
            for (d in 0..9) {
                if (counts[d] == 0) continue
                counts[d]--
                val ok = groups(prefix, n, "$expr$d", picked + 1)
                counts[d]++
                if (ok) return true
            }
            return false
        }
        if (sigma && groups("S", MAX_DIGITS)) return best
        if (pi && groups("P", MAX_DIGITS)) return best
        if (sqrt) for (a in 0..9) if (counts[a] > 0) {
            counts[a]--
            if (test("r$a")) { counts[a]++; return best }
            for (b in 0..9) if (counts[b] > 0 && test("r$a$b")) { counts[a]++; return best }
            counts[a]++
        }
        return best
    }

    fun canReach(hand: Hand, target: Long, timeLimitMs: Long = 60) = distance(hand, target, timeLimitMs) == 0.0
}
