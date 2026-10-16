package com.example.chess.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Helper object providing Android native Sharesheet integration for Grandmaster Chess.
 *
 * Uses implicit [Intent.ACTION_SEND] with [Intent.createChooser] so that users can share
 * promotional app download links across any compatible installed application (WhatsApp,
 * Telegram, Gmail, Messages, Bluetooth, Nearby Share / Quick Share, etc.).
 */
object ShareAppHelper {

    const val APP_NAME = "Grandmaster Chess"
    const val DEFAULT_DOWNLOAD_URL = "https://play.google.com/store/apps/details?id=com.aistudio.chessgame.rkzvpx"
    const val SHARE_SUBJECT = "Play Grandmaster Chess"
    const val SHARE_CHOOSER_TITLE = "Share Grandmaster Chess via"
    const val SHARE_MIME_TYPE = "text/plain"

    /**
     * Builds the promotional share message with the app name, description, and download URL.
     */
    fun buildShareMessage(
        downloadUrl: String = DEFAULT_DOWNLOAD_URL
    ): String {
        return "♟️ Play $APP_NAME! Challenge yourself, improve your chess skills, and enjoy playing chess against AI. Download the app: $downloadUrl"
    }

    /**
     * Creates an implicit [Intent.ACTION_SEND] configured for plain text sharing.
     */
    fun createShareIntent(
        message: String,
        subject: String = SHARE_SUBJECT
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = SHARE_MIME_TYPE
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }
    }

    /**
     * Creates a native system chooser intent wrapping the send intent.
     */
    fun createChooserIntent(
        sendIntent: Intent,
        chooserTitle: String = SHARE_CHOOSER_TITLE,
        context: Context? = null
    ): Intent {
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            if (context != null && context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Launches the native Android Sharesheet with all compatible sharing targets.
     * Safely catches [ActivityNotFoundException] without crashing.
     */
    fun shareApp(
        context: Context,
        downloadUrl: String = DEFAULT_DOWNLOAD_URL
    ) {
        try {
            val message = buildShareMessage(downloadUrl)
            val sendIntent = createShareIntent(message)
            val chooser = createChooserIntent(sendIntent, SHARE_CHOOSER_TITLE, context)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No compatible sharing apps found on device",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open sharing menu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
