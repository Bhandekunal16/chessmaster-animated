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

    @Test
    fun testInvalidOrOutdatedStoredValues() {
        val prefs = context.getSharedPreferences(SharedPreferencesSettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)

        // Seed invalid / out of bounds ordinals
        prefs.edit()
            .putInt(SharedPreferencesSettingsRepository.KEY_THEME, 999) // Out of bounds
            .putInt(SharedPreferencesSettingsRepository.KEY_PIECE_SET, -1) // Negative
            .putInt(SharedPreferencesSettingsRepository.KEY_MODE, 50)
            .putInt(SharedPreferencesSettingsRepository.KEY_DIFFICULTY, 100)
            .putInt(SharedPreferencesSettingsRepository.KEY_SPEED, 25)
            .putInt(SharedPreferencesSettingsRepository.KEY_TIME_CONTROL, 99)
            .commit()

        val repo = SharedPreferencesSettingsRepository(prefs)
        val loaded = repo.loadSettings()

        // Should safely fallback to defaults
        assertEquals(BoardTheme.CLASSIC_WOOD, loaded.boardTheme)
        assertEquals(PieceSet.CLASSIC_STAUNTON, loaded.pieceSet)
        assertEquals(GameMode.PVC, loaded.mode)
        assertEquals(AIDifficulty.MEDIUM, loaded.aiDifficulty)
        assertEquals(AnimationSpeed.NORMAL, loaded.animationSpeed)
        assertEquals(TimeControl.UNLIMITED, loaded.timeControl)
    }

    @Test
    fun testResetToDefaults() {
        repository.updateSettings(
            GameSettings(
                mode = GameMode.PVP,
                aiDifficulty = AIDifficulty.HARD,
                soundEnabled = false
            )
        )
        assertFalse(repository.settings.value.soundEnabled)

        repository.resetToDefaults()

        assertEquals(GameSettings(), repository.settings.value)
        assertEquals(GameSettings(), repository.loadSettings())
    }

    @Test
    fun testFakeSettingsRepository() {
        val fakeRepo = FakeSettingsRepository()
        assertEquals(GameSettings(), fakeRepo.loadSettings())
        assertEquals(GameSettings(), fakeRepo.settings.value)

        val custom = GameSettings(
            boardTheme = BoardTheme.MIDNIGHT,
            soundEnabled = false
        )
        fakeRepo.updateSettings(custom)
        assertEquals(custom, fakeRepo.settings.value)
        assertEquals(custom, fakeRepo.loadSettings())

        fakeRepo.resetToDefaults()
        assertEquals(GameSettings(), fakeRepo.settings.value)
    }
}
