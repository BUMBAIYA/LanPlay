package com.bumbaiya.lanplay.game.combinemaths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    private val engine = GameEngine(Random(1))
    private val t0 = 1_000_000L

    private fun lobby(): GameState {
        var s = engine.newGame("ROOM", "a", "Asha")
        s = (engine.addPlayer(s, "b", "Bo") as Outcome.Ok).state
        s = (engine.addPlayer(s, "c", "Cy") as Outcome.Ok).state
        return s
    }

    private fun ok(o: Outcome) = (o as Outcome.Ok).state
    private fun rejected(o: Outcome) = (o as Outcome.Rejected).reason

    private fun card(id: Int, d: Int) = GameCard(id, digit = d)
    private fun card(id: Int, op: Op) = GameCard(id, op = op)

    /** A round in progress: target 12, a's turn. a can make 12 with 3x4, b has 9 and +, c has 1 and 2. */
    private fun inTurns(): GameState = lobby().copy(
        phase = Phase.Turns("a", hasDrawn = false, turnEndsAt = t0 + 30_000),
        target = listOf(1, 2),
        hands = mapOf(
            "a" to RoundPlayer(listOf(card(1, 3), card(2, 4), card(3, Op.Plus))),
            "b" to RoundPlayer(listOf(card(4, 9), card(5, Op.Plus))),
            "c" to RoundPlayer(listOf(card(6, 1), card(7, 2), card(8, Op.Plus))),
        ),
    )

    @Test fun cannotStartAlone() {
        val s = engine.newGame("R", "a", "Asha")
        assertEquals("Need at least 2 players", rejected(engine.apply(s.copy(players = s.players.map { it.copy(ready = true) }), "a", Intent.Start, t0)))
    }

    @Test fun everyoneMustBeReadyThenAdminCanStart() {
        var s = lobby()
        assertEquals("Everyone must be ready", rejected(engine.apply(s, "a", Intent.Start, t0)))
        for (id in listOf("a", "b", "c")) s = ok(engine.apply(s, id, Intent.SetReady(true), t0))
        assertEquals("Only the admin can start", rejected(engine.apply(s, "b", Intent.Start, t0)))
        s = ok(engine.apply(s, "a", Intent.Start, t0))
        assertTrue(s.phase is Phase.DrawingTarget)
        assertEquals(3, s.hands.size)
        s.hands.values.forEach { assertEquals(2, it.hand.size) } // one number, one operator
    }

    @Test fun onlyAdminChangesSettings() {
        val s = lobby()
        assertEquals("Only the admin can change settings", rejected(engine.apply(s, "b", Intent.UpdateSettings(Settings(turnSeconds = 10)), t0)))
        assertEquals(10, ok(engine.apply(s, "a", Intent.UpdateSettings(Settings(turnSeconds = 10)), t0)).settings.turnSeconds)
    }

    @Test fun drawerStopsAfterOneCardOrIsForcedAtThree() {
        var s = lobby().copy(phase = Phase.DrawingTarget("b", emptyList()))
        assertEquals("Someone else is drawing the target", rejected(engine.apply(s, "a", Intent.DrawTargetCard, t0)))
        assertEquals("Draw at least one target card", rejected(engine.apply(s, "b", Intent.StopTarget, t0)))
        s = ok(engine.apply(s, "b", Intent.DrawTargetCard, t0))
        val stopped = ok(engine.apply(s, "b", Intent.StopTarget, t0))
        assertEquals(1, stopped.target.size)
        assertTrue(stopped.phase is Phase.Turns)

        var three = lobby().copy(phase = Phase.DrawingTarget("b", emptyList()))
        repeat(3) { three = ok(engine.apply(three, "b", Intent.DrawTargetCard, t0)) }
        assertEquals(3, three.target.size)
        assertTrue(three.phase is Phase.Turns) // 3 cards: starts by itself
    }

    @Test fun onlyTheCurrentPlayerMayAct() {
        assertEquals("It is not your turn", rejected(engine.apply(inTurns(), "b", Intent.Submit("1+1"), t0)))
    }

    @Test fun hittingTheTargetWinsTheRound() {
        val s = ok(engine.apply(inTurns(), "a", Intent.Submit("3x4"), t0))
        assertEquals(Phase.RoundEnd("a", "Asha hit the target!"), s.phase)
        assertEquals(5, s.player("a")!!.score)
    }

    @Test fun wrongSubmissionLocksYouForTwoSeconds() {
        var s = ok(engine.apply(inTurns(), "a", Intent.Submit("3+4"), t0))
        assertTrue(s.phase is Phase.Turns)
        assertEquals("Wait for the penalty to end", rejected(engine.apply(s, "a", Intent.Submit("3x4"), t0 + 1_000)))
        s = ok(engine.apply(s, "a", Intent.Submit("3x4"), t0 + 2_001))
        assertTrue(s.phase is Phase.RoundEnd)
    }

    @Test fun passNeedsADrawFirstThenMovesToNextPlayerAndOpensCatchWindow() {
        val s0 = inTurns()
        assertEquals("Draw a card before passing", rejected(engine.apply(s0, "a", Intent.Pass, t0)))
        var s = ok(engine.apply(s0, "a", Intent.DrawCard(number = true), t0))
        assertEquals("You already drew this turn", rejected(engine.apply(s, "a", Intent.DrawCard(true), t0)))
        s = ok(engine.apply(s, "a", Intent.Pass, t0 + 1000))
        assertEquals("b", (s.phase as Phase.Turns).currentId)
        assertEquals(listOf(CatchWindow("a", t0 + 1000 + 5000)), s.catchWindows)
    }

    @Test fun validCatchGivesThreePointsAndPutsPasserOut() {
        var s = ok(engine.apply(ok(engine.apply(inTurns(), "a", Intent.DrawCard(true), t0)), "a", Intent.Pass, t0))
        s = ok(engine.apply(s, "b", Intent.Catch("a", "3x4"), t0 + 2000))
        assertEquals(3, s.player("b")!!.score)
        assertTrue(s.hands.getValue("a").out)
    }

    @Test fun cannotCatchWhileThePlayersTurnIsStillRunning() {
        assertEquals("Too late, or that player's turn is not over", rejected(engine.apply(inTurns(), "b", Intent.Catch("a", "3x4"), t0)))
    }

    @Test fun cannotCatchAfterTheWindowCloses() {
        val s = ok(engine.apply(ok(engine.apply(inTurns(), "a", Intent.DrawCard(true), t0)), "a", Intent.Pass, t0))
        assertEquals("Too late, or that player's turn is not over", rejected(engine.apply(s, "b", Intent.Catch("a", "3x4"), t0 + 6000)))
    }

    @Test fun falseCatchPutsTheCatcherOut() {
        val s0 = ok(engine.apply(ok(engine.apply(inTurns(), "a", Intent.DrawCard(true), t0)), "a", Intent.Pass, t0))
        val s = ok(engine.apply(s0, "b", Intent.Catch("a", "3+4"), t0 + 1000))
        assertTrue(s.hands.getValue("b").out)
        assertFalse(s.hands.getValue("a").out)
        assertEquals(0, s.player("b")!!.score)
        assertEquals("c", (s.phase as Phase.Turns).currentId) // b was up next, so the turn moves on
    }

    @Test fun timeoutCountsAsAPass() {
        val s = engine.tick(inTurns(), t0 + 30_001)
        assertEquals("b", (s.phase as Phase.Turns).currentId)
        assertEquals("a", s.catchWindows.single().playerId)
        assertEquals(s, engine.tick(s, t0 + 30_002)) // nothing else to do yet
    }

    @Test fun lastPlayerStandingWinsTheRound() {
        var s = inTurns().copy(hands = inTurns().hands + ("c" to RoundPlayer(inTurns().hands.getValue("c").hand, out = true)))
        s = ok(engine.apply(ok(engine.apply(s, "a", Intent.DrawCard(true), t0)), "a", Intent.Pass, t0))
        s = ok(engine.apply(s, "b", Intent.Catch("a", "3x4"), t0 + 500))
        assertEquals("b", (s.phase as Phase.RoundEnd).winnerId)
        assertEquals(3 + 5, s.player("b")!!.score)
    }

    @Test fun seriesEndsWhenSomeoneReachesTheWinScore() {
        val s0 = inTurns().let { it.copy(players = it.players.map { p -> if (p.id == "a") p.copy(score = 8) else p }) }
        val s = ok(engine.apply(s0, "a", Intent.Submit("3x4"), t0))
        assertEquals(Phase.Finished("a"), s.phase)
    }

    @Test fun backToLobbyResetsScoresAndReadiness() {
        val finished = inTurns().copy(phase = Phase.Finished("a"))
        val s = ok(engine.apply(finished, "a", Intent.BackToLobby, t0))
        assertEquals(Phase.Lobby, s.phase)
        assertTrue(s.players.all { it.score == 0 && !it.ready })
    }

    @Test fun othersCanWatchTheCurrentPlayersRowAndItClearsWhenTheTurnEnds() {
        val row = listOf(RowSlot("3"), RowSlot("×"), RowSlot("4", raised = "2"))
        assertEquals("It is not your turn", rejected(engine.apply(inTurns(), "b", Intent.Preview(row), t0)))
        var s = ok(engine.apply(inTurns(), "a", Intent.Preview(row), t0))
        assertEquals(row, s.liveRow)
        s = ok(engine.apply(s, "a", Intent.DrawCard(true), t0))
        s = ok(engine.apply(s, "a", Intent.Pass, t0))
        assertTrue(s.liveRow.isEmpty())
    }

    @Test fun stateSurvivesAJsonRoundTrip() {
        val json = kotlinx.serialization.json.Json { classDiscriminator = "type" }
        val s = inTurns()
        assertEquals(s, json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), s)))
        val i: Intent = Intent.Catch("a", "3x4")
        assertEquals(i, json.decodeFromString(Intent.serializer(), json.encodeToString(Intent.serializer(), i)))
    }
}
