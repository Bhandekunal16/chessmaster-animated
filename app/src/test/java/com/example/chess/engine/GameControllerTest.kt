package com.example.chess.engine

import com.example.chess.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun testValidPlayerMoveAndTurnTransition() {
        val controller = GameController(scope = testScope, computeDispatcher = testDispatcher)
        val initial = controller.gameState.value
        assertEquals(PieceColor.WHITE, initial.activeColor)
        assertEquals(0, initial.moveHistory.size)

        // 1. e4 (from e2 to e4)
        val e2 = Square.fromAlgebraic("e2")!!
        val e4 = Square.fromAlgebraic("e4")!!
        val e4Move = controller.getLegalMoves(e2).first { it.to == e4 }

        val success = controller.applyMove(e4Move)
        assertTrue("Move e4 should be applied successfully", success)

        val updated = controller.gameState.value
        assertEquals(PieceColor.BLACK, updated.activeColor)
        assertEquals(1, updated.moveHistory.size)
        assertEquals("e4", updated.moveHistory.first().san)
        assertNotNull(updated.getPiece(e4))
        assertNull(updated.getPiece(e2))
    }

    @Test
    fun testInvalidOrConflictingMoveRequests() {
        val controller = GameController(scope = testScope, computeDispatcher = testDispatcher)

        // Attempt to move Black pawn on White's turn
        val d7 = Square.fromAlgebraic("d7")!!
        val d5 = Square.fromAlgebraic("d5")!!
        val illegalBlackPawn = Move(
            from = d7,
            to = d5,
            piece = Piece.create(PieceType.PAWN, PieceColor.BLACK)
        )

        val success = controller.applyMove(illegalBlackPawn)
        assertFalse("Illegal move should be rejected", success)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
        assertEquals(0, controller.gameState.value.moveHistory.size)
    }

    @Test
    fun testUndoAndRedoBehavior() {
        val controller = GameController(scope = testScope, computeDispatcher = testDispatcher)

        // Play 1. e4
        val e4Move = controller.getLegalMoves(Square.fromAlgebraic("e2")!!).first { it.to == Square.fromAlgebraic("e4")!! }
        controller.applyMove(e4Move)

        // Play 1... e5
        val e5Move = controller.getLegalMoves(Square.fromAlgebraic("e7")!!).first { it.to == Square.fromAlgebraic("e5")!! }
        controller.applyMove(e5Move)

        assertEquals(2, controller.gameState.value.moveHistory.size)

        // Undo in PVP mode (should undo 1 move -> Black's e5)
        val undonePvp = controller.undoMove(GameMode.PVP)
        assertTrue(undonePvp)
        assertEquals(1, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.BLACK, controller.gameState.value.activeColor)
        assertEquals(1, controller.gameState.value.redoHistory.size)

        // Redo move
        val redone = controller.redoMove()
        assertTrue(redone)
        assertEquals(2, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
        assertEquals(0, controller.gameState.value.redoHistory.size)

        // Undo in PVC mode (should undo 2 moves -> both player and AI to return to White)
        val undonePvc = controller.undoMove(GameMode.PVC)
        assertTrue(undonePvc)
        assertEquals(0, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
        assertEquals(2, controller.gameState.value.redoHistory.size)
    }

    @Test
    fun testAIMoveExecution() = testScope.runTest {
        val controller = GameController(scope = this, computeDispatcher = testDispatcher)

        // White plays 1. e4
        val e4Move = controller.getLegalMoves(Square.fromAlgebraic("e2")!!).first { it.to == Square.fromAlgebraic("e4")!! }
        controller.applyMove(e4Move)
        assertEquals(PieceColor.BLACK, controller.gameState.value.activeColor)

        // Trigger AI for Black
        val aiJob = controller.triggerAIMove(AIDifficulty.EASY)
        advanceUntilIdle()

        // Verify AI executed a move
        assertEquals(2, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
        assertFalse(controller.isThinking.value)
    }

    @Test
    fun testRestartDuringAIComputation() = testScope.runTest {
        val controller = GameController(scope = this, computeDispatcher = testDispatcher)

        // White plays 1. e4
        val e4Move = controller.getLegalMoves(Square.fromAlgebraic("e2")!!).first { it.to == Square.fromAlgebraic("e4")!! }
        controller.applyMove(e4Move)

        // Trigger AI move
        val aiJob = controller.triggerAIMove(AIDifficulty.HARD, delayMs = 500L)
        assertTrue(controller.isThinking.value)

        // Restart game while AI is computing
        controller.restartGame()

        advanceUntilIdle()

        // Game should be back to initial state, AI job cancelled, not thinking
        assertEquals(0, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
        assertFalse(controller.isThinking.value)
        assertTrue(aiJob.isCancelled || !aiJob.isActive)
    }

    @Test
    fun testRejectionOfStaleAIResults() = testScope.runTest {
        val controller = GameController(scope = this, computeDispatcher = testDispatcher)

        // White plays 1. e4
        val e4Move = controller.getLegalMoves(Square.fromAlgebraic("e2")!!).first { it.to == Square.fromAlgebraic("e4")!! }
        controller.applyMove(e4Move)

        // Trigger AI move with delay
        controller.triggerAIMove(AIDifficulty.EASY, delayMs = 300L)

        // While AI is computing, player undoes move
        controller.undoMove(GameMode.PVP)
        assertEquals(0, controller.gameState.value.moveHistory.size)

        advanceUntilIdle()

        // Stale AI move must NOT be applied to initial state
        assertEquals(0, controller.gameState.value.moveHistory.size)
        assertEquals(PieceColor.WHITE, controller.gameState.value.activeColor)
    }

    @Test
    fun testGameOverTransitions() {
        val controller = GameController(scope = testScope, computeDispatcher = testDispatcher)

        // Scholar's Mate: 1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6 4. Qxf7#
        val moves = listOf("e2-e4", "e7-e5", "f1-c4", "b8-c6", "d1-h5", "g8-f6", "h5-f7")

        for (notation in moves) {
            val parts = notation.split("-")
            val from = Square.fromAlgebraic(parts[0])!!
            val to = Square.fromAlgebraic(parts[1])!!
            val move = controller.getLegalMoves(from).first { it.to == to }
            val applied = controller.applyMove(move)
            assertTrue("Move $notation must be applied", applied)
        }

        val finalState = controller.gameState.value
        assertTrue(finalState.isCheckmate)
        assertTrue(finalState.isGameOver)
        assertEquals(PieceColor.WHITE, finalState.winner)

        // Further moves should be rejected
        val furtherMoveAttempt = controller.getLegalMoves().firstOrNull()
        if (furtherMoveAttempt != null) {
            assertFalse(controller.applyMove(furtherMoveAttempt))
        }
    }
}
