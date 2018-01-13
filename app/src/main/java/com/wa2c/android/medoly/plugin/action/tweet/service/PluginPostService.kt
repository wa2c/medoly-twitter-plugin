package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import twitter4j.StatusUpdate
import twitter4j.Twitter
import java.io.FileInputStream
import java.io.InputStream


/**
 * Intent service
 */
class PluginPostService : AbstractPluginService(PluginPostService::class.java.simpleName) {
    /** Twitter。  */
    private var twitter: Twitter? = null

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        try {
            twitter = TwitterUtils.getTwitterInstance(context)

            if (receivedClassName == PluginReceivers.EventPostTweetReceiver::class.java.name) {
                tweet()
            }
        } catch (e: Exception) {
            Logger.e(e)
        }
    }


    /**
     * Tweet.
     */
    private fun tweet() {
        var result: CommandResult = CommandResult.IGNORE
        var inputStream: InputStream? = null
        try {
            // Check previous media
            val mediaUriText = propertyData.mediaUri.toString()
            val previousMediaUri = prefs.getString(PREFKEY_PREVIOUS_MEDIA_URI)
            val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled)
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText == previousMediaUri) {
                result = CommandResult.IGNORE
                return
            }
            prefs.putValue(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText)

            if (!TwitterUtils.hasAccessToken(context)) {
                result = CommandResult.AUTH_FAILED
                return
            }

            // Get message
            val message = tweetMessage
            if (TextUtils.isEmpty(message)) {
                result = CommandResult.IGNORE
                return
            }

            // Get album art uri
            var albumArtUri: Uri? = null
            if (prefs.getBoolean(R.string.prefkey_send_album_art, true)) {
                albumArtUri = propertyData.albumArtUri
                if (albumArtUri != null) {
                    try {
                        if (ContentResolver.SCHEME_CONTENT == albumArtUri.scheme) {
                            inputStream = context.contentResolver.openInputStream(albumArtUri)
                        } else if (ContentResolver.SCHEME_FILE == albumArtUri.scheme) {
                            inputStream = FileInputStream(albumArtUri.path)
                        }
                    } catch (ignored: Exception) {
                        albumArtUri = null
                        inputStream = null
                    }

                }
            }

            if (inputStream == null) {
                twitter!!.updateStatus(StatusUpdate(message))
            } else {
                twitter!!.updateStatus(StatusUpdate(message).media(albumArtUri!!.lastPathSegment, inputStream))
            }
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            result = CommandResult.FAILED
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close()
                } catch (ignored: Exception) {
                }

            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth)
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == CommandResult.SUCCEEDED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_tweet_success_message_show))
                    AppUtils.showToast(context, R.string.message_post_success)
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_tweet_failure_message_show, true))
                    AppUtils.showToast(context, R.string.message_post_failure)
            }
        }
    }

    companion object {

        /** Previous data key.  */
        private val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }

}
