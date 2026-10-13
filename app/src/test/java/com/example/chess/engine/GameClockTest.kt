package com.example.chess.engine

import com.example.chess.model.PieceColor
import com.example.chess.model.TimeControl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameClockTest {

    @Test
    fun testStartAndStopClock() = runTest {
        var tickCount = 0
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L,
            onTick = { _, _ -> tickCount++ }
        )

        clock.start(TimeControl.BLITZ_5, PieceColor.WHITE)
        val state = clock.clockState.value
        assertTrue(state.isRunning)
        assertEquals(300, state.whiteTimeSeconds)
        assertEquals(300, state.blackTimeSeconds)
        assertEquals(PieceColor.WHITE, state.activeColor)

        // Advance 2 seconds
        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(2, tickCount)
        assertEquals(298, clock.clockState.value.whiteTimeSeconds)
        assertEquals(300, clock.clockState.value.blackTimeSeconds)

        // Stop the clock
        clock.stop()
        assertFalse(clock.clockState.value.isRunning)

        // Advancing time should not tick anymore
        advanceTimeBy(3000L)
        runCurrent()
        assertEquals(2, tickCount)
        assertEquals(298, clock.clockState.value.whiteTimeSeconds)
    }

    @Test
    fun testActivePlayerSwitchingWithIncrement() = runTest {
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L
        )

        clock.start(TimeControl.RAPID_15_10, PieceColor.WHITE)
        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(898, clock.clockState.value.whiteTimeSeconds)

        // Switch to Black with 10 second increment for White
        clock.switchTurn(PieceColor.BLACK, incrementSeconds = 10)
        assertEquals(908, clock.clockState.value.whiteTimeSeconds)
        assertEquals(PieceColor.BLACK, clock.clockState.value.activeColor)

        // Next 3 ticks should decrement Black's time
        advanceTimeBy(3000L)
        runCurrent()
        assertEquals(897, clock.clockState.value.blackTimeSeconds)
        assertEquals(908, clock.clockState.value.whiteTimeSeconds)
    }

    @Test
    fun testPauseResumeAndReset() = runTest {
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L
        )

        clock.start(TimeControl.RAPID_10, PieceColor.WHITE)
        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(599, clock.clockState.value.whiteTimeSeconds)

        // Pause
        clock.pause()
        assertFalse(clock.clockState.value.isRunning)
        advanceTimeBy(5000L)
        runCurrent()
        assertEquals(599, clock.clockState.value.whiteTimeSeconds)

        // Resume
        clock.resume()
        assertTrue(clock.clockState.value.isRunning)
        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(597, clock.clockState.value.whiteTimeSeconds)

        // Reset
        clock.reset(TimeControl.RAPID_10, PieceColor.WHITE)
        assertEquals(600, clock.clockState.value.whiteTimeSeconds)
        assertEquals(600, clock.clockState.value.blackTimeSeconds)
        assertTrue(clock.clockState.value.isRunning)
    }

    @Test
    fun testTimeoutHandling() = runTest {
        var timedOutColor: PieceColor? = null
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L,
            onTimeout = { color -> timedOutColor = color }
        )

        // Start with only 2 seconds on White's clock
        clock.start(TimeControl.BULLET_3, PieceColor.WHITE, whiteSeconds = 2, blackSeconds = 180)
        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(1, clock.clockState.value.whiteTimeSeconds)
        assertFalse(clock.clockState.value.isExpired)

        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(0, clock.clockState.value.whiteTimeSeconds)
        assertTrue(clock.clockState.value.isExpired)
        assertEquals(PieceColor.WHITE, clock.clockState.value.expiredColor)
        assertEquals(PieceColor.WHITE, timedOutColor)
        assertFalse(clock.clockState.value.isRunning)
    }

    @Test
    fun testPreventionOfDuplicateTimerJobs() = runTest {
        var tickCount = 0
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L,
            onTick = { _, _ -> tickCount++ }
        )

        // Multiple calls to start should not spawn overlapping timers
        clock.start(TimeControl.BLITZ_5, PieceColor.WHITE)
        clock.start(TimeControl.BLITZ_5, PieceColor.WHITE)
        clock.start(TimeControl.BLITZ_5, PieceColor.WHITE)

        advanceTimeBy(2000L)
        runCurrent()
        // Should only tick 2 times (from the single active job)
        assertEquals(2, tickCount)
    }

    @Test
    fun testCancellationAndUnlimitedMode() = runTest {
        var tickCount = 0
        val clock = GameClock(
            scope = this,
            tickDelayMs = 1000L,
            onTick = { _, _ -> tickCount++ }
        )

        // Unlimited mode should not run timer
        clock.start(TimeControl.UNLIMITED, PieceColor.WHITE)
        assertFalse(clock.clockState.value.isRunning)
        advanceTimeBy(5000L)
        runCurrent()
        assertEquals(0, tickCount)

        // Cancel clock
        clock.cancel()
        assertFalse(clock.clockState.value.isRunning)
    }
}
