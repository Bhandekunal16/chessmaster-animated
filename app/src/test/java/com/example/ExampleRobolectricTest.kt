package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.chess.model.GameSettings
import com.example.chess.ui.SettingsDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Grandmaster Chess", appName)
  }

  @Test
  fun `clicking settings button opens settings dialog`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.chess.viewmodel.ChessViewModel(context as android.app.Application)
    composeTestRule.setContent {
      com.example.chess.ui.ChessScreen(viewModel = vm)
    }
    // Initially settings dialog is not displayed
    composeTestRule.onNodeWithTag("settings_dialog").assertDoesNotExist()

    // Click top settings button
    composeTestRule.onNodeWithTag("top_settings_button").performClick()
    composeTestRule.waitForIdle()

    // Check if viewmodel state changed
    org.junit.Assert.assertTrue("showSettingsDialog state should be true", vm.showSettingsDialog.value)

    // Settings dialog must now exist and be displayed
    composeTestRule.onNodeWithTag("settings_dialog").assertIsDisplayed()

    // Click close settings button
    composeTestRule.onNodeWithTag("close_settings_button").performClick()
    composeTestRule.waitForIdle()

    org.junit.Assert.assertFalse("showSettingsDialog state should be false", vm.showSettingsDialog.value)
    composeTestRule.onNodeWithTag("settings_dialog").assertDoesNotExist()
  }
}
