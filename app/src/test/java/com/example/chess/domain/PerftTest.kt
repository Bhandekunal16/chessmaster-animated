package com.example.chess.domain

import com.example.chess.domain.model.BoardPosition
import com.example.chess.engine.ChessEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM Perft (Performance Test) regression suite against established reference node counts.
 * Validates legal move generation correctness across all piece types, castling, en passant,
 * promotions, checks, and discovered attacks.
 */
class PerftTest {

    @Test
    fun testInitialPositionPerft() {
        val initial = BoardPosition.createInitial()

        // Standard Chess reference counts for starting position:
        assertEquals(20L, ChessEngine.perft(initial, 1))
        assertEquals(400L, ChessEngine.perft(initial, 2))
        assertEquals(8902L, ChessEngine.perft(initial, 3))
        assertEquals(197281L, ChessEngine.perft(initial, 4))
    }

    @Test
    fun testPosition2Kiwipete() {
        // Kiwipete position exercises deep pins, castling, en passant, and double checks
        // FEN: r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1
        val pos = BoardPosition.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")

        assertEquals(48L, ChessEngine.perft(pos, 1))
        assertEquals(2039L, ChessEngine.perft(pos, 2))
        assertEquals(97862L, ChessEngine.perft(pos, 3))
    }

    @Test
    fun testPosition3PinsAndPawns() {
        // FEN: 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1
        val pos = BoardPosition.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")

        assertEquals(14L, ChessEngine.perft(pos, 1))
        assertEquals(191L, ChessEngine.perft(pos, 2))
        assertEquals(2812L, ChessEngine.perft(pos, 3))
    }

    @Test
    fun testPosition4CastlingAndPromotions() {
        // FEN: r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1
        val pos = BoardPosition.fromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")

        assertEquals(6L, ChessEngine.perft(pos, 1))
        assertEquals(264L, ChessEngine.perft(pos, 2))
    }

    @Test
    fun testPosition5PromotionsUnderCheck() {
        // FEN: rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8
        val pos = BoardPosition.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8")

        assertEquals(44L, ChessEngine.perft(pos, 1))
        assertEquals(1486L, ChessEngine.perft(pos, 2))
    }
}
