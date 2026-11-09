package com.bumbaiya.lanplay.platform

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
    private lateinit var app: Context
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
        app = context.applicationContext
        settings = AppSettings(app)
        discovery = RoomDiscovery(app)
    }

    fun clearError() { _error.value = null }

    fun host(name: String) {
        if (_busy.value) return
        scope.launch {
            _busy.value = true; _error.value = null
            leave()
            try {
                val room = RoomIds.generate()
                val s = HostServer(room)
                val port = withContext(Dispatchers.IO) { s.start() }
                server = s
                val ip = withContext(Dispatchers.IO) { LocalAddress.find() }
                _hostLink.value = JoinLink(room, ip ?: "127.0.0.1", port)
                discovery.advertise(room, port)
                HostService.start(app, room)
                connect("127.0.0.1", port, name)
            } catch (e: Exception) {
                _error.value = "Could not start the room: ${e.message}"
                leave()
            } finally { _busy.value = false }
        }
    }

    fun join(link: JoinLink, name: String) {
        leave()
        _error.value = null
        connect(link.host, link.port, name)
    }

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
        discovery.stopAdvertising()
        HostService.stop(app)
    }

    private fun connect(host: String, port: Int, name: String) {
        val c = GameClient(scope)
        _client.value = c
        c.connect(host, port, name.ifBlank { "Player" })
    }
}
