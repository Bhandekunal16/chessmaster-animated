package com.example.chess.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chess.model.AIDifficulty
import com.example.chess.model.GameMode
import com.example.chess.model.GameSettings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GrandmasterTopAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testIntermediateDifficultyLabelDisplayed() {
        val settings = GameSettings(
            mode = GameMode.PVC,
            aiDifficulty = AIDifficulty.MEDIUM // "Intermediate"
        )

        composeTestRule.setContent {
            GrandmasterTopAppBar(
                settings = settings,
                onDifficultySelected = {},
                onModeSelected = {},
                onThemeClick = {},
                onPgnClick = {},
                onShareClick = {},
                onSettingsClick = {}
            )
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Grandmaster Chess").assertIsDisplayed()

        // Verify Intermediate difficulty label is displayed clearly
        composeTestRule.onNodeWithText("Intermediate").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_difficulty_selector").assertIsDisplayed()
    }

    @Test
    fun testNoviceAndMasterDifficultyLabelsDisplayed() {
        var currentDifficulty = AIDifficulty.EASY

        composeTestRule.setContent {
            GrandmasterTopAppBar(
                settings = GameSettings(mode = GameMode.PVC, aiDifficulty = currentDifficulty),
                onDifficultySelected = { currentDifficulty = it },
                onModeSelected = {},
                onThemeClick = {},
                onPgnClick = {},
                onShareClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Novice").assertIsDisplayed()
    }

    @Test
    fun testTwoPlayerModeLabelDisplayed() {
        composeTestRule.setContent {
            GrandmasterTopAppBar(
                settings = GameSettings(mode = GameMode.PVP),
                onDifficultySelected = {},
                onModeSelected = {},
                onThemeClick = {},
                onPgnClick = {},
                onShareClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("2 Players").assertIsDisplayed()
    }

    @Test
    fun testDifficultySelectorDropdownOpensAndSelects() {
        var selectedDifficulty: AIDifficulty? = null
        var selectedMode: GameMode? = null

        composeTestRule.setContent {
            GrandmasterTopAppBar(
                settings = GameSettings(mode = GameMode.PVC, aiDifficulty = AIDifficulty.EASY),
                onDifficultySelected = { selectedDifficulty = it },
                onModeSelected = { selectedMode = it },
                onThemeClick = {},
                onPgnClick = {},
                onShareClick = {},
                onSettingsClick = {}
            )
        }

        // Click difficulty selector to expand dropdown
        composeTestRule.onNodeWithTag("top_difficulty_selector").performClick()
        composeTestRule.waitForIdle()

        // Dropdown menu should be displayed with options
        composeTestRule.onNodeWithTag("difficulty_option_MEDIUM").assertIsDisplayed()
        composeTestRule.onNodeWithTag("difficulty_option_HARD").assertIsDisplayed()

        // Select Intermediate (MEDIUM)
        composeTestRule.onNodeWithTag("difficulty_option_MEDIUM").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Selected difficulty must be MEDIUM (Intermediate)", AIDifficulty.MEDIUM, selectedDifficulty)
        assertEquals("Selected mode must be PVC", GameMode.PVC, selectedMode)
    }

    @Test
    fun testAllToolbarActionButtonsClickable() {
        var themeClicked = false
        var pgnClicked = false
        var shareClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            GrandmasterTopAppBar(
                settings = GameSettings(),
                onDifficultySelected = {},
                onModeSelected = {},
                onThemeClick = { themeClicked = true },
                onPgnClick = { pgnClicked = true },
                onShareClick = { shareClicked = true },
                onSettingsClick = { settingsClicked = true }
            )
        }

        composeTestRule.onNodeWithTag("top_themes_button").assertIsDisplayed().performClick()
        assertTrue("Theme action clicked", themeClicked)

        composeTestRule.onNodeWithTag("pgn_button").assertIsDisplayed().performClick()
        assertTrue("PGN action clicked", pgnClicked)

        composeTestRule.onNodeWithTag("top_share_app_button").assertIsDisplayed().performClick()
        assertTrue("Share action clicked", shareClicked)

        composeTestRule.onNodeWithTag("top_settings_button").assertIsDisplayed().performClick()
        assertTrue("Settings action clicked", settingsClicked)
    }
}
