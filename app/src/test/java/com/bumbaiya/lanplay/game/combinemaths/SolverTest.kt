package com.bumbaiya.lanplay.game.combinemaths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolverTest {
    @Test fun findsSimpleTargets() {
        assertTrue(Solver.canReach(Hand(listOf(3, 4), listOf(Op.Plus)), 7))
        assertTrue(Solver.canReach(Hand(listOf(3, 4), listOf(Op.Plus)), 12)) // tilted + is x
        assertFalse(Solver.canReach(Hand(listOf(4, 3), listOf(Op.Plus)), 64)) // 4^3 needs no operator card, but every answer must use one
        assertTrue(Solver.canReach(Hand(listOf(1, 2, 3), listOf(Op.Sigma)), 6))
        assertTrue(Solver.canReach(Hand(listOf(2, 3, 4), listOf(Op.Pi)), 24))
    }

    @Test fun reportsDistanceWhenOutOfReach() {
        val h = Hand(listOf(1, 1), listOf(Op.Plus))
        assertFalse(Solver.canReach(h, 500))
        assertTrue(Solver.distance(h, 500) > 0)
    }

    @Test fun needsAnOperator() {
        assertFalse(Solver.canReach(Hand(listOf(3, 4), emptyList()), 34))
    }

    /** With the leaning draws, a round whose target looks hard still becomes reachable quickly. */
    @Test fun roundsBecomeSolvable() {
        val engine = GameEngine(Random(7))
        val rnd = Random(42)
        var solved = 0
        val trials = 60
        repeat(trials) {
            val digits = List(1 + rnd.nextInt(3)) { rnd.nextInt(0, 10) }
            val base = engine.newGame("R", "a", "Asha").let { (engine.addPlayer(it, "b", "Bo") as Outcome.Ok).state }
            var s = base.copy(
                phase = Phase.Turns("a", hasDrawn = false, turnEndsAt = Long.MAX_VALUE / 2), target = digits,
                hands = mapOf("a" to RoundPlayer(listOf(GameCard(1, digit = rnd.nextInt(0, 10)), GameCard(2, op = Op.Plus))), "b" to RoundPlayer(listOf(GameCard(3, digit = 5), GameCard(4, op = Op.Minus)))),
                nextCardId = 10,
            )
            val target = s.targetValue!!
            var turns = 0
            while (turns < 14 && s.phase is Phase.Turns) {
                val who = (s.phase as Phase.Turns).currentId
                val number = rnd.nextBoolean()
                s = (engine.apply(s, who, Intent.DrawCard(number), 0) as? Outcome.Ok)?.state ?: break
                val rp = s.hands.getValue(who)
                if (Solver.canReach(Hand(rp.hand.mapNotNull { it.digit }, rp.hand.mapNotNull { it.op }), target, 40)) { solved++; return@repeat }
                s = (engine.apply(s, who, Intent.Pass, 0) as? Outcome.Ok)?.state ?: break
                turns++
            }
        }
        println("solved $solved of $trials")
        assertTrue("only $solved of $trials solvable", solved >= trials * 8 / 10)
    }

    /** For comparison only: the same rounds with purely random draws. */
    @Test fun baselineWithPureLuck() {
        val rnd = Random(42)
        val pool = listOf(Op.Plus, Op.Minus, Op.Pi, Op.Sigma, Op.Sqrt, Op.Wild)
        var solved = 0
        val trials = 60
        repeat(trials) {
            val digits = List(1 + rnd.nextInt(3)) { rnd.nextInt(0, 10) }
            val target = digits.joinToString("").toLong()
            val d = mutableListOf(rnd.nextInt(0, 10)); val o = mutableListOf(Op.Plus)
            for (turn in 1..14) {
                if (rnd.nextBoolean()) d += rnd.nextInt(0, 10) else o += pool[rnd.nextInt(pool.size)]
                if (Solver.canReach(Hand(d, o), target, 40)) { solved++; break }
            }
        }
        println("baseline solved $solved of $trials")
    }
}
