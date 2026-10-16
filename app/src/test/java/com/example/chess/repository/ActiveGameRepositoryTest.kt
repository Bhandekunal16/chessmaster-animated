package com.example.chess.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.engine.ChessEngine
import com.example.chess.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActiveGameRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SharedPreferencesActiveGameRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(SharedPreferencesActiveGameRepository.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = SharedPreferencesActiveGameRepository(prefs)
    }

    @Test
    fun testSaveLoadSnapshotRoundTrip() = runBlocking {
        val moves = listOf(
            SerializedMove(fromFile = 4, fromRank = 1, toFile = 4, toRank = 3), // e2 -> e4
            SerializedMove(fromFile = 4, fromRank = 6, toFile = 4, toRank = 4)  // e7 -> e5
        )

        val snapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 42L,
            mode = GameMode.PVC,
            aiDifficulty = AIDifficulty.HARD,
            playerColor = PieceColor.WHITE,
            timeControl = TimeControl.RAPID_10,
            whiteTimeSeconds = 580,
            blackTimeSeconds = 595,
            isTimerRunning = true,
            moveHistory = moves
        )

        repository.saveActiveGame(snapshot)

        val loaded = repository.loadActiveGame()
        assertNotNull(loaded)
        assertEquals(1, loaded!!.schemaVersion)
        assertEquals(42L, loaded.saveSequence)
        assertEquals(GameMode.PVC, loaded.mode)
        assertEquals(AIDifficulty.HARD, loaded.aiDifficulty)
        assertEquals(PieceColor.WHITE, loaded.playerColor)
        assertEquals(TimeControl.RAPID_10, loaded.timeControl)
        assertEquals(580, loaded.whiteTimeSeconds)
        assertEquals(595, loaded.blackTimeSeconds)
        assertEquals(2, loaded.moveHistory.size)
        assertEquals(4, loaded.moveHistory[0].fromFile)
        assertEquals(1, loaded.moveHistory[0].fromRank)
        assertEquals(4, loaded.moveHistory[0].toFile)
        assertEquals(3, loaded.moveHistory[0].toRank)
    }

    @Test
    fun testGameStateReconstructionPreservesBoardAndTurn() {
        // e4, e5, Nf3, Nc6, Bc4
        val moves = listOf(
            SerializedMove(4, 1, 4, 3), // 1. e4
            SerializedMove(4, 6, 4, 4), // 1... e5
            SerializedMove(6, 0, 5, 2), // 2. Nf3
            SerializedMove(1, 7, 2, 5), // 2... Nc6
            SerializedMove(5, 0, 2, 3)  // 3. Bc4
        )

        val snapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 5L,
            timeControl = TimeControl.BLITZ_5,
            whiteTimeSeconds = 290,
            blackTimeSeconds = 295,
            moveHistory = moves
        )

        val reconstructed = GameSnapshotSerializer.reconstructGameState(snapshot)
        assertNotNull(reconstructed)

        // Turn should be Black to move after 5 plies
        assertEquals(PieceColor.BLACK, reconstructed!!.activeColor)
        assertEquals(5, reconstructed.moveHistory.size)

        // Check pieces on board
        val bc4 = reconstructed.getPiece(Square(2, 3))
        assertNotNull(bc4)
        assertEquals(PieceType.BISHOP, bc4!!.type)
        assertEquals(PieceColor.WHITE, bc4.color)

        val nc6 = reconstructed.getPiece(Square(2, 5))
        assertNotNull(nc6)
        assertEquals(PieceType.KNIGHT, nc6!!.type)
        assertEquals(PieceColor.BLACK, nc6.color)

        // Clocks should be preserved but paused
        assertEquals(290, reconstructed.whiteTimeSeconds)
        assertEquals(295, reconstructed.blackTimeSeconds)
        assertFalse(reconstructed.isTimerRunning)
    }

    @Test
    fun testCorruptedOrIllegalMoveSequenceRejected() {
        // Illegal move: Pawn on e2 tries to jump to h7
        val illegalMoves = listOf(
            SerializedMove(4, 1, 7, 6)
        )
        val snapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 1L,
            moveHistory = illegalMoves
        )

        val reconstructed = GameSnapshotSerializer.reconstructGameState(snapshot)
        assertNull(reconstructed)
    }

    @Test
    fun testOutdatedSchemaVersionRejected() {
        val raw = """{"schemaVersion": 999, "saveSequence": 1, "mode": "PVC"}"""
        val deserialized = GameSnapshotSerializer.deserialize(raw)
        assertNull(deserialized)
    }

    @Test
    fun testMalformedJsonRejected() {
        val deserialized = GameSnapshotSerializer.deserialize("not-valid-json")
        assertNull(deserialized)
    }

    @Test
    fun testMonotonicSequencePreventsStaleOverwrites() = runBlocking {
        val newerSnapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 10L,
            whiteTimeSeconds = 200
        )
        val staleSnapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 5L, // Stale!
            whiteTimeSeconds = 300
        )

        repository.saveActiveGame(newerSnapshot)
        // Stale write attempt
        repository.saveActiveGame(staleSnapshot)

        val loaded = repository.loadActiveGame()
        assertNotNull(loaded)
        assertEquals(10L, loaded!!.saveSequence)
        assertEquals(200, loaded.whiteTimeSeconds)
    }

    @Test
    fun testClearActiveGame() = runBlocking {
        val snapshot = ActiveGameSnapshot(
            schemaVersion = 1,
            saveSequence = 1L,
            whiteTimeSeconds = 500
        )
        repository.saveActiveGame(snapshot)
        assertNotNull(repository.loadActiveGame())

        repository.clearActiveGame()
        assertNull(repository.loadActiveGame())
    }

    @Test
    fun testFakeActiveGameRepository() = runBlocking {
        val fake = FakeActiveGameRepository()
        assertNull(fake.loadActiveGame())

        val snap1 = ActiveGameSnapshot(saveSequence = 1L, whiteTimeSeconds = 100)
        fake.saveActiveGame(snap1)
        assertEquals(100, fake.loadActiveGame()!!.whiteTimeSeconds)

        val older = ActiveGameSnapshot(saveSequence = 0L, whiteTimeSeconds = 500)
        fake.saveActiveGame(older)
        assertEquals(100, fake.loadActiveGame()!!.whiteTimeSeconds)

        fake.clearActiveGame()
        assertNull(fake.loadActiveGame())
    }
}
