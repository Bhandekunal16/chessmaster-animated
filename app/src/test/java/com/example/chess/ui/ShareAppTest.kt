package com.example.chess.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.chess.viewmodel.ChessViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.chess.model.GameSettings
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShareAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBuildShareMessageContainsRequiredElements() {
        val message = ShareAppHelper.buildShareMessage()
        
        // Must contain app name
        assertTrue("Message must contain app name", message.contains("Grandmaster Chess"))
        // Must contain chess emoji
        assertTrue("Message must contain chess emoji", message.contains("♟️"))
        // Must contain promotional description
        assertTrue("Message must contain promotional text", message.contains("Challenge yourself"))
        assertTrue("Message must contain promotional text", message.contains("improve your chess skills"))
        assertTrue("Message must contain promotional text", message.contains("enjoy playing chess against AI"))
        // Must contain Play Store download URL
        assertTrue("Message must contain download URL", message.contains(ShareAppHelper.DEFAULT_DOWNLOAD_URL))
    }

    @Test
    fun testBuildShareMessageWithCustomDownloadUrl() {
        val customUrl = "https://example.com/download/chess"
        val message = ShareAppHelper.buildShareMessage(customUrl)
        
        assertTrue("Message must contain custom download URL", message.contains(customUrl))
        assertTrue("Message must contain Grandmaster Chess", message.contains("Grandmaster Chess"))
    }

    @Test
    fun testCreateShareIntentProperties() {
        val message = ShareAppHelper.buildShareMessage()
        val intent = ShareAppHelper.createShareIntent(message)

        assertEquals("Intent action must be ACTION_SEND", Intent.ACTION_SEND, intent.action)
        assertEquals("MIME type must be text/plain", "text/plain", intent.type)
        assertEquals("Extra subject must be set", ShareAppHelper.SHARE_SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Extra text must match promotional message", message, intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun testCreateChooserIntentProperties() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sendIntent = ShareAppHelper.createShareIntent("test message")
        val chooser = ShareAppHelper.createChooserIntent(sendIntent, "Share Grandmaster Chess", context)

        assertEquals("Intent action must be ACTION_CHOOSER", Intent.ACTION_CHOOSER, chooser.action)
        val wrappedIntent = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Chooser must wrap the send intent", wrappedIntent)
        assertEquals("Wrapped intent action must be ACTION_SEND", Intent.ACTION_SEND, wrappedIntent?.action)
        assertEquals("Wrapped intent text must be test message", "test message", wrappedIntent?.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("Chooser title must match", "Share Grandmaster Chess", chooser.getStringExtra(Intent.EXTRA_TITLE))
        assertTrue("FLAG_ACTIVITY_NEW_TASK should be added for non-activity context", (chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun testShareAppLaunchesChooserActivity() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        ShareAppHelper.shareApp(context)

        val shadowApp = shadowOf(context)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("An activity must be started when shareApp is called", startedIntent)
        assertEquals("Started intent must be ACTION_CHOOSER", Intent.ACTION_CHOOSER, startedIntent.action)

        val targetIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Chooser intent must wrap ACTION_SEND target", targetIntent)
        assertEquals("Target intent must be ACTION_SEND", Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("Target MIME type must be text/plain", "text/plain", targetIntent?.type)
        val text = targetIntent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        assertTrue("Shared text must contain Grandmaster Chess", text.contains("Grandmaster Chess"))
        assertTrue("Shared text must contain download link", text.contains("play.google.com"))
    }

    @Test
    fun testViewModelShareAppDelegation() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ChessViewModel(context)
        viewModel.shareApp(context)

        val shadowApp = shadowOf(context)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("ChessViewModel.shareApp must launch chooser intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
    }

    @Test
    fun testTopShareButtonInChessScreenTriggersShare() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ChessViewModel(context)

        composeTestRule.setContent {
            ChessScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("top_share_app_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        val shadowApp = shadowOf(context)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Clicking top share button must trigger chooser intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
    }

    @Test
    fun testSettingsHeaderShareButtonTriggersShare() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val settings = GameSettings()

        composeTestRule.setContent {
            SettingsDialog(
                currentSettings = settings,
                onSave = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag("settings_header_share_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        val shadowApp = shadowOf(context)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Clicking settings header share button must trigger chooser intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
    }

    @Test
    fun testSettingsShareAppCardTriggersShare() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val settings = GameSettings()

        composeTestRule.setContent {
            SettingsDialog(
                currentSettings = settings,
                onSave = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag("settings_lazy_column", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("settings_share_app_button"))
        composeTestRule.onNodeWithTag("settings_share_app_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        val shadowApp = shadowOf(context)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("Clicking settings share button must trigger chooser intent", startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
    }
}
