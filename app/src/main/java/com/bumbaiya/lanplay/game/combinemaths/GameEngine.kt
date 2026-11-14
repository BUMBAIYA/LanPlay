package com.bumbaiya.lanplay.game.combinemaths

import kotlin.random.Random

/**
 * The referee. It holds no state of its own: every call takes a [GameState] and returns a new
 * one (or a rejection). That makes it trivial to unit-test and safe to run on the host only.
 *
 * Times are passed in as `now` (epoch milliseconds) instead of reading the clock, so tests can
 * move time forward by hand.
 */
class GameEngine(private val random: Random = Random.Default) {

    // ---------- people coming and going ----------

    fun newGame(roomId: String, adminId: String, adminName: String) = GameState(
        roomId = roomId,
        adminId = adminId,
        players = listOf(Player(adminId, adminName)),
    )

    fun addPlayer(s: GameState, id: String, name: String): Outcome {
        if (s.phase != Phase.Lobby) return Outcome.Rejected("The game has already started")
        if (s.players.any { it.id == id }) return Outcome.Ok(s)
        if (s.players.size >= MAX_PLAYERS) return Outcome.Rejected("The room is full")
        return ok(s.copy(players = s.players + Player(id, name.ifBlank { "Player" }), notice = "$name joined"))
    }

    fun removePlayer(s: GameState, id: String, now: Long): GameState {
        val who = s.player(id)?.name ?: return s
        if (s.phase == Phase.Lobby) {
            val left = s.players.filterNot { it.id == id }
            return bump(s.copy(players = left, adminId = if (s.adminId == id) left.firstOrNull()?.id ?: id else s.adminId, notice = "$who left"))
        }
        // Mid-game: they stay on the scoreboard but are out of the round.
        val marked = s.copy(players = s.players.map { if (it.id == id) it.copy(connected = false) else it })
        return eliminate(marked, id, now, "$who left the game")
    }

    /** Marks a player as connected or not (dropped connection or reconnect). */
    fun setConnected(s: GameState, id: String, connected: Boolean): GameState {
        val p = s.player(id) ?: return s
        if (p.connected == connected) return s
        return bump(s.copy(players = s.players.map { if (it.id == id) it.copy(connected = connected) else it },
            notice = if (connected) "${p.name} is back" else "${p.name} lost connection"))
    }

    // ---------- intents ----------

    fun apply(s: GameState, playerId: String, intent: Intent, now: Long): Outcome {
        val me = s.player(playerId) ?: return Outcome.Rejected("You are not in this room")
        val live = s.copy(catchWindows = s.catchWindows.filter { it.closesAt >= now })
        return when (intent) {
            is Intent.Kick -> when {
                playerId != live.adminId -> Outcome.Rejected("Only the admin can remove players")
                intent.playerId == playerId -> Outcome.Rejected("You cannot remove yourself")
                live.player(intent.playerId) == null -> Outcome.Rejected("That player is not in the room")
                else -> ok(removePlayer(live, intent.playerId, now).let { if (live.phase == Phase.Lobby) it else it.copy(players = it.players.filterNot { p -> p.id == intent.playerId }) })
            }
            is Intent.SetReady -> inLobby(live) { ok(it.copy(players = it.players.map { p -> if (p.id == playerId) p.copy(ready = intent.ready) else p })) }
            is Intent.UpdateSettings -> inLobby(live) {
                if (playerId != it.adminId) Outcome.Rejected("Only the admin can change settings")
                else ok(it.copy(settings = intent.settings.sanitized()))
            }
            Intent.Start -> inLobby(live) {
                when {
                    playerId != it.adminId -> Outcome.Rejected("Only the admin can start")
                    it.players.size < MIN_PLAYERS -> Outcome.Rejected("Need at least $MIN_PLAYERS players")
                    !it.players.all { p -> p.ready } -> Outcome.Rejected("Everyone must be ready")
                    else -> ok(startRound(it))
                }
            }
            Intent.DrawTargetCard -> drawTarget(live, playerId, now, stop = false)
            Intent.StopTarget -> drawTarget(live, playerId, now, stop = true)
            is Intent.DrawCard -> turnOnly(live, playerId) { turns ->
                if (turns.hasDrawn) return@turnOnly Outcome.Rejected("You already drew this turn")
                val (card, withId) = drawFor(live, playerId, intent.number)
                val hand = withId.hands.getValue(playerId)
                ok(withId.copy(
                    hands = withId.hands + (playerId to hand.copy(hand = hand.hand + card)),
                    phase = turns.copy(hasDrawn = true),
                    notice = "${me.name} drew a card",
                ))
            }
            is Intent.Submit -> turnOnly(live, playerId) { submit(live, me, intent.expression, now) }
            is Intent.Preview -> turnOnly(live, playerId) { ok(live.copy(liveRow = intent.row.take(MAX_PREVIEW_SLOTS))) }
            Intent.Pass -> turnOnly(live, playerId) { turns ->
                if (!turns.hasDrawn) Outcome.Rejected("Draw a card before passing")
                else ok(endTurn(live, now, passed = true, notice = "${me.name} passed"))
            }
            is Intent.Catch -> catch(live, me, intent, now)
            Intent.NextRound -> when {
                playerId != live.adminId -> Outcome.Rejected("Only the admin can continue")
                live.phase !is Phase.RoundEnd -> Outcome.Rejected("The round is not over")
                else -> ok(startRound(live))
            }
            Intent.BackToLobby -> when {
                playerId != live.adminId -> Outcome.Rejected("Only the admin can do that")
                live.phase !is Phase.Finished && live.phase !is Phase.RoundEnd -> Outcome.Rejected("The game is still running")
                else -> ok(live.copy(
                    phase = Phase.Lobby, target = emptyList(), hands = emptyMap(), catchWindows = emptyList(), liveRow = emptyList(), round = 0,
                    players = live.players.filter { it.connected }.map { it.copy(score = 0, ready = false) },
                    notice = "Back in the lobby",
                ))
            }
        }
    }

