package com.example.chess.domain

import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.rules.CheckDetector
import com.example.chess.domain.rules.MoveGenerator
import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure JVM unit tests for legal move generation and king safety.
 * Verifies piece movements, pinned pieces, discovered checks, single/double checks, and king safety.
 */
class MoveGeneratorTest {

    @Test
    fun testInitialPositionPawnAndKnightMoves() {
        val initial = BoardPosition.createInitial()
        val legalMoves = MoveGenerator.generateLegalMoves(initial)

        // 16 pawn moves (8 single + 8 double) + 4 knight moves = 20 total
        assertEquals(20, legalMoves.size)

        val pawns = legalMoves.filter { it.piece.type == PieceType.PAWN }
        assertEquals(16, pawns.size)

        val knights = legalMoves.filter { it.piece.type == PieceType.KNIGHT }
        assertEquals(4, knights.size)
    }

    @Test
    fun testPinnedPieceCannotMoveOffPinLine() {
        // White King on e1, White Bishop on e2, Black Rook on e8 -> Bishop is pinned along the e-file!
        // FEN: 4r3/8/8/8/8/8/4B3/4K3 w - - 0 1
        val fen = "4r3/8/8/8/8/8/4B3/4K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        val e2 = Square.fromAlgebraic("e2")!!
        val bishopMoves = MoveGenerator.generateLegalMoves(position, e2)

        // The pinned bishop cannot move to d3, c4, f3, etc., as that would expose White king to check
        assertTrue("Pinned bishop must have no legal moves off the ray", bishopMoves.isEmpty())

        // King can move off e1
        val e1 = Square.fromAlgebraic("e1")!!
        val kingMoves = MoveGenerator.generateLegalMoves(position, e1)
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("d1") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("f1") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("d2") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("f2") })
    }

    @Test
    fun testPinnedRookCanOnlyMoveAlongPinRay() {
        // White King on e1, White Rook on e3, Black Queen on e8 -> Rook can move along e-file, but cannot leave e-file
        val fen = "4q3/8/8/8/8/4R3/8/4K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        val e3 = Square.fromAlgebraic("e3")!!
        val rookMoves = MoveGenerator.generateLegalMoves(position, e3)

        // Can move to e2, e4, e5, e6, e7, e8 (capture)
        assertTrue(rookMoves.all { it.to.file == 4 })
        assertTrue(rookMoves.any { it.to == Square.fromAlgebraic("e8") }) // Queen capture
        assertTrue(rookMoves.any { it.to == Square.fromAlgebraic("e4") })
        assertFalse(rookMoves.any { it.to == Square.fromAlgebraic("d3") }) // Cannot step off e-file
        assertFalse(rookMoves.any { it.to == Square.fromAlgebraic("f3") })
    }

    @Test
    fun testDiscoveredCheck() {
        // White King on e1, White Knight on e4, White Rook on e2, Black King on e8
        // When Knight moves to f6, it uncovers a discovered check from the e2 Rook to the e8 King!
        val fen = "4k3/8/8/8/4N3/8/4R3/4K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        val nf6 = Move(
            from = Square.fromAlgebraic("e4")!!,
            to = Square.fromAlgebraic("f6")!!,
            piece = position.getPiece(Square.fromAlgebraic("e4")!!)!!
        )

        val nextPos = MoveGenerator.applyMove(position, nf6)
        assertTrue("Moving knight uncovers check to black king", CheckDetector.isKingInCheck(PieceColor.BLACK, nextPos.board))
    }

    @Test
    fun testDoubleCheckForcesKingMove() {
        // White has King on e1. Black delivers double check with Queen on e8 and Knight on f3+.
        // Position: 4q3/8/8/8/8/5n2/8/4K3 w - - 0 1
        val fen = "4q3/8/8/8/8/5n2/8/4K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        assertTrue("White king is in check", CheckDetector.isKingInCheck(PieceColor.WHITE, position.board))
        assertTrue("White king is in double check", CheckDetector.isDoubleCheck(PieceColor.WHITE, position.board))

        val allLegalMoves = MoveGenerator.generateLegalMoves(position)
        // In double check, the only legal moves are King moves (cannot block or capture two pieces at once)
        assertTrue(allLegalMoves.isNotEmpty())
        assertTrue("All legal moves in double check must be King moves", allLegalMoves.all { it.piece.type == PieceType.KING })
    }

    @Test
    fun testKingCannotMoveIntoAttackedSquare() {
        // White King on e1, Black Rook on d8. King cannot move to d1 or d2.
        val fen = "3r4/8/8/8/8/8/8/4K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        val kingMoves = MoveGenerator.generateLegalMoves(position, Square.fromAlgebraic("e1")!!)
        assertFalse(kingMoves.any { it.to == Square.fromAlgebraic("d1") })
        assertFalse(kingMoves.any { it.to == Square.fromAlgebraic("d2") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("f1") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("f2") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("e2") })
    }

    @Test
    fun testSlidingPieceBlockedByOwnAndEnemyPieces() {
        // White Rook on a1. White pawn on a3 (blocked at a2). White rook cannot pass a3.
        val fen = "8/8/8/8/8/P7/8/R3K3 w - - 0 1"
        val position = BoardPosition.fromFen(fen)

        val a1Moves = MoveGenerator.generateLegalMoves(position, Square.fromAlgebraic("a1")!!)
        assertTrue(a1Moves.any { it.to == Square.fromAlgebraic("a2") })
        assertFalse(a1Moves.any { it.to == Square.fromAlgebraic("a3") }) // Own pawn blocks
        assertFalse(a1Moves.any { it.to == Square.fromAlgebraic("a4") }) // Cannot jump
    }
}
