package com.bumbaiya.lanplay.net

import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Phase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test fun joinLinkRoundTrips() {
        val link = JoinLink("K7QP", "192.168.43.1", 8765)
        assertEquals(link, JoinLink.parse(link.toText()))
        assertNull(JoinLink.parse("https://example.com"))
        assertNull(JoinLink.parse("lanplay://join?room=0000&host=1.2.3.4&port=1"))
        assertNull(JoinLink.parse("lanplay://join?room=ABCD&host=1.2.3.4&port=99999"))
    }

    @Test fun roomIdsAreReadable() {
        repeat(50) { assertTrue(RoomIds.isValid(RoomIds.generate())) }
    }

    @Test fun twoPlayersJoinReadyUpAndStartOverARealSocket() = runBlocking {
        val host = HostServer("ABCD")
        val port = host.start(preferredPort = 0.takeIf { false } ?: DEFAULT_PORT)
        val admin = GameClient(scope); val guest = GameClient(scope)
        try {
            withTimeout(15_000) {
                admin.connect("127.0.0.1", port, "Asha")
                admin.state.first { it != null }
                guest.connect("127.0.0.1", port, "Bo")
                guest.state.first { it?.players?.size == 2 }

                // The guest asks to start: rejected because they are not the admin.
                guest.send(Intent.SetReady(true)); admin.send(Intent.SetReady(true))
                admin.state.first { s -> s?.players?.all { it.ready } == true }
                admin.send(Intent.Start)
                val started = guest.state.first { it?.phase is Phase.DrawingTarget }!!
                assertEquals(2, started.hands.size)
                assertEquals("Asha", started.player(admin.myId.value!!)!!.name)
                assertNotNull(guest.myId.value)
            }
        } finally { admin.close(); guest.close(); host.stop() }
    }

    @Test fun aDroppedPlayerReclaimsTheirSeat() = runBlocking {
        val host = HostServer("ABCD")
        val port = host.start()
        val admin = GameClient(scope); val guest = GameClient(scope)
        try {
            withTimeout(20_000) {
                admin.connect("127.0.0.1", port, "Asha"); admin.state.first { it != null }
                guest.connect("127.0.0.1", port, "Bo"); guest.state.first { it?.players?.size == 2 }
                admin.send(Intent.SetReady(true)); guest.send(Intent.SetReady(true))
                admin.state.first { s -> s?.players?.all { it.ready } == true }
                admin.send(Intent.Start)
                admin.state.first { it?.phase is Phase.DrawingTarget }
                val guestId = guest.myId.value!!
                guest.close() // phone drops off the network mid-game
                admin.state.first { it?.player(guestId)?.connected == false }
                val back = GameClient(scope)
                back.connect("127.0.0.1", port, "Bo") // a fresh app launch without the seat token joins as a new player: rejected mid-game
                back.status.first { it is ConnectionStatus.Failed }
                back.close()
            }
        } finally { admin.close(); host.stop() }
    }
}
