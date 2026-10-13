package com.example.chess.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SharedPreferencesSettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = SharedPreferencesSettingsRepository(prefs)
    }

    @Test
    fun testDefaultValuesWhenSettingsMissing() {
        val settings = repository.loadSettings()
        assertEquals(GameMode.PVC, settings.mode)
        assertEquals(AIDifficulty.MEDIUM, settings.aiDifficulty)
        assertEquals(PieceColor.WHITE, settings.playerColor)
        assertEquals(BoardTheme.CLASSIC_WOOD, settings.boardTheme)
        assertEquals(PieceSet.CLASSIC_STAUNTON, settings.pieceSet)
        assertEquals(AnimationSpeed.NORMAL, settings.animationSpeed)
        assertTrue(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertFalse(settings.is3DView)
        assertFalse(settings.autoFlipBoard)
        assertEquals(TimeControl.UNLIMITED, settings.timeControl)
        assertTrue(settings.syncThemeEffects)
    }

    @Test
    fun testPersistingUpdatesAndLoading() {
        val updated = GameSettings(
            mode = GameMode.PVP,
            aiDifficulty = AIDifficulty.HARD,
            playerColor = PieceColor.BLACK,
            boardTheme = BoardTheme.NEON_CYBER,
            pieceSet = PieceSet.CYBER_MECHA,
            animationSpeed = AnimationSpeed.FAST,
            soundEnabled = false,
            hapticsEnabled = false,
            is3DView = true,
            autoFlipBoard = true,
            timeControl = TimeControl.BLITZ_5,
            syncThemeEffects = false
        )

        repository.updateSettings(updated)

        // Verify state flow is updated immediately
        assertEquals(updated, repository.settings.value)

        // Verify fresh read from SharedPreferences
        val freshLoaded = repository.loadSettings()
        assertEquals(GameMode.PVP, freshLoaded.mode)
        assertEquals(AIDifficulty.HARD, freshLoaded.aiDifficulty)
        assertEquals(PieceColor.BLACK, freshLoaded.playerColor)
        assertEquals(BoardTheme.NEON_CYBER, freshLoaded.boardTheme)
        assertEquals(PieceSet.CYBER_MECHA, freshLoaded.pieceSet)
        assertEquals(AnimationSpeed.FAST, freshLoaded.animationSpeed)
        assertFalse(freshLoaded.soundEnabled)
        assertFalse(freshLoaded.hapticsEnabled)
        assertTrue(freshLoaded.is3DView)
        assertTrue(freshLoaded.autoFlipBoard)
        assertEquals(TimeControl.BLITZ_5, freshLoaded.timeControl)
        assertFalse(freshLoaded.syncThemeEffects)
    }

    @Test
    fun testCompatibilityWithExistingPreferenceKeys() {
        val prefs = context.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)

        // Seed raw preference values using existing legacy keys
        prefs.edit()
            .putInt(SharedPreferencesSettingsRepository.KEY_THEME, BoardTheme.OCEAN.ordinal)
            .putInt(SharedPreferencesSettingsRepository.KEY_PIECE_SET, PieceSet.WOODEN_CARVED.ordinal)
            .putInt(SharedPreferencesSettingsRepository.KEY_MODE, GameMode.PVC.ordinal)
            .putInt(SharedPreferencesSettingsRepository.KEY_DIFFICULTY, AIDifficulty.HARD.ordinal)
            .putInt(SharedPreferencesSettingsRepository.KEY_SPEED, AnimationSpeed.SLOW.ordinal)
            .putBoolean(SharedPreferencesSettingsRepository.KEY_SOUND, false)
            .putBoolean(SharedPreferencesSettingsRepository.KEY_HAPTICS, true)
            .putBoolean(SharedPreferencesSettingsRepository.KEY_IS_3D, true)
            .putBoolean(SharedPreferencesSettingsRepository.KEY_SYNC_THEME_EFFECTS, false)
            .commit()

        val repo = SharedPreferencesSettingsRepository(prefs)
        val loaded = repo.loadSettings()

        assertEquals(BoardTheme.OCEAN, loaded.boardTheme)
        assertEquals(PieceSet.WOODEN_CARVED, loaded.pieceSet)
        assertEquals(AIDifficulty.HARD, loaded.aiDifficulty)
        assertEquals(AnimationSpeed.SLOW, loaded.animationSpeed)
        assertFalse(loaded.soundEnabled)
        assertTrue(loaded.hapticsEnabled)
        assertTrue(loaded.is3DView)
        assertFalse(loaded.syncThemeEffects)
    }
}
