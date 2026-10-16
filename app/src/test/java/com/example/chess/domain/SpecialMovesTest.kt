package com.example.chess.domain

import com.example.chess.domain.model.BoardPosition
import com.example.chess.domain.rules.CheckDetector
import com.example.chess.domain.rules.MoveGenerator
import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure JVM unit tests for special chess rules:
 * Castling (both sides, through check, rights revocation on king/rook move/capture),
 * En Passant (availability, expiration, king safety/pin check),
 * Pawn Promotion (all 4 pieces, capture promotion, immediate check impact).
 */
class SpecialMovesTest {

    @Test
    fun testWhiteKingsideAndQueensideCastling() {
        // White King on e1, Rooks on a1 and h1, empty intervening squares
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val pos = BoardPosition.fromFen(fen)

        val e1 = Square.fromAlgebraic("e1")!!
        val kingMoves = MoveGenerator.generateLegalMoves(pos, e1)

        val kingside = kingMoves.find { it.isCastling && it.isKingsideCastle }
        assertNotNull("Kingside castling must be legal", kingside)
        assertEquals(Square.fromAlgebraic("g1"), kingside!!.to)

        val queenside = kingMoves.find { it.isCastling && !it.isKingsideCastle }
        assertNotNull("Queenside castling must be legal", queenside)
        assertEquals(Square.fromAlgebraic("c1"), queenside!!.to)

        // Apply kingside castling and verify Rook moved to f1 and King to g1
        val afterO_O = MoveGenerator.applyMove(pos, kingside)
        assertEquals(PieceType.KING, afterO_O.getPiece(Square.fromAlgebraic("g1")!!)?.type)
        assertEquals(PieceType.ROOK, afterO_O.getPiece(Square.fromAlgebraic("f1")!!)?.type)
        assertNull(afterO_O.getPiece(Square.fromAlgebraic("e1")!!))
        assertNull(afterO_O.getPiece(Square.fromAlgebraic("h1")!!))
        assertFalse("White castling rights revoked after castling", afterO_O.castlingRights.whiteKingside)
        assertFalse(afterO_O.castlingRights.whiteQueenside)
    }

    @Test
    fun testCastlingRejectedWhileInCheckOrThroughCheck() {
        // 1. King in check cannot castle
        // White King on e1, Black Rook on e8 giving check
        val fenCheck = "4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1"
        val posCheck = BoardPosition.fromFen(fenCheck)
        val movesInCheck = MoveGenerator.generateLegalMoves(posCheck, Square.fromAlgebraic("e1")!!)
        assertFalse("Cannot castle while in check", movesInCheck.any { it.isCastling })

        // 2. Castling through check: Black Rook on f8 attacking f1
        val fenThroughCheck = "5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1"
        val posThrough = BoardPosition.fromFen(fenThroughCheck)
        val movesThrough = MoveGenerator.generateLegalMoves(posThrough, Square.fromAlgebraic("e1")!!)
        assertFalse("Cannot castle kingside through attacked square f1", movesThrough.any { it.isCastling && it.isKingsideCastle })
        assertTrue("Queenside castling remains legal if not attacked", movesThrough.any { it.isCastling && !it.isKingsideCastle })
    }

    @Test
    fun testCastlingRightsRevokedWhenRookCapturedOnOriginalSquare() {
        // Black Knight on b3 captures White Rook on a1 (Nxa1)
        val fen = "r3k2r/8/8/8/8/1n6/8/R3K2R b KQkq - 0 1"
        val pos = BoardPosition.fromFen(fen)

        val nxa1 = Move(
            from = Square.fromAlgebraic("b3")!!,
            to = Square.fromAlgebraic("a1")!!,
            piece = pos.getPiece(Square.fromAlgebraic("b3")!!)!!,
            capturedPiece = pos.getPiece(Square.fromAlgebraic("a1")!!)
        )

        val nextPos = MoveGenerator.applyMove(pos, nxa1)
        assertFalse("White queenside right must be lost when a1 rook is captured", nextPos.castlingRights.whiteQueenside)
        assertTrue("White kingside right is preserved", nextPos.castlingRights.whiteKingside)
    }

