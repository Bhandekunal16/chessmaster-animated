package com.example.chess.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.engine.GameController
import com.example.chess.model.*
import com.example.chess.repository.SharedPreferencesSettingsRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChessViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: ChessViewModel
    private lateinit var settingsRepo: SharedPreferencesSettingsRepository
    private lateinit var gameController: GameController

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val prefs = app.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        settingsRepo = SharedPreferencesSettingsRepository(prefs)
        gameController = GameController()
        viewModel = ChessViewModel(
            application = app,
            settingsRepository = settingsRepo,
            gameController = gameController
        )
    }

    @Test
    fun testSquareSelectionAndLegalMoves() {
        assertNull(viewModel.selectedSquare.value)
        assertTrue(viewModel.legalMovesForSelected.value.isEmpty())

        // Click White e2 pawn
        val e2 = Square.fromAlgebraic("e2")!!
        viewModel.onSquareClicked(e2)

        assertEquals(e2, viewModel.selectedSquare.value)
        val legalMoves = viewModel.legalMovesForSelected.value
        assertEquals(2, legalMoves.size) // e3 and e4
        assertTrue(legalMoves.any { it.to == Square.fromAlgebraic("e3") })
        assertTrue(legalMoves.any { it.to == Square.fromAlgebraic("e4") })

        // Click empty square or another piece
        val e5 = Square.fromAlgebraic("e5")!!
        viewModel.onSquareClicked(e5) // Not a legal move from e2 (e4 and e3 are legal)
        assertNull(viewModel.selectedSquare.value)
        assertTrue(viewModel.legalMovesForSelected.value.isEmpty())
    }

    @Test
    fun testDialogStateTransitions() {
        // Settings dialog
        assertFalse(viewModel.showSettingsDialog.value)
        viewModel.openSettings()
        assertTrue(viewModel.showSettingsDialog.value)
        viewModel.closeSettings()
        assertFalse(viewModel.showSettingsDialog.value)

        // Theme dialog
        assertFalse(viewModel.showThemeDialog.value)
        viewModel.openThemeDialog()
        assertTrue(viewModel.showThemeDialog.value)
        viewModel.closeThemeDialog()
        assertFalse(viewModel.showThemeDialog.value)

        // PGN dialog
        assertFalse(viewModel.showPgnDialog.value)
        viewModel.openPgn()
        assertTrue(viewModel.showPgnDialog.value)
        viewModel.closePgn()
        assertFalse(viewModel.showPgnDialog.value)

        // Board flip toggle
        assertFalse(viewModel.isFlipped.value)
        viewModel.toggleFlip()
        assertTrue(viewModel.isFlipped.value)
        viewModel.toggleFlip()
        assertFalse(viewModel.isFlipped.value)
    }

    @Test
    fun testSettingsPropagationAndDelegation() {
        val updated = viewModel.settings.value.copy(
            boardTheme = BoardTheme.NEON_CYBER,
            pieceSet = PieceSet.CYBER_MECHA,
            animationSpeed = AnimationSpeed.FAST
        )

        viewModel.updateSettings(updated)

        // Verify propagated through ViewModel and saved in repository
        assertEquals(BoardTheme.NEON_CYBER, viewModel.settings.value.boardTheme)
        assertEquals(PieceSet.CYBER_MECHA, viewModel.settings.value.pieceSet)
        assertEquals(AnimationSpeed.FAST, viewModel.settings.value.animationSpeed)
        assertEquals(BoardTheme.NEON_CYBER, settingsRepo.settings.value.boardTheme)

        // Test reset to default theme
        viewModel.resetToDefaultTheme()
        assertEquals(BoardTheme.CLASSIC_WOOD, viewModel.settings.value.boardTheme)
        assertEquals(PieceSet.CLASSIC_STAUNTON, viewModel.settings.value.pieceSet)
    }

    @Test
    fun testGameRestartClearsPresentationState() {
        // Select square
        viewModel.onSquareClicked(Square.fromAlgebraic("e2")!!)
        assertNotNull(viewModel.selectedSquare.value)

        viewModel.restartGame()

        assertNull(viewModel.selectedSquare.value)
        assertTrue(viewModel.legalMovesForSelected.value.isEmpty())
        assertNull(viewModel.animatedPiece.value)
        assertFalse(viewModel.showGameOverDialog.value)
        assertEquals(0, viewModel.gameState.value.moveHistory.size)
    }

    @Test
    fun testMaterialAdvantageAndPgnDelegation() {
        val advantage = viewModel.calculateMaterialAdvantage(PieceColor.WHITE)
        assertEquals(0, advantage)

        val pgn = viewModel.getPgnText()
        assertNotNull(pgn)
        assertTrue(pgn.contains("[Event \"Casual Match\"]"))
    }

    @Test
    fun testAndroidViewModelFactoryCreation() {
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val createdVm = factory.create(ChessViewModel::class.java)
        assertNotNull(createdVm)
        assertNotNull(createdVm.gameState.value)
        assertNotNull(createdVm.settings.value)
    }

    @Test
    fun testSingleArgConstructorInstantiation() {
        val createdVm = ChessViewModel(app)
        assertNotNull(createdVm)
        assertNotNull(createdVm.gameState.value)
    }
}
