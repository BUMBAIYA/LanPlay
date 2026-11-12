package com.bumbaiya.lanplay.platform

import android.app.Application
import android.content.Context
import com.bumbaiya.lanplay.net.GameClient
import com.bumbaiya.lanplay.net.HostServer
import com.bumbaiya.lanplay.net.JoinLink
import com.bumbaiya.lanplay.net.RoomIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one place that knows how to host, join and leave. The screens only watch [client] and call
 * these functions. It lives for as long as the app process does.
 */
object Session {
    private lateinit var app: Application
    lateinit var settings: AppSettings
        private set
    lateinit var discovery: RoomDiscovery
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var server: HostServer? = null

    private val _client = MutableStateFlow<GameClient?>(null)
    /** The connection to the current game, or null when not in one. */
    val client: StateFlow<GameClient?> = _client
    private val _hostLink = MutableStateFlow<JoinLink?>(null)
    /** Only set on the host: what the QR code shows. */
    val hostLink: StateFlow<JoinLink?> = _hostLink
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext as Application
        settings = AppSettings(app)
        discovery = RoomDiscovery(app)
    }

    fun clearError() { _error.value = null }

    /** Debug-only: every number is 0 and every operator +, so a test can score with "0+0" against target 0. */
    var debugFixedCards = false

    private object FixedRandom : kotlin.random.Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextInt(from: Int, until: Int) = from
    }

    fun host(name: String) {
        if (_busy.value) return
        scope.launch {
            _busy.value = true; _error.value = null
            leave()
            WifiBinding.bind(app)
            try {
                val room = RoomIds.generate()
                val s = HostServer(room, if (debugFixedCards) com.bumbaiya.lanplay.game.combinemaths.GameEngine(FixedRandom) else com.bumbaiya.lanplay.game.combinemaths.GameEngine())
                val port = withContext(Dispatchers.IO) { s.start() }
                server = s
                val ip = withContext(Dispatchers.IO) { LocalAddress.find() }
                _hostLink.value = JoinLink(room, ip ?: "127.0.0.1", port)
                discovery.advertise(room, port, ip)
                HostService.start(app, room)
                connect("127.0.0.1", port, name)
            } catch (e: Exception) {
                _error.value = "Could not start the room: ${e.message}"
                leave()
            } finally { _busy.value = false }
        }
    }

    private val _joining = MutableStateFlow<Pair<JoinLink, String>?>(null)
    /** The room we are joining (and our name), so the connecting screen can show it and retry. */
    val joining: StateFlow<Pair<JoinLink, String>?> = _joining

    fun join(link: JoinLink, name: String) {
        leave()
        _error.value = null
        _joining.value = link to name
        WifiBinding.bind(app)
        connect(link.host, link.port, name)
    }

    fun retryJoin() { _joining.value?.let { (l, n) -> join(l, n) } }

    /** Join by typing a room ID: searches the local network for it. */
    fun joinByRoom(room: String, name: String) {
        if (_busy.value) return
        val id = RoomIds.normalize(room)
        if (!RoomIds.isValid(id)) { _error.value = "Room IDs are ${RoomIds.LENGTH} letters or digits"; return }
        scope.launch {
            _busy.value = true; _error.value = null
            discovery.startSearching()
            val link = discovery.find(id)
            discovery.stopSearching()
            _busy.value = false
            if (link == null) _error.value = "Room $id not found. Are you on the host's hotspot? You can also scan the QR code."
            else join(link, name)
        }
    }

    fun leave() {
        _client.value?.close(); _client.value = null
        server?.stop(); server = null
        _hostLink.value = null
        _joining.value = null
        discovery.stopAdvertising()
        HostService.stop(app)
        WifiBinding.unbind(app)
    }

    private fun connect(host: String, port: Int, name: String) {
        val c = GameClient(scope)
        _client.value = c
        c.connect(host, port, name.ifBlank { "Player" })
    }
}
