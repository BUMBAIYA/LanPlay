package com.bumbaiya.lanplay.net

import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val PROTOCOL_VERSION = 1
const val DEFAULT_PORT = 8765

val NetJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

/** Messages a player's phone sends to the host. */
@Serializable
sealed interface ClientMessage {
    /** First message on every connection. [seatToken] lets a dropped player reclaim their seat. */
    @Serializable @SerialName("hello") data class Hello(val name: String, val seatToken: String? = null, val version: Int = PROTOCOL_VERSION) : ClientMessage
    @Serializable @SerialName("do") data class Do(val intent: Intent) : ClientMessage
}

/** Messages the host sends back. The state is sent in full every time: it is small. */
@Serializable
sealed interface ServerMessage {
    @Serializable @SerialName("welcome") data class Welcome(val playerId: String, val seatToken: String, val state: GameState) : ServerMessage
    @Serializable @SerialName("state") data class State(val state: GameState) : ServerMessage
    @Serializable @SerialName("error") data class Error(val reason: String) : ServerMessage
}
