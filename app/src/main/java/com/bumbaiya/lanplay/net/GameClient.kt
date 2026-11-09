package com.bumbaiya.lanplay.net

import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ConnectionStatus {
    data object Idle : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Connected : ConnectionStatus
    /** The link dropped; we keep retrying with our seat token. */
    data object Reconnecting : ConnectionStatus
    data class Failed(val reason: String) : ConnectionStatus
}

/**
 * A player's connection to the host. Exposes the latest [state] and lets the UI [send] intents.
 * If the connection drops it retries on its own and reclaims the same seat.
 */
class GameClient(private val scope: CoroutineScope) {
    private val http = HttpClient(CIO) { install(WebSockets) { pingIntervalMillis = 15_000 } }
    private val outbox = Channel<ClientMessage>(Channel.UNLIMITED)
    private var job: Job? = null
    @Volatile private var closed = false
    @Volatile private var clockOffset = 0L

    /** The host's current time, as best we can tell. Use this (not the phone clock) for timers. */
    fun serverNow(): Long = System.currentTimeMillis() + clockOffset

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state
    private val _myId = MutableStateFlow<String?>(null)
    val myId: StateFlow<String?> = _myId
    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val status: StateFlow<ConnectionStatus> = _status
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    /** Short messages from the host such as "It is not your turn". Show them as toasts. */
    val messages: SharedFlow<String> = _messages

    fun connect(host: String, port: Int, name: String, maxRetries: Int = 20) {
        closed = false
        _status.value = ConnectionStatus.Connecting
        job = scope.launch {
            var token: String? = null
            var failures = 0
            while (isActive && !closed) {
                try {
                    http.webSocket(host = host, port = port, path = "/ws") {
                        send(Frame.Text(NetJson.encodeToString(ClientMessage.serializer(), ClientMessage.Hello(name, token))))
                        val sender = launch { for (m in outbox) send(Frame.Text(NetJson.encodeToString(ClientMessage.serializer(), m))) }
                        try {
                            for (frame in incoming) {
                                if (frame !is Frame.Text) continue
                                when (val m = NetJson.decodeFromString(ServerMessage.serializer(), frame.readText())) {
                                    is ServerMessage.Welcome -> {
                                        token = m.seatToken; failures = 0
                                        _myId.value = m.playerId; accept(m.state)
                                        _status.value = ConnectionStatus.Connected
                                    }
                                    is ServerMessage.State -> accept(m.state)
                                    is ServerMessage.Error -> {
                                        _messages.tryEmit(m.reason)
                                        if (_myId.value == null) { // turned away at the door
                                            _status.value = ConnectionStatus.Failed(m.reason); closed = true; close()
                                        }
                                    }
                                }
                            }
                        } finally { sender.cancel() }
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) { failures++ }
                if (closed) break
                if (_myId.value == null && failures >= 3) { _status.value = ConnectionStatus.Failed("Could not reach the host"); break }
                if (failures > maxRetries) { _status.value = ConnectionStatus.Failed("Lost connection to the host"); break }
                _status.value = if (_myId.value == null) ConnectionStatus.Connecting else ConnectionStatus.Reconnecting
                delay(1500)
            }
        }
    }

    /** Ignores out-of-date snapshots that arrive after newer ones. */
    private fun accept(s: GameState) {
        if (s.stamp > 0) clockOffset = s.stamp - System.currentTimeMillis()
        val cur = _state.value
        if (cur == null || s.seq >= cur.seq) _state.value = s
    }

    fun send(intent: Intent) { outbox.trySend(ClientMessage.Do(intent)) }

    fun close() {
        closed = true
        job?.cancel()
        http.close()
        _status.value = ConnectionStatus.Idle
    }
}
