package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Intent
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.logE
import twitter4j.StatusUpdate
import twitter4j.Twitter
import java.io.FileInputStream


/**
 * Post plugin service.
 */
class PluginPostService : AbstractPluginService(PluginPostService::class.java.simpleName) {
    /** Twitter。  */
    private var twitter: Twitter? = null

    override fun onHandleIntent(intent: Intent?) {
        try {
            super.onHandleIntent(intent)

            if (receivedClassName == PluginReceivers.EventPostTweetReceiver::class.java.name) {
                twitter = TwitterUtils.getTwitterInstance(context)
                tweet()
            }
        } catch (e: Exception) {
            logE(e)
        }
    }


    /**
     * Tweet.
     */
    private fun tweet() {
        var result: CommandResult = CommandResult.IGNORE
        try {
            if (!TwitterUtils.hasAccessToken(context)) {
                result = CommandResult.AUTH_FAILED
                return
            }

            // Get message
            val message = TwitterUtils.getTweetMessage(context, propertyData)
            if (message.isEmpty()) {
                result = CommandResult.IGNORE
                return
            }

            // Get album art uri
            if (prefs.getBoolean(R.string.prefkey_send_album_art, true)) {
                propertyData.albumArtUri
            } else {
                null
            }.let { albumArtUri ->
                if (albumArtUri?.scheme == ContentResolver.SCHEME_CONTENT) {
                    context.contentResolver.openInputStream(albumArtUri)
                } else if (albumArtUri?.scheme == ContentResolver.SCHEME_FILE) {
                    albumArtUri.path?.let { FileInputStream(it) }
                } else {
                    null
                }?.use { stream ->
                    twitter?.updateStatus(StatusUpdate(message).media(albumArtUri?.lastPathSegment, stream))
                } ?: run {
                    twitter?.updateStatus(StatusUpdate(message))
                }
            }
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {

            result = CommandResult.FAILED
        } finally {
            // save previous media
            prefs.putString(PREFKEY_PREVIOUS_MEDIA_URI, propertyData.mediaUri.toString())
            val succeeded = getString(R.string.message_post_success)
            val failed = getString(R.string.message_post_failure)
            showMessage(result, succeeded, failed)
        }
    }

}
