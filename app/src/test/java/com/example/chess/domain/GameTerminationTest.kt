package com.example.chess.domain

import com.example.chess.domain.history.DrawRuleEvaluator
import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.model.GameResult
import com.example.chess.domain.rules.GameTerminationEvaluator
import com.example.chess.domain.rules.MoveGenerator
import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure JVM unit tests for game termination conditions:
 * Checkmate, Stalemate, Insufficient Material, Threefold & Fivefold Repetition,
 * Fifty-Move & Seventy-Five Move rules, and Checkmate Precedence.
 */
class GameTerminationTest {

    @Test
    fun testFoolsMateCheckmate() {
        // 1. f3 e5 2. g4 Qh4# (Fool's mate)
        var pos = BoardPosition.createInitial()
        val moves = listOf(
            Square.fromAlgebraic("f2")!! to Square.fromAlgebraic("f3")!!,
            Square.fromAlgebraic("e7")!! to Square.fromAlgebraic("e5")!!,
            Square.fromAlgebraic("g2")!! to Square.fromAlgebraic("g4")!!,
            Square.fromAlgebraic("d8")!! to Square.fromAlgebraic("h4")!!
        )

        for ((from, to) in moves) {
            val legal = MoveGenerator.generateLegalMoves(pos, from).first { it.to == to }
            pos = MoveGenerator.applyMove(pos, legal)
        }

        val result = GameTerminationEvaluator.evaluate(pos)
        assertTrue("Position must be Checkmate", result is GameResult.Checkmate)
        assertEquals(PieceColor.BLACK, (result as GameResult.Checkmate).winner)
        assertTrue(result.isGameOver)
        assertFalse(result.isDraw)
    }

    @Test
    fun testClassicStalemate() {
        // Black King on a8, White King on c7, White Queen on b6. Black to move.
        // FEN: k7/2K5/1Q6/8/8/8/8/8 b - - 0 1
        val fen = "k7/2K5/1Q6/8/8/8/8/8 b - - 0 1"
        val pos = BoardPosition.fromFen(fen)

        val result = GameTerminationEvaluator.evaluate(pos)
        assertTrue("Position must be Stalemate", result is GameResult.Stalemate)
        assertTrue(result.isGameOver)
        assertTrue(result.isDraw)
        assertFalse(result.isCheck)
    }

    @Test
    fun testInsufficientMaterialVariations() {
        // 1. King vs King
        val kvk = BoardPosition.fromFen("8/8/8/4k3/8/8/4K3/8 w - - 0 1")
        assertTrue(DrawRuleEvaluator.isInsufficientMaterial(kvk.board))
        assertEquals(GameResult.InsufficientMaterial, GameTerminationEvaluator.evaluate(kvk))

        // 2. King + Bishop vs King
        val kbvk = BoardPosition.fromFen("8/8/8/4k3/8/5B2/4K3/8 w - - 0 1")
        assertTrue(DrawRuleEvaluator.isInsufficientMaterial(kbvk.board))
        assertEquals(GameResult.InsufficientMaterial, GameTerminationEvaluator.evaluate(kbvk))

        // 3. King + Knight vs King
        val knvk = BoardPosition.fromFen("8/8/8/4k3/8/5N2/4K3/8 w - - 0 1")
        assertTrue(DrawRuleEvaluator.isInsufficientMaterial(knvk.board))
        assertEquals(GameResult.InsufficientMaterial, GameTerminationEvaluator.evaluate(knvk))

        // 4. King + Bishop vs King + Bishop on SAME colored squares
        // White Bishop on c1 (rank 0, file 2: (0+2)%2 = 0, dark square)
        // Black Bishop on f8 (rank 7, file 5: (7+5)%2 = 0, dark square)
        val sameColorBishops = BoardPosition.fromFen("5b2/8/8/4k3/8/8/4K3/2B5 w - - 0 1")
        assertTrue("Bishops on same color squares is insufficient material", DrawRuleEvaluator.isInsufficientMaterial(sameColorBishops.board))
        assertEquals(GameResult.InsufficientMaterial, GameTerminationEvaluator.evaluate(sameColorBishops))

        // 5. King + Bishop vs King + Bishop on OPPOSITE colored squares
        // White Bishop on c1 (rank 0, file 2: 0, dark square)
        // Black Bishop on c8 (rank 7, file 2: (7+2)%2 = 1, light square)
        val oppositeColorBishops = BoardPosition.fromFen("2b5/8/8/4k3/8/8/4K3/2B5 w - - 0 1")
        assertFalse("Bishops on opposite color squares is NOT insufficient material", DrawRuleEvaluator.isInsufficientMaterial(oppositeColorBishops.board))

        // 6. King + Pawn vs King is NOT insufficient material
        val kpvk = BoardPosition.fromFen("8/8/8/4k3/4P3/8/4K3/8 w - - 0 1")
        assertFalse(DrawRuleEvaluator.isInsufficientMaterial(kpvk.board))
    }