    /** Called by the host a few times a second: moves on when a turn runs out of time. */
    fun tick(s: GameState, now: Long): GameState {
        val trimmed = s.catchWindows.filter { it.closesAt >= now }
        val base = if (trimmed.size != s.catchWindows.size) bump(s.copy(catchWindows = trimmed)) else s
        val p = base.phase
        return if (p is Phase.Turns && now >= p.turnEndsAt) {
            val who = base.player(p.currentId)?.name ?: "Player"
            endTurn(base, now, passed = true, notice = "$who ran out of time")
        } else base
    }

    // ---------- the steps of a round ----------

    private fun startRound(s: GameState): GameState {
        var state = s
        val hands = mutableMapOf<String, RoundPlayer>()
        for (p in s.players.filter { it.connected }) {
            val (n, a) = newCard(state, number = true); state = a
            val (o, b) = newCard(state, number = false); state = b
            hands[p.id] = RoundPlayer(hand = listOf(n, o))
        }
        val drawer = s.players.filter { it.connected }.random(random)
        return bump(state.copy(
            round = s.round + 1, hands = hands, catchWindows = emptyList(), liveRow = emptyList(), lastSubmitted = emptyMap(), target = emptyList(),
            phase = Phase.DrawingTarget(drawer.id, emptyList()),
            notice = "${drawer.name} draws the target",
        ))
    }

    private fun drawTarget(s: GameState, playerId: String, now: Long, stop: Boolean): Outcome {
        val phase = s.phase as? Phase.DrawingTarget ?: return Outcome.Rejected("Not drawing the target now")
        if (phase.drawerId != playerId) return Outcome.Rejected("Someone else is drawing the target")
        val digits = if (stop) phase.digits else {
            if (phase.digits.size >= MAX_TARGET_CARDS) return Outcome.Rejected("The target has $MAX_TARGET_CARDS cards already")
            phase.digits + random.nextInt(0, 10)
        }
        if (stop && digits.isEmpty()) return Outcome.Rejected("Draw at least one target card")
        if (!stop && digits.size < MAX_TARGET_CARDS) return ok(s.copy(phase = phase.copy(digits = digits)))
        return ok(beginTurns(s, digits, phase.drawerId, now))
    }

    private fun beginTurns(s: GameState, digits: List<Int>, first: String, now: Long) = s.copy(
        target = digits,
        phase = Phase.Turns(first, hasDrawn = false, turnEndsAt = now + s.settings.turnSeconds * 1000L),
        notice = "Target is ${digits.joinToString("")}",
    )

