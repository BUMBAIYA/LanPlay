package com.bumbaiya.lanplay.game.combinemaths

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One card. Exactly one of [digit] / [op] is set. Ids are unique within a game. */
@Serializable
data class GameCard(val id: Int, val digit: Int? = null, val op: Op? = null)

/** Lobby settings the admin can change. Every number here is flexible on purpose. */
@Serializable
data class Settings(
    val turnSeconds: Int = 30,
    val catchWindowSeconds: Int = 5,
    val penaltySeconds: Int = 2,
    val winScore: Int = 11,
    val roundWinPoints: Int = 5,
    val catchPoints: Int = 3,
    /** Off by default: the game is about doing the maths in your head. */
    val showLiveResult: Boolean = false,
)

@Serializable
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val ready: Boolean = false,
    val connected: Boolean = true,
)

/** A player's cards for the current round. */
@Serializable
data class RoundPlayer(
    val hand: List<GameCard> = emptyList(),
    val out: Boolean = false,
    /** Wrong submissions block you until this time (epoch ms). */
    val lockedUntil: Long = 0,
)

@Serializable
sealed interface Phase {
    @Serializable @SerialName("lobby") data object Lobby : Phase

    /** One randomly picked player draws 1 to 3 target cards and decides when to stop. */
    @Serializable @SerialName("target") data class DrawingTarget(val drawerId: String, val digits: List<Int>) : Phase

    @Serializable @SerialName("turns") data class Turns(val currentId: String, val hasDrawn: Boolean, val turnEndsAt: Long) : Phase

    @Serializable @SerialName("roundEnd") data class RoundEnd(val winnerId: String?, val reason: String) : Phase

    @Serializable @SerialName("finished") data class Finished(val winnerId: String) : Phase
}

/** One slot of the row a player is building, as others see it live: the face showing, plus a raised card if any. */
@Serializable
data class RowSlot(val glyph: String, val raised: String? = null)

/** What a player last handed in: the row they built, what it came to, and whether they passed instead. */
@Serializable
data class Submitted(val row: List<RowSlot>, val value: String? = null, val passed: Boolean = false)

/** After a player passes (or times out), others may catch them until [closesAt]. */
@Serializable
data class CatchWindow(val playerId: String, val closesAt: Long)

/** Everything about a game. All cards are face-up, so every player gets the whole thing. */
@Serializable
data class GameState(
    val roomId: String,
    val adminId: String,
    val settings: Settings = Settings(),
    val players: List<Player> = emptyList(),
    val phase: Phase = Phase.Lobby,
    val target: List<Int> = emptyList(),
    val hands: Map<String, RoundPlayer> = emptyMap(),
    val catchWindows: List<CatchWindow> = emptyList(),
    /** What the current player is building right now, so everyone can watch. Cleared when the turn ends. */
    val liveRow: List<RowSlot> = emptyList(),
    /** Each player's last answer or pass this round, so anyone can look at it when peeking at their cards. */
    val lastSubmitted: Map<String, Submitted> = emptyMap(),
    val round: Int = 0,
    val nextCardId: Int = 1,
    /** Last event in words, shown to everyone ("Asha was caught!"). */
    val notice: String = "",
    /** Goes up by one on every change so clients can ignore stale updates. */
    val seq: Long = 0,
    /** The host's clock when this was sent. Phones' clocks differ, so timers use this to line up. */
    val stamp: Long = 0,
) {
    val targetValue: Long? get() = target.takeIf { it.isNotEmpty() }?.joinToString("")?.toLongOrNull()
    fun player(id: String) = players.firstOrNull { it.id == id }
}

/** Everything a player can ask the host to do. The host checks each one. */
@Serializable
sealed interface Intent {
    @Serializable @SerialName("ready") data class SetReady(val ready: Boolean) : Intent
    @Serializable @SerialName("settings") data class UpdateSettings(val settings: Settings) : Intent
    @Serializable @SerialName("start") data object Start : Intent
    @Serializable @SerialName("drawTarget") data object DrawTargetCard : Intent
    @Serializable @SerialName("stopTarget") data object StopTarget : Intent
    @Serializable @SerialName("draw") data class DrawCard(val number: Boolean) : Intent
    @Serializable @SerialName("submit") data class Submit(val expression: String) : Intent
    /** The current player's row as it changes, so others can watch. Purely for show: the host never judges it. */
    @Serializable @SerialName("preview") data class Preview(val row: List<RowSlot>) : Intent
    @Serializable @SerialName("pass") data object Pass : Intent
    @Serializable @SerialName("catch") data class Catch(val targetId: String, val expression: String) : Intent
    @Serializable @SerialName("nextRound") data object NextRound : Intent
    @Serializable @SerialName("lobby") data object BackToLobby : Intent
    /** A player chose to leave. They cannot come back to this game. */
    @Serializable @SerialName("leave") data object Leave : Intent
    /** Admin only: remove a player from the room. */
    @Serializable @SerialName("kick") data class Kick(val playerId: String) : Intent
}

sealed interface Outcome {
    data class Ok(val state: GameState) : Outcome
    data class Rejected(val reason: String) : Outcome
}
