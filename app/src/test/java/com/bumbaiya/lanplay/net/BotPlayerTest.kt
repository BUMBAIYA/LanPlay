package com.bumbaiya.lanplay.net

import com.bumbaiya.lanplay.game.combinemaths.Intent
import com.bumbaiya.lanplay.game.combinemaths.Phase
import com.bumbaiya.lanplay.game.combinemaths.RowSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

/**
 * A stand-in second player for manual testing, NOT a normal test: it does nothing unless the
 * environment variable LANPLAY_BOT is set. Forward the emulator's port first:
 *   adb forward tcp:8765 tcp:8765
 * then run with LANPLAY_BOT=1. It joins the room on the emulator, readies up and plays dull turns.
 */
class BotPlayerTest {
    @Test fun bot() = runBlocking {
        if (System.getenv("LANPLAY_BOT") == null) return@runBlocking
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val c = GameClient(scope)
        c.connect("127.0.0.1", DEFAULT_PORT, "Bot")
        c.state.first { it != null }
        c.send(Intent.SetReady(true))
        withTimeoutOrNull(150_000) {
            var lastSeq = -1L
            while (true) {
                delay(1500)
                val s = c.state.value ?: continue
                val me = c.myId.value ?: continue
                if (s.seq == lastSeq) continue
                lastSeq = s.seq
                when (val p = s.phase) {
                    is Phase.DrawingTarget -> if (p.drawerId == me) { c.send(Intent.DrawTargetCard); delay(800); c.send(Intent.StopTarget) }
                    is Phase.Turns -> if (p.currentId == me) {
                        // Build a row slowly so a watcher can see it appear, then play on.
                        val row = listOf(RowSlot("5"), RowSlot("×"), RowSlot("6", raised = "2"))
                        for (n in 1..row.size) { c.send(Intent.Preview(row.take(n))); delay(1200) }
                        delay(1500)
                        if (!p.hasDrawn) c.send(Intent.DrawCard(number = true))
                        delay(500)
                        c.send(Intent.Pass)
                    }
                    else -> Unit
                }
            }
        }
        c.close()
    }
}
