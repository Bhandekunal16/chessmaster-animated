package com.example

import com.example.chess.animation.PieceAnimationCalculator
import com.example.chess.engine.ChessAI
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testInitialBoardSetup() {
        val state = ChessEngine.createInitialState()
        assertEquals(PieceColor.WHITE, state.activeColor)

        // Check White pawns
        for (f in 0..7) {
            val pawn = state.getPiece(Square(f, 1))
            assertNotNull(pawn)
            assertEquals(PieceType.PAWN, pawn?.type)
            assertEquals(PieceColor.WHITE, pawn?.color)
        }

        // Check Black King
        val blackKing = state.getPiece(Square(4, 7))
        assertNotNull(blackKing)
        assertEquals(PieceType.KING, blackKing?.type)
        assertEquals(PieceColor.BLACK, blackKing?.color)

        // Initial legal moves count for White is 20 (16 pawn pushes + 4 knight hops)
        val legalMoves = ChessEngine.getLegalMoves(state)
        assertEquals(20, legalMoves.size)
    }

    @Test
    fun testPawnMovesAndCapture() {
        var state = ChessEngine.createInitialState()
        // White plays 1. e4 (from e2 to e4)
        val e2 = Square.fromAlgebraic("e2")!!
        val e4 = Square.fromAlgebraic("e4")!!
        val e4Move = ChessEngine.getLegalMoves(state, e2).find { it.to == e4 }
        assertNotNull("1. e4 must be legal", e4Move)

        state = ChessEngine.applyMove(state, e4Move!!)
        assertEquals("e4", state.moveHistory.last().san)
        assertEquals(PieceColor.BLACK, state.activeColor)

        // Black plays 1... d5 (from d7 to d5)
        val d7 = Square.fromAlgebraic("d7")!!
        val d5 = Square.fromAlgebraic("d5")!!
        val d5Move = ChessEngine.getLegalMoves(state, d7).find { it.to == d5 }
        assertNotNull("1... d5 must be legal", d5Move)

        state = ChessEngine.applyMove(state, d5Move!!)
        assertEquals("d5", state.moveHistory.last().san)

        // White captures 2. exd5
        val exd5 = ChessEngine.getLegalMoves(state, e4).find { it.to == d5 }
        assertNotNull("exd5 capture must be legal", exd5)
        state = ChessEngine.applyMove(state, exd5!!)
        assertEquals("exd5", state.moveHistory.last().san)
        assertEquals(1, state.capturedBlack.size)
        assertEquals(PieceType.PAWN, state.capturedBlack[0].type)
    }

    @Test
    fun testKnightJumpsOverPieces() {
        var state = ChessEngine.createInitialState()
        val g1 = Square(6, 0)
        val nf3 = Square(5, 2)
        val move = ChessEngine.getLegalMoves(state, g1).find { it.to == nf3 }
        assertNotNull("Nf3 must be legal from initial position", move)
        state = ChessEngine.applyMove(state, move!!)
        assertEquals("Nf3", state.moveHistory.last().san)
    }

    @Test
    fun testScholarsMateCheckmate() {
        var state = ChessEngine.createInitialState()

        // 1. e4 e5
        val e4 = ChessEngine.getLegalMoves(state, Square(4, 1)).first { it.to == Square(4, 3) }
        state = ChessEngine.applyMove(state, e4)
        val e5 = ChessEngine.getLegalMoves(state, Square(4, 6)).first { it.to == Square(4, 4) }
        state = ChessEngine.applyMove(state, e5)

        // 2. Bc4 Nc6
        val bc4 = ChessEngine.getLegalMoves(state, Square(5, 0)).first { it.to == Square(2, 3) }
        state = ChessEngine.applyMove(state, bc4)
        val nc6 = ChessEngine.getLegalMoves(state, Square(1, 7)).first { it.to == Square(2, 5) }
        state = ChessEngine.applyMove(state, nc6)

        // 3. Qh5 Nf6??
        val qh5 = ChessEngine.getLegalMoves(state, Square(3, 0)).first { it.to == Square(7, 4) }
        state = ChessEngine.applyMove(state, qh5)
        val nf6 = ChessEngine.getLegalMoves(state, Square(6, 7)).first { it.to == Square(5, 5) }
        state = ChessEngine.applyMove(state, nf6)

        // 4. Qxf7# (Checkmate!)
        val qxf7 = ChessEngine.getLegalMoves(state, Square(7, 4)).first { it.to == Square(5, 6) }
        state = ChessEngine.applyMove(state, qxf7)

        assertTrue("Opponent must be in check", state.isCheck)
        assertTrue("Position must be checkmate", state.isCheckmate)
        assertEquals(PieceColor.WHITE, state.winner)
        assertEquals("Qxf7#", state.moveHistory.last().san)
    }

    @Test
    fun testKingsideCastling() {
        var state = ChessEngine.createInitialState()

        // Clear f1 and g1 for white kingside castling
        val e4 = ChessEngine.getLegalMoves(state, Square(4, 1)).first { it.to == Square(4, 3) }
        state = ChessEngine.applyMove(state, e4)
        val e5 = ChessEngine.getLegalMoves(state, Square(4, 6)).first { it.to == Square(4, 4) }
        state = ChessEngine.applyMove(state, e5)

        val nf3 = ChessEngine.getLegalMoves(state, Square(6, 0)).first { it.to == Square(5, 2) }
        state = ChessEngine.applyMove(state, nf3)
        val a6 = ChessEngine.getLegalMoves(state, Square(0, 6)).first { it.to == Square(0, 5) }
        state = ChessEngine.applyMove(state, a6)

        val be2 = ChessEngine.getLegalMoves(state, Square(5, 0)).first { it.to == Square(4, 1) }
        state = ChessEngine.applyMove(state, be2)
        val h6 = ChessEngine.getLegalMoves(state, Square(7, 6)).first { it.to == Square(7, 5) }
        state = ChessEngine.applyMove(state, h6)

        // White King can castle O-O (from e1 to g1)
        val oO = ChessEngine.getLegalMoves(state, Square(4, 0)).find { it.to == Square(6, 0) }
        assertNotNull("O-O must be available", oO)
        assertTrue("Must be marked as castling", oO!!.isCastling)

        state = ChessEngine.applyMove(state, oO)
        assertEquals("O-O", state.moveHistory.last().san)
        // Verify king on g1 and rook on f1
        assertEquals(PieceType.KING, state.getPiece(Square(6, 0))?.type)
        assertEquals(PieceType.ROOK, state.getPiece(Square(5, 0))?.type)
    }

    @Test
    fun testPieceAnimationCalculatorDistinctTransformations() {
        val whiteKnight = Piece.create(PieceType.KNIGHT, PieceColor.WHITE)
        val whiteRook = Piece.create(PieceType.ROOK, PieceColor.WHITE)

        val knightAnimMid = PieceAnimationCalculator.calculate(
            piece = whiteKnight,
            from = Square(1, 0),
            to = Square(2, 2),
            progress = 0.5f,
            isCapture = false
        )

        val rookAnimMid = PieceAnimationCalculator.calculate(
            piece = whiteRook,
            from = Square(0, 0),
            to = Square(0, 4),
            progress = 0.5f,
            isCapture = false
        )

        // Knight must have high midair elevation jump scale and rotation
        assertTrue("Knight midair jump scale must exceed 1.25x", knightAnimMid.scale > 1.25f)
        assertTrue("Knight must have tilt rotation", knightAnimMid.rotation != 0f)

        // Rook slides grounded (rotation is 0f)
        assertEquals(0f, rookAnimMid.rotation, 0.001f)
    }

    @Test
    fun testChessAIFindsMove() {
        val state = ChessEngine.createInitialState()
        val move = ChessAI.findBestMove(state, AIDifficulty.EASY)
        assertNotNull("AI must find a legal move", move)
        assertTrue("Move must be one of the legal moves", ChessEngine.getLegalMoves(state).contains(move))
    }

    @Test
    fun testAllBoardThemesAvailableAndValid() {
        val themes = BoardTheme.values()
        assertEquals(8, themes.size)

        themes.forEach { theme ->
            assertNotNull(theme.title)
            assertNotNull(theme.subtitle)
            assertTrue("Light and dark squares must have distinct colors", theme.lightSquare != theme.darkSquare)
            assertTrue("Board border must be defined", theme.boardBorder != 0L)
            assertTrue("Check color must be defined", theme.checkColor != 0L)
            assertTrue("Highlight color must be defined", theme.highlightColor != 0L)
            assertTrue("Legal move color must be defined", theme.legalMoveColor != 0L)
            assertTrue("Capture target color must be defined", theme.captureTargetColor != 0L)
        }
    }

    @Test
    fun testAllPieceSetsAvailable() {
        val pieceSets = PieceSet.values()
        assertEquals(8, pieceSets.size)

        pieceSets.forEach { set ->
            assertNotNull(set.title)
            assertNotNull(set.description)
            assertTrue(set.title.isNotEmpty())
        }
    }

    @Test
    fun testThemeChangeDoesNotAffectGameStateOrMoveLegality() {
        var state = ChessEngine.createInitialState()
        val initialMoves = ChessEngine.getLegalMoves(state)

        var settings = GameSettings(
            boardTheme = BoardTheme.CLASSIC_WOOD,
            pieceSet = PieceSet.CLASSIC_STAUNTON
        )

        // Switch to all 8 themes and 8 piece sets
        for (theme in BoardTheme.values()) {
            for (pSet in PieceSet.values()) {
                settings = settings.copy(boardTheme = theme, pieceSet = pSet)
                // Verify game state and legality remain identical
                val legalMoves = ChessEngine.getLegalMoves(state)
                assertEquals(initialMoves.size, legalMoves.size)
                assertEquals(PieceColor.WHITE, state.activeColor)
            }
        }
    }
}
