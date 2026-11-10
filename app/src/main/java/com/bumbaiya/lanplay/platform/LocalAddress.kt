package com.bumbaiya.lanplay.platform

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Finds this phone's address on the local network, which is what other phones connect to.
 * When this phone shares a hotspot the address is usually 192.168.43.1 (or similar) on an
 * interface called ap0/swlan0/wlan1; when it is connected to someone else's Wi-Fi it is on wlan0.
 */
object LocalAddress {
    fun find(): String? {
        val candidates = runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { nic -> nic.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.map { nic.name to it.hostAddress!! } }
        // Prefer a hotspot interface, then Wi-Fi, then anything else on the local network.
        return candidates.firstOrNull { (n, _) -> n.startsWith("ap") || n.startsWith("swlan") || n == "wlan1" }?.second
            ?: candidates.firstOrNull { (n, _) -> n.startsWith("wlan") }?.second
            ?: candidates.firstOrNull()?.second
    }
}

/**
 * Wi-Fi without internet (a home router with no data, a phone hotspot) often makes Android send
 * traffic over mobile data instead, so connecting to 192.168.x.x silently fails. While we are in a
 * room we pin this app's traffic to the Wi-Fi network so local addresses are reachable.
 */
object WifiBinding {
    @Suppress("DEPRECATION")
    fun bind(context: android.content.Context) {
        runCatching {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val wifi = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true }
            if (wifi != null) cm.bindProcessToNetwork(wifi)
        }
    }

    fun unbind(context: android.content.Context) {
        runCatching {
            (context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).bindProcessToNetwork(null)
        }
    }
}
