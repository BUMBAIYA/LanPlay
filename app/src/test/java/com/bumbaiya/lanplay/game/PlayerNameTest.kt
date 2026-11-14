package com.bumbaiya.lanplay.game

import com.bumbaiya.lanplay.game.combinemaths.GameEngine
import com.bumbaiya.lanplay.game.combinemaths.GameState
import com.bumbaiya.lanplay.game.combinemaths.Outcome
import com.bumbaiya.lanplay.game.combinemaths.PlayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNameTest {
    @Test fun keepsLettersOnly() {
        assertEquals("AmitChauhan", PlayerName.clean("Amit Chauhan 123 !@#"))
        assertEquals("", PlayerName.clean("1234 !!"))
    }

    @Test fun capsAtFifteen() {
        assertEquals(15, PlayerName.clean("abcdefghijklmnopqrstuvwxyz").length)
    }

    @Test fun engineCleansNamesFromOtherPhones() {
        val engine = GameEngine()
        val out = engine.addPlayer(GameState("ABCD", "a"), "a", "Bad Name 9!")
        assertTrue(out is Outcome.Ok)
        assertEquals("BadName", (out as Outcome.Ok).state.players.single().name)
    }

    @Test fun `problem names the characters that are not allowed`() {
        assertEquals(null, PlayerName.problem("Amit"))
        assertEquals(null, PlayerName.problem(""))
        assertEquals("Can't use \"1\", space. Letters only.", PlayerName.problem("A1 t"))
        assertEquals("Up to 15 letters.", PlayerName.problem("a".repeat(16)))
        assertEquals(false, PlayerName.isValid("Am it"))
    }
}