    @Test
    fun testFiftyMoveRuleClaimableAndAutomatic() {
        // Position at halfMoveClock = 100 (50 full moves) without pawn move or capture (White has Rook on a1 so material is sufficient)
        val fen100 = "8/8/8/4k3/8/8/4K3/R7 w - - 100 51"
        val pos100 = BoardPosition.fromFen(fen100)
        val res100 = GameTerminationEvaluator.evaluate(pos100)

        assertTrue(res100 is GameResult.FiftyMoveRule)
        val fiftyRule = res100 as GameResult.FiftyMoveRule
        assertTrue("50-move rule must be claimable at 100 half-moves", fiftyRule.isClaimable)
        assertFalse("50-move rule is NOT automatic at 100 half-moves", fiftyRule.isAutomatic)

        // Position at halfMoveClock = 150 (75 full moves) -> Automatic draw (FIDE Rule 9.6.2)
        val fen150 = "8/8/8/4k3/8/8/4K3/R7 w - - 150 76"
        val pos150 = BoardPosition.fromFen(fen150)
        val res150 = GameTerminationEvaluator.evaluate(pos150)

        assertTrue(res150 is GameResult.FiftyMoveRule)
        val seventyFiveRule = res150 as GameResult.FiftyMoveRule
        assertTrue("75-move rule is automatic", seventyFiveRule.isAutomatic)
        assertTrue("Automatic 75-move rule ends game", seventyFiveRule.isGameOver)
        assertTrue(seventyFiveRule.isDraw)
    }

    @Test
    fun testCheckmatePrecedenceOverSeventyFiveMoveRule() {
        // If half-move clock is 150, but Black is in checkmate on this move, Checkmate wins!
        // Black King on h8, White Queen on g7, White King on f7
        val fenCheckmateAt150 = "7k/5Q2/5K2/8/8/8/8/8 b - - 150 76"
        val pos = BoardPosition.fromFen(fenCheckmateAt150)

        // Wait, on 7k/5Q2/5K2/8/8/8/8/8 b, Queen on f7? If Queen on g7, that's Qg7#.
        // Let's create: White Queen on g7#, White King on f6, Black King on h8
        val fenMate = "7k/6Q1/5K2/8/8/8/8/8 b - - 150 76"
        val posMate = BoardPosition.fromFen(fenMate)

        val result = GameTerminationEvaluator.evaluate(posMate)
        assertTrue("Checkmate must take precedence over 75-move automatic draw", result is GameResult.Checkmate)
        assertEquals(PieceColor.WHITE, (result as GameResult.Checkmate).winner)
    }

    @Test
    fun testThreefoldAndFivefoldRepetition() {
        val initial = BoardPosition.createInitial()
        val sig = com.example.chess.domain.history.RepetitionTracker.computeNormalizedSignature(initial)

        // Occurred twice before + current = 3 occurrences -> Claimable threefold repetition
        val history2 = listOf(sig, "other1", sig, "other2")
        val res3 = GameTerminationEvaluator.evaluate(initial, history2)
        assertTrue(res3 is GameResult.ThreefoldRepetition)
        val three = res3 as GameResult.ThreefoldRepetition
        assertTrue(three.isClaimable)
        assertFalse(three.isAutomatic)

        // Occurred 4 times before + current = 5 occurrences -> Automatic fivefold repetition
        val history4 = listOf(sig, sig, sig, sig)
        val res5 = GameTerminationEvaluator.evaluate(initial, history4)
        assertTrue(res5 is GameResult.ThreefoldRepetition)
        val five = res5 as GameResult.ThreefoldRepetition
        assertTrue(five.isClaimable)
        assertTrue(five.isAutomatic)
        assertTrue(five.isGameOver)
        assertTrue(five.isDraw)
    }
}
