package com.example.chess.repository

import android.content.SharedPreferences
import com.example.chess.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for managing and persisting chess game settings.
 */
interface SettingsRepository {
    val settings: StateFlow<GameSettings>
    fun loadSettings(): GameSettings
    fun updateSettings(newSettings: GameSettings)
    fun resetToDefaults()
}

/**
 * SharedPreferences-backed implementation of [SettingsRepository].
 * Preserves existing preference keys and schema for backward compatibility.
 */
class SharedPreferencesSettingsRepository(
    private val prefs: SharedPreferences
) : SettingsRepository {

    companion object {
        const val PREFS_NAME = "grandmaster_chess_prefs"
        const val KEY_THEME = "theme"
        const val KEY_PIECE_SET = "piece_set"
        const val KEY_MODE = "mode"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SPEED = "speed"
        const val KEY_SOUND = "sound"
        const val KEY_HAPTICS = "haptics"
        const val KEY_IS_3D = "is3D"
        const val KEY_SYNC_THEME_EFFECTS = "sync_theme_effects"
        const val KEY_AUTO_FLIP = "auto_flip"
        const val KEY_TIME_CONTROL = "time_control"
        const val KEY_PLAYER_COLOR = "player_color"
    }

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    override fun loadSettings(): GameSettings {
        return runCatching {
            val themeOrdinal = prefs.getInt(KEY_THEME, BoardTheme.CLASSIC_WOOD.ordinal)
            val pieceSetOrdinal = prefs.getInt(KEY_PIECE_SET, PieceSet.CLASSIC_STAUNTON.ordinal)
            val modeOrdinal = prefs.getInt(KEY_MODE, GameMode.PVC.ordinal)
            val diffOrdinal = prefs.getInt(KEY_DIFFICULTY, AIDifficulty.MEDIUM.ordinal)
            val speedOrdinal = prefs.getInt(KEY_SPEED, AnimationSpeed.NORMAL.ordinal)
            val sound = prefs.getBoolean(KEY_SOUND, true)
            val haptics = prefs.getBoolean(KEY_HAPTICS, true)
            val is3D = prefs.getBoolean(KEY_IS_3D, false)
            val syncEffects = prefs.getBoolean(KEY_SYNC_THEME_EFFECTS, true)
            val autoFlip = prefs.getBoolean(KEY_AUTO_FLIP, false)
            val tcOrdinal = prefs.getInt(KEY_TIME_CONTROL, TimeControl.UNLIMITED.ordinal)
            val playerColorOrdinal = prefs.getInt(KEY_PLAYER_COLOR, PieceColor.WHITE.ordinal)

            GameSettings(
                mode = GameMode.entries.getOrElse(modeOrdinal) { GameMode.PVC },
                aiDifficulty = AIDifficulty.entries.getOrElse(diffOrdinal) { AIDifficulty.MEDIUM },
                playerColor = PieceColor.entries.getOrElse(playerColorOrdinal) { PieceColor.WHITE },
                boardTheme = BoardTheme.entries.getOrElse(themeOrdinal) { BoardTheme.CLASSIC_WOOD },
                pieceSet = PieceSet.entries.getOrElse(pieceSetOrdinal) { PieceSet.CLASSIC_STAUNTON },
                animationSpeed = AnimationSpeed.entries.getOrElse(speedOrdinal) { AnimationSpeed.NORMAL },
                soundEnabled = sound,
                hapticsEnabled = haptics,
                is3DView = is3D,
                autoFlipBoard = autoFlip,
                timeControl = TimeControl.entries.getOrElse(tcOrdinal) { TimeControl.UNLIMITED },
                syncThemeEffects = syncEffects
            )
        }.getOrElse {
            GameSettings()
        }
    }

    override fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putInt(KEY_THEME, newSettings.boardTheme.ordinal)
            .putInt(KEY_PIECE_SET, newSettings.pieceSet.ordinal)
            .putInt(KEY_MODE, newSettings.mode.ordinal)
            .putInt(KEY_DIFFICULTY, newSettings.aiDifficulty.ordinal)
            .putInt(KEY_SPEED, newSettings.animationSpeed.ordinal)
            .putBoolean(KEY_SOUND, newSettings.soundEnabled)
            .putBoolean(KEY_HAPTICS, newSettings.hapticsEnabled)
            .putBoolean(KEY_IS_3D, newSettings.is3DView)
            .putBoolean(KEY_SYNC_THEME_EFFECTS, newSettings.syncThemeEffects)
            .putBoolean(KEY_AUTO_FLIP, newSettings.autoFlipBoard)
            .putInt(KEY_TIME_CONTROL, newSettings.timeControl.ordinal)
            .putInt(KEY_PLAYER_COLOR, newSettings.playerColor.ordinal)
            .apply()
    }

    override fun resetToDefaults() {
        updateSettings(GameSettings())
    }
}

/**
 * In-memory test fake implementation of [SettingsRepository].
 */
class FakeSettingsRepository(
    initialSettings: GameSettings = GameSettings()
) : SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    override fun loadSettings(): GameSettings = _settings.value

    override fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
    }

    override fun resetToDefaults() {
        _settings.value = GameSettings()
    }
}
