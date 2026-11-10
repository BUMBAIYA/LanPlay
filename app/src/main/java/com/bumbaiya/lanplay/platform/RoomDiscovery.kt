package com.bumbaiya.lanplay.platform

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.bumbaiya.lanplay.net.JoinLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Finding a room by its ID with no typing of addresses. The host announces itself on the local
 * network (mDNS / "Bonjour") under the name "LanPlay-ROOMID"; a joiner who types ROOMID searches
 * for that name and gets the address from the answer.
 */
class RoomDiscovery(context: Context) {
    private val app = context.applicationContext
    private val nsd = app.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val _rooms = MutableStateFlow<Map<String, JoinLink>>(emptyMap())
    /** Rooms found so far, by room ID. */
    val rooms: StateFlow<Map<String, JoinLink>> = _rooms

    /** Host side: announce the room until [stopAdvertising]. */
    fun advertise(roomId: String, port: Int, ip: String? = null) {
        stopAdvertising()
        // The IPv4 address goes in the announcement so joiners never have to guess between IPv4/IPv6.
        val info = NsdServiceInfo().apply { serviceName = SERVICE_PREFIX + roomId; serviceType = SERVICE_TYPE; setPort(port); ip?.let { setAttribute("ip", it) } }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, code: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, code: Int) {}
        }
        registration = l
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stopAdvertising() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
    }

    /** Joiner side: start searching. Safe to call again. */
    fun startSearching() {
        if (discovery != null) return
        multicastLock = wifi.createMulticastLock("lanplay").apply { setReferenceCounted(false); runCatching { acquire() } }
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, c: Int) { discovery = null }
            override fun onStopDiscoveryFailed(t: String, c: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (info.serviceName.startsWith(SERVICE_PREFIX)) resolve(info)
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                val id = info.serviceName.removePrefix(SERVICE_PREFIX)
                _rooms.value = _rooms.value - id
            }
        }
        discovery = l
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stopSearching() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discovery = null
        runCatching { multicastLock?.release() }
        multicastLock = null
        _rooms.value = emptyMap()
    }

    /** Waits up to [timeoutMs] for [roomId] to show up. Returns null if it never does. */
    suspend fun find(roomId: String, timeoutMs: Long = 8000): JoinLink? =
        withTimeoutOrNull(timeoutMs) { _rooms.map { it[roomId.uppercase()] }.first { it != null } }

    @Suppress("DEPRECATION") // the replacement needs API 34; this works on every version we support
    private fun resolve(info: NsdServiceInfo) {
        runCatching {
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, code: Int) {
                    // Android resolves one service at a time; try again shortly if it was busy.
                    if (code == NsdManager.FAILURE_ALREADY_ACTIVE && discovery != null) handler.postDelayed({ resolve(info) }, 400)
                }
                override fun onServiceResolved(i: NsdServiceInfo) {
                    val announced = i.attributes["ip"]?.let { String(it) }
                    val v4 = (i.host as? java.net.Inet4Address)?.hostAddress
                    val host = announced ?: v4 ?: i.host?.hostAddress?.substringBefore('%') ?: return
                    val id = i.serviceName.removePrefix(SERVICE_PREFIX)
                    _rooms.value = _rooms.value + (id to JoinLink(id, host, i.port))
                }
            })
        }
    }

    companion object {
        const val SERVICE_TYPE = "_lanplay._tcp."
        const val SERVICE_PREFIX = "LanPlay-"
    }
}
