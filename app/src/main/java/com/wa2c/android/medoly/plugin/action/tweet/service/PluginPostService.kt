package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import timber.log.Timber
import twitter4j.StatusUpdate
import twitter4j.Twitter
import java.io.FileInputStream
import java.io.InputStream


/**
 * Post plugin service.
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
            Timber.e(e)
        }
    }


    /**
     * Tweet.
     */
    private fun tweet() {
        var result: CommandResult = CommandResult.IGNORE
        var inputStream: InputStream? = null
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
            Timber.e(e)
            result = CommandResult.FAILED
        } finally {
            // save previous media
            prefs.putString(PREFKEY_PREVIOUS_MEDIA_URI, propertyData.mediaUri.toString())

            if (inputStream != null)
                try {
                    inputStream.close()
                } catch (ignored: Exception) {
                }

            val succeeded = getString(R.string.message_post_success)
            val failed = getString(R.string.message_post_failure)
            showMessage(result, succeeded, failed)
        }
    }

}
