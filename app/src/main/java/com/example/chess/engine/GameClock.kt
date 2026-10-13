package com.example.chess.engine

import com.example.chess.model.PieceColor
import com.example.chess.model.TimeControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Immutable state representation for the chess clock.
 */
data class ClockState(
    val whiteTimeSeconds: Int = 0,
    val blackTimeSeconds: Int = 0,
    val activeColor: PieceColor = PieceColor.WHITE,
    val isRunning: Boolean = false,
    val isExpired: Boolean = false,
    val expiredColor: PieceColor? = null
)

/**
 * Dedicated clock component responsible for tracking player times, turn switching,
 * time controls, and timeout detection using structured coroutines.
 */
class GameClock(
    private val scope: CoroutineScope,
    private val tickDelayMs: Long = 1000L,
    private val onTick: ((whiteSeconds: Int, blackSeconds: Int) -> Unit)? = null,
    private val onTimeout: ((PieceColor) -> Unit)? = null
) {
    private val _clockState = MutableStateFlow(ClockState())
    val clockState: StateFlow<ClockState> = _clockState.asStateFlow()

    private var timerJob: Job? = null
    var currentTimeControl: TimeControl = TimeControl.UNLIMITED
        private set

    /**
     * Initializes and starts the timer with the specified [timeControl].
     * Cancels any existing timer job to prevent duplicates.
     */
    fun start(
        timeControl: TimeControl,
        initialColor: PieceColor = PieceColor.WHITE,
        whiteSeconds: Int = timeControl.totalSeconds,
        blackSeconds: Int = timeControl.totalSeconds
    ) {
        this.currentTimeControl = timeControl
        stopTimerJob()

        val shouldRun = timeControl != TimeControl.UNLIMITED && timeControl.totalSeconds > 0
        _clockState.value = ClockState(
            whiteTimeSeconds = whiteSeconds,
            blackTimeSeconds = blackSeconds,
            activeColor = initialColor,
            isRunning = shouldRun,
            isExpired = false,
            expiredColor = null
        )

        if (shouldRun) {
            startTimerJob()
        }
    }

    /**
     * Pauses the timer without resetting player times.
     */
    fun pause() {
        stopTimerJob()
        _clockState.value = _clockState.value.copy(isRunning = false)
    }

    /**
     * Resumes the timer for the active player if not expired.
     */
    fun resume() {
        if (currentTimeControl == TimeControl.UNLIMITED || _clockState.value.isExpired) return
        if (_clockState.value.isRunning && timerJob?.isActive == true) return

        _clockState.value = _clockState.value.copy(isRunning = true)
        startTimerJob()
    }

    /**
     * Switches the active clock to [nextColor], optionally adding [incrementSeconds]
     * to the player who just finished their turn.
     */
    fun switchTurn(nextColor: PieceColor, incrementSeconds: Int = 0) {
        val current = _clockState.value
        val updatedWhite = if (current.activeColor == PieceColor.WHITE) {
            current.whiteTimeSeconds + incrementSeconds
        } else {
            current.whiteTimeSeconds
        }
        val updatedBlack = if (current.activeColor == PieceColor.BLACK) {
            current.blackTimeSeconds + incrementSeconds
        } else {
            current.blackTimeSeconds
        }

        _clockState.value = current.copy(
            whiteTimeSeconds = updatedWhite,
            blackTimeSeconds = updatedBlack,
            activeColor = nextColor
        )
    }

    /**
     * Resets the clock to the initial time for [timeControl].
     */
    fun reset(timeControl: TimeControl = this.currentTimeControl, initialColor: PieceColor = PieceColor.WHITE) {
        start(timeControl, initialColor)
    }

    /**
     * Explicitly syncs the current remaining times.
     */
    fun syncTimes(whiteSeconds: Int, blackSeconds: Int) {
        _clockState.value = _clockState.value.copy(
            whiteTimeSeconds = whiteSeconds,
            blackTimeSeconds = blackSeconds
        )
    }

    /**
     * Stops and cancels any active timer job.
     */
    fun stop() {
        stopTimerJob()
        _clockState.value = _clockState.value.copy(isRunning = false)
    }

    /**
     * Clean up and cancel all resources when owner is destroyed.
     */
    fun cancel() {
        stop()
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startTimerJob() {
        stopTimerJob()
        timerJob = scope.launch {
            while (isActive) {
                delay(tickDelayMs)
                val current = _clockState.value
                if (!current.isRunning || current.isExpired) break

                if (current.activeColor == PieceColor.WHITE) {
                    val remaining = current.whiteTimeSeconds - 1
                    if (remaining <= 0) {
                        _clockState.value = current.copy(
                            whiteTimeSeconds = 0,
                            isRunning = false,
                            isExpired = true,
                            expiredColor = PieceColor.WHITE
                        )
                        onTimeout?.invoke(PieceColor.WHITE)
                        break
                    } else {
                        _clockState.value = current.copy(whiteTimeSeconds = remaining)
                        onTick?.invoke(remaining, current.blackTimeSeconds)
                    }
                } else {
                    val remaining = current.blackTimeSeconds - 1
                    if (remaining <= 0) {
                        _clockState.value = current.copy(
                            blackTimeSeconds = 0,
                            isRunning = false,
                            isExpired = true,
                            expiredColor = PieceColor.BLACK
                        )
                        onTimeout?.invoke(PieceColor.BLACK)
                        break
                    } else {
                        _clockState.value = current.copy(blackTimeSeconds = remaining)
                        onTick?.invoke(current.whiteTimeSeconds, remaining)
                    }
                }
            }
        }
    }
}