    @Test
    fun testEnPassantAvailabilityAndExpiration() {
        // White pawn on e5. Black plays d7-d5. En passant target becomes d6.
        val fenBefore = "rnbqkbnr/pppppppp/8/4P3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        val posBefore = BoardPosition.fromFen(fenBefore)

        val d7d5 = Move(
            from = Square.fromAlgebraic("d7")!!,
            to = Square.fromAlgebraic("d5")!!,
            piece = posBefore.getPiece(Square.fromAlgebraic("d7")!!)!!
        )

        val posAfter = MoveGenerator.applyMove(posBefore, d7d5)
        assertEquals(Square.fromAlgebraic("d6"), posAfter.enPassantTarget)

        // White has legal en passant capture exd6
        val whiteMoves = MoveGenerator.generateLegalMoves(posAfter, Square.fromAlgebraic("e5")!!)
        val epMove = whiteMoves.find { it.isEnPassant }
        assertNotNull("En passant move exd6 must be legal", epMove)
        assertEquals(Square.fromAlgebraic("d6"), epMove!!.to)

        // If White plays a different move (e.g. a2-a3), en passant expires on the next turn!
        val a2a3 = MoveGenerator.generateLegalMoves(posAfter, Square.fromAlgebraic("a2")!!).first { it.to == Square.fromAlgebraic("a3")!! }
        val posA3 = MoveGenerator.applyMove(posAfter, a2a3)
        assertNull("En passant target must expire after non-capturing move", posA3.enPassantTarget)
    }

    @Test
    fun testEnPassantRejectedIfExposingKingToHorizontalCheck() {
        val board = Array(8) { Array<Piece?>(8) { null } }
        board[4][0] = Piece.create(PieceType.ROOK, PieceColor.BLACK) // a5
        board[4][4] = Piece.create(PieceType.KING, PieceColor.WHITE) // e5
        board[4][5] = Piece.create(PieceType.PAWN, PieceColor.WHITE) // f5
        board[4][6] = Piece.create(PieceType.PAWN, PieceColor.BLACK) // g5
        board[7][7] = Piece.create(PieceType.KING, PieceColor.BLACK) // h8

        val posWithEp = BoardPosition(
            board = board,
            activeColor = PieceColor.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = Square.fromAlgebraic("g6")
        )

        val f5Moves = MoveGenerator.generateLegalMoves(posWithEp, Square.fromAlgebraic("f5")!!)
        assertFalse(
            "En passant must be rejected when it exposes king horizontally to rook check",
            f5Moves.any { it.isEnPassant }
        )
    }

    @Test
    fun testPawnPromotionAllFourPiecesAndCapture() {
        // White pawn on a7, Black knight on b8.
        val fen = "1n6/P7/8/8/8/8/8/4K2k w - - 0 1"
        val pos = BoardPosition.fromFen(fen)

        val a7 = Square.fromAlgebraic("a7")!!
        val moves = MoveGenerator.generateLegalMoves(pos, a7)

        // 4 quiet promotions to a8 (Q, R, B, N) + 4 capture promotions to b8 (Q, R, B, N) = 8 moves
        assertEquals(8, moves.size)

        val quietPromos = moves.filter { it.to == Square.fromAlgebraic("a8")!! }
        assertEquals(4, quietPromos.size)
        assertTrue(quietPromos.any { it.promotionPiece == PieceType.QUEEN })
        assertTrue(quietPromos.any { it.promotionPiece == PieceType.ROOK })
        assertTrue(quietPromos.any { it.promotionPiece == PieceType.BISHOP })
        assertTrue(quietPromos.any { it.promotionPiece == PieceType.KNIGHT })

        val capturePromos = moves.filter { it.to == Square.fromAlgebraic("b8")!! }
        assertEquals(4, capturePromos.size)
        assertTrue(capturePromos.all { it.isCapture })
    }
}
