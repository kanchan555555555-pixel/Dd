package com.example

import com.example.engine.ArcadeGameEngine
import com.example.model.GameType
import com.example.model.InputAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `pong paddle position clamps and unpauses on action A`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val engine = ArcadeGameEngine(scope)
        engine.selectGame(GameType.PONG, autoStart = false)
        assertTrue(engine.state.value.isPaused)

        engine.handleInput(playerSlot = 1, action = InputAction.ACTION_A, pressed = true)
        assertFalse(engine.state.value.isPaused)

        engine.handleInput(playerSlot = 1, action = InputAction.PADDLE_POS, value = 0.25f, pressed = true)
        assertEquals(0.25f, engine.state.value.pong.p1Y, 0.001f)

        engine.handleInput(playerSlot = 1, action = InputAction.PADDLE_POS, value = 2.0f, pressed = true)
        assertEquals(0.90f, engine.state.value.pong.p1Y, 0.001f)
        scope.cancel()
    }

    @Test
    fun `tictactoe places player mark and smart AI responds`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val engine = ArcadeGameEngine(scope)
        engine.selectGame(GameType.TICTACTOE, autoStart = true)

        engine.handleInput(playerSlot = 1, action = InputAction.CELL_TAP, value = 0f, pressed = true)
        val cells = engine.state.value.ticTacToe.cells
        assertEquals(1, cells[0])
        assertEquals(1, cells.count { it == 2 })
        scope.cancel()
    }

    @Test
    fun `bricks game initializes 40 bricks and launches ball on action A`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val engine = ArcadeGameEngine(scope)
        engine.selectGame(GameType.BRICKS, autoStart = false)

        assertEquals(40, engine.state.value.bricks.bricks.size)
        assertTrue(engine.state.value.bricks.ballAttached)

        engine.handleInput(playerSlot = 1, action = InputAction.ACTION_A, pressed = true)
        assertFalse(engine.state.value.isPaused)
        assertFalse(engine.state.value.bricks.ballAttached)
        scope.cancel()
    }
}
