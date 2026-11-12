package com.bumbaiya.lanplay.game.combinemaths

import java.math.BigInteger

/**
 * An exact fraction (numerator / denominator).
 *
 * Why not Double? In floating point 0.1 + 0.2 != 0.3, and 1/3 * 3 may not equal 1.
 * A game about exact mental math must never be wrong because of rounding, so every
 * calculation uses whole-number fractions that are always reduced (6/4 becomes 3/2).
 */
class Rational private constructor(val num: BigInteger, val den: BigInteger) {

    operator fun plus(o: Rational) = of(num * o.den + o.num * den, den * o.den)
    operator fun minus(o: Rational) = of(num * o.den - o.num * den, den * o.den)
    operator fun times(o: Rational) = of(num * o.num, den * o.den)

    /** Returns null when dividing by zero, so callers must handle the invalid case. */
    fun divOrNull(o: Rational): Rational? =
        if (o.num.signum() == 0) null else of(num * o.den, den * o.num)

    val isInteger get() = den == BigInteger.ONE
    val isZero get() = num.signum() == 0
    val isNegative get() = num.signum() < 0

    /** Integer square root (floor) by Newton's method; BigInteger.sqrt() needs API 33. */
    private fun isqrt(v: BigInteger): BigInteger {
        if (v.signum() == 0) return BigInteger.ZERO
        var x = BigInteger.ONE.shiftLeft((v.bitLength() + 1) / 2)
        while (true) {
            val y = (x + v / x).shiftRight(1)
            if (y >= x) return x
            x = y
        }
    }

    /** Exact square root, or null if the result is not a whole fraction (or negative). */
    fun sqrtOrNull(): Rational? {
        if (isNegative) return null
        val n = isqrt(num)
        val d = isqrt(den)
        return if (n * n == num && d * d == den) of(n, d) else null
    }

    /** Integer power. Only small whole exponents are allowed so the numbers stay bounded. */
    fun powOrNull(exp: Rational): Rational? {
        if (!exp.isInteger || exp.isNegative) return null
        val e = exp.num
        if (e > MAX_EXPONENT) return null
        if (isZero && e.signum() == 0) return null // 0^0 is undefined
        val p = e.toInt()
        return of(num.pow(p), den.pow(p))
    }

    override fun equals(other: Any?) = other is Rational && num == other.num && den == other.den
    override fun hashCode() = 31 * num.hashCode() + den.hashCode()
    override fun toString() = if (isInteger) num.toString() else "$num/$den"

    companion object {
        private val MAX_EXPONENT = BigInteger.valueOf(12)

        fun of(n: Long): Rational = Rational(BigInteger.valueOf(n), BigInteger.ONE)

        fun of(n: BigInteger, d: BigInteger): Rational {
            require(d.signum() != 0) { "zero denominator" }
            val g = n.gcd(d).let { if (it.signum() == 0) BigInteger.ONE else it }
            val sign = if (d.signum() < 0) BigInteger.ONE.negate() else BigInteger.ONE
            return Rational(n / g * sign, d / g * sign)
        }
    }
}