    private fun submit(s: GameState, me: Player, expression: String, now: Long): Outcome {
        val rp = s.hands[me.id] ?: return Outcome.Rejected("You have no cards")
        if (now < rp.lockedUntil) return Outcome.Rejected("Wait for the penalty to end")
        val target = s.targetValue ?: return Outcome.Rejected("No target")
        return if (hits(expression, rp, target)) {
            ok(finishRound(s, me.id, "${me.name} hit the target!"))
        } else {
            val locked = rp.copy(lockedUntil = now + s.settings.penaltySeconds * 1000L)
            val hand = Hand(rp.hand.mapNotNull { it.digit }, rp.hand.mapNotNull { it.op })
            val value = (ExpressionEvaluator.evaluate(expression, hand, Rational.of(target)) as? EvalResult.Value)?.value?.toString()
            ok(s.copy(hands = s.hands + (me.id to locked), lastSubmitted = s.lastSubmitted + (me.id to Submitted(s.liveRow, value)), notice = "${me.name} got it wrong (+${s.settings.penaltySeconds}s penalty)"))
        }
    }

    private fun catch(s: GameState, me: Player, c: Intent.Catch, now: Long): Outcome {
        if (s.phase !is Phase.Turns) return Outcome.Rejected("Nothing to catch right now")
        val myHand = s.hands[me.id] ?: return Outcome.Rejected("You have no cards")
        if (myHand.out) return Outcome.Rejected("You are out of this round")
        if (c.targetId == me.id) return Outcome.Rejected("You cannot catch yourself")
        val window = s.catchWindows.firstOrNull { it.playerId == c.targetId && it.closesAt >= now }
            ?: return Outcome.Rejected("Too late, or that player's turn is not over")
        val victim = s.hands[c.targetId] ?: return Outcome.Rejected("Unknown player")
        val victimName = s.player(c.targetId)?.name ?: "Player"
        val target = s.targetValue ?: return Outcome.Rejected("No target")
        val without = s.copy(catchWindows = s.catchWindows - window)
        return if (hits(c.expression, victim, target)) {
            val paid = without.copy(players = without.players.map { if (it.id == me.id) it.copy(score = it.score + s.settings.catchPoints) else it })
            ok(eliminate(paid, c.targetId, now, "${me.name} caught $victimName! DHAPPA!"))
        } else {
            ok(eliminate(without, me.id, now, "${me.name} called a false catch and is out"))
        }
    }

    /** Takes a player out of the round and makes sure the game keeps moving. */
    private fun eliminate(s: GameState, id: String, now: Long, notice: String): GameState {
        val hands = s.hands + (id to (s.hands[id] ?: RoundPlayer()).copy(out = true))
        val s2 = bump(s.copy(hands = hands, notice = notice))
        val active = activeIds(s2)
        if (s2.phase !is Phase.Turns && s2.phase !is Phase.DrawingTarget) return s2
        if (active.size <= 1) return finishRound(s2, active.firstOrNull(), "$notice. ${active.firstOrNull()?.let { s2.player(it)?.name } ?: "Nobody"} is the last one standing")
        val p = s2.phase
        return if (p is Phase.Turns && p.currentId == id) endTurn(s2, now, passed = false, notice = notice) else s2
    }

    private fun endTurn(s: GameState, now: Long, passed: Boolean, notice: String): GameState {
        val p = s.phase as? Phase.Turns ?: return s
        val windows = if (passed) s.catchWindows + CatchWindow(p.currentId, now + s.settings.catchWindowSeconds * 1000L) else s.catchWindows
        val order = s.players.map { it.id }
        val active = activeIds(s)
        if (active.isEmpty()) return finishRound(s, null, "Everybody is out")
        var i = order.indexOf(p.currentId)
        var next: String
        do { i = (i + 1) % order.size; next = order[i] } while (next !in active)
        val last = if (passed && s.liveRow.isNotEmpty()) s.lastSubmitted + (p.currentId to Submitted(s.liveRow, passed = true)) else s.lastSubmitted
        return bump(s.copy(
            catchWindows = windows, liveRow = emptyList(), lastSubmitted = last,
            phase = Phase.Turns(next, hasDrawn = false, turnEndsAt = now + s.settings.turnSeconds * 1000L),
            notice = notice,
        ))
    }

