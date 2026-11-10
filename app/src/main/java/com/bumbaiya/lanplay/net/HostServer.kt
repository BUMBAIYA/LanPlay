package com.bumbaiya.lanplay.net

import com.bumbaiya.lanplay.game.combinemaths.GameEngine
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Outcome
import com.bumbaiya.lanplay.game.combinemaths.Phase
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The host phone's server. It is the only place the rules run: players send intents, the host
 * checks them with [GameEngine] and sends everyone the new state.
 *
 * The first person to connect becomes the admin (the host app connects to its own server first).
 */
class HostServer(
    val roomId: String,
    private val engine: GameEngine = GameEngine(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val seatTokens = ConcurrentHashMap<String, String>() // token -> playerId
    private val removedIds = ConcurrentHashMap.newKeySet<String>() // kicked players cannot reclaim their seat
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    private val _state = MutableStateFlow<GameState?>(null)
    /** The host's own view of the game (the host app uses its client for the UI, this is for tests/debug). */
    val state: StateFlow<GameState?> = _state

    /** Starts listening and returns the port. Tries [preferredPort] first, then any free port. */
    fun start(preferredPort: Int = DEFAULT_PORT): Int {
        val port = if (isFree(preferredPort)) preferredPort else ServerSocket(0).use { it.localPort }
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets) { pingPeriodMillis = 15_000; timeoutMillis = 30_000 }
            routing { webSocket("/ws") { handleConnection(this) } }
        }.also { it.start(wait = false) }
        scope.launch {
            while (isActive) {
                delay(250)
                lock.withLock {
                    val s = _state.value ?: return@withLock
                    val n = engine.tick(s, clock())
                    if (n != s) publish(n)
                }
            }
        }
        return port
    }

    fun stop() {
        scope.cancel()
        server?.stop(200, 500)
        server = null
    }

    private fun isFree(port: Int) = runCatching { ServerSocket(port).use { true } }.getOrDefault(false)

    private suspend fun handleConnection(session: DefaultWebSocketServerSession) {
        var myId: String? = null
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val msg = runCatching { NetJson.decodeFromString(ClientMessage.serializer(), frame.readText()) }.getOrNull()
                    ?: continue
                when (msg) {
                    is ClientMessage.Hello -> myId = hello(msg, session) ?: return
                    is ClientMessage.Do -> myId?.let { handleIntent(it, msg, session) }
                }
            }
        } finally {
            myId?.let { disconnected(it, session) }
        }
    }

    /** Returns the player's id, or null if they were turned away (the connection is then closed). */
    private suspend fun hello(msg: ClientMessage.Hello, session: DefaultWebSocketServerSession): String? = lock.withLock {
        if (msg.version != PROTOCOL_VERSION) {
            reject(session, "Different app version. Update LanPlay on every phone.")
            return null
        }
        val knownId = msg.seatToken?.let { seatTokens[it] }
        if (knownId != null && knownId in removedIds) { reject(session, "The host removed you from this room"); return null }
        var current = _state.value
        val id: String
        val token: String
        if (knownId != null && current?.player(knownId) != null) {
            id = knownId; token = msg.seatToken!!
            current = engine.setConnected(current!!, id, true)
        } else {
            id = UUID.randomUUID().toString(); token = UUID.randomUUID().toString()
            if (current == null) {
                current = engine.newGame(roomId, id, msg.name)
            } else when (val o = engine.addPlayer(current, id, msg.name)) {
                is Outcome.Ok -> current = o.state
                is Outcome.Rejected -> { reject(session, o.reason); return null }
            }
            seatTokens[token] = id
        }
        sessions[id] = session
        current = current!!.copy(stamp = clock())
        _state.value = current
        send(session, ServerMessage.Welcome(id, token, current))
        broadcast(current, except = id)
        id
    }

    private suspend fun handleIntent(id: String, msg: ClientMessage.Do, session: DefaultWebSocketServerSession) = lock.withLock {
        val s = _state.value ?: return@withLock
        when (val o = engine.apply(s, id, msg.intent, clock())) {
            is Outcome.Ok -> {
                val kicked = (msg.intent as? Intent.Kick)?.playerId
                val kickedSession = kicked?.let { removedIds.add(it); sessions.remove(it) }
                publish(o.state)
                kickedSession?.let { send(it, ServerMessage.Kicked("The host removed you from this room")); runCatching { it.close() } }
            }
            is Outcome.Rejected -> send(session, ServerMessage.Error(o.reason))
        }
    }

    private suspend fun disconnected(id: String, session: DefaultWebSocketServerSession) = lock.withLock {
        if (sessions[id] !== session) return@withLock // they already reconnected on a new connection
        sessions.remove(id)
        val s = _state.value ?: return@withLock
        // In the lobby a dropped player is simply removed; in a game they keep their seat so they can return.
        val next = if (s.phase == Phase.Lobby) engine.removePlayer(s, id, clock()) else engine.setConnected(s, id, false)
        if (next != s) publish(next)
    }

    private suspend fun publish(s: GameState) { val stamped = s.copy(stamp = clock()); _state.value = stamped; broadcast(stamped, except = null) }

    private suspend fun broadcast(s: GameState, except: String?) {
        for ((id, session) in sessions) if (id != except) send(session, ServerMessage.State(s))
    }

    private suspend fun send(session: DefaultWebSocketServerSession, msg: ServerMessage) {
        runCatching { session.send(Frame.Text(NetJson.encodeToString(ServerMessage.serializer(), msg))) }
    }

    private suspend fun reject(session: DefaultWebSocketServerSession, reason: String) {
        send(session, ServerMessage.Error(reason))
        runCatching { session.close() }
    }
}