    private fun finishRound(s: GameState, winnerId: String?, reason: String): GameState {
        val scored = if (winnerId == null) s.players else s.players.map { if (it.id == winnerId) it.copy(score = it.score + s.settings.roundWinPoints) else it }
        val leader = scored.maxByOrNull { it.score }
        val phase = if (leader != null && leader.score >= s.settings.winScore) Phase.Finished(leader.id) else Phase.RoundEnd(winnerId, reason)
        return bump(s.copy(players = scored, phase = phase, catchWindows = emptyList(), liveRow = emptyList(), notice = reason))
    }

    // ---------- helpers ----------

    private fun hits(expression: String, rp: RoundPlayer, target: Long): Boolean {
        val hand = Hand(rp.hand.mapNotNull { it.digit }, rp.hand.mapNotNull { it.op })
        val r = ExpressionEvaluator.evaluate(expression, hand, Rational.of(target))
        return r is EvalResult.Value && r.value == Rational.of(target)
    }

    private fun activeIds(s: GameState) = s.hands.filterValues { !it.out }.keys.filter { id -> s.player(id)?.connected == true }

    /**
     * A draw during a round. Pure luck can leave a round with no way to reach the target, so the
     * draw leans toward cards that help: the longer a player's hand has been stuck, the more
     * likely the card they get is one that gets them closer to (or all the way to) the target.
     */
    private fun drawFor(s: GameState, playerId: String, number: Boolean): Pair<GameCard, GameState> {
        val target = s.targetValue
        val rp = s.hands[playerId]
        if (target == null || rp == null) return newCard(s, number)
        val help = (ASSIST_BASE + ASSIST_PER_CARD * (rp.hand.size - 2)).coerceIn(ASSIST_BASE, ASSIST_MAX)
        if (random.nextDouble() >= help) return newCard(s, number)
        val id = s.nextCardId
        val options: List<GameCard> = if (number) (0..9).map { GameCard(id, digit = it) } else OPERATOR_POOL.map { GameCard(id, op = it) }
        val digits = rp.hand.mapNotNull { it.digit }
        val ops = rp.hand.mapNotNull { it.op }
        val scored = options.map { c ->
            val h = Hand(digits + listOfNotNull(c.digit), ops + listOfNotNull(c.op))
            c to Solver.distance(h, target, timeLimitMs = 12)
        }
        val bestDistance = scored.minOf { it.second }
        val best = scored.filter { it.second == bestDistance }.map { it.first }
        return best[random.nextInt(0, best.size)] to s.copy(nextCardId = id + 1)
    }

    private fun newCard(s: GameState, number: Boolean): Pair<GameCard, GameState> {
        val id = s.nextCardId
        val card = if (number) GameCard(id, digit = random.nextInt(0, 10)) else GameCard(id, op = OPERATOR_POOL.random(random))
        return card to s.copy(nextCardId = id + 1)
    }

    private inline fun inLobby(s: GameState, block: (GameState) -> Outcome) =
        if (s.phase == Phase.Lobby) block(s) else Outcome.Rejected("Only possible in the lobby")

    private inline fun turnOnly(s: GameState, playerId: String, block: (Phase.Turns) -> Outcome): Outcome {
        val p = s.phase as? Phase.Turns ?: return Outcome.Rejected("No round in progress")
        if (s.hands[playerId]?.out == true) return Outcome.Rejected("You are out of this round")
        if (p.currentId != playerId) return Outcome.Rejected("It is not your turn")
        return block(p)
    }

    private fun Settings.sanitized() = copy(
        turnSeconds = turnSeconds.coerceIn(5, 300), catchWindowSeconds = catchWindowSeconds.coerceIn(1, 30),
        penaltySeconds = penaltySeconds.coerceIn(0, 30), winScore = winScore.coerceIn(1, 100),
        roundWinPoints = roundWinPoints.coerceIn(0, 50), catchPoints = catchPoints.coerceIn(0, 50),
    )

    private fun ok(s: GameState) = Outcome.Ok(bump(s))
    private fun bump(s: GameState) = s.copy(seq = s.seq + 1)

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        const val MAX_TARGET_CARDS = 3
        const val MAX_PREVIEW_SLOTS = 30
        private const val ASSIST_BASE = 0.4
        private const val ASSIST_PER_CARD = 0.15
        private const val ASSIST_MAX = 0.95
        private val OPERATOR_POOL = listOf(Op.Plus, Op.Minus, Op.Pi, Op.Sigma, Op.Sqrt, Op.Wild)
    }
}
