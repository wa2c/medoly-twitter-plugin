package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Context
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.*
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import twitter4j.StatusUpdate
import twitter4j.Twitter
import java.io.FileInputStream

/**
 * Love worker.
 */
class PluginPostTweetWorker(private val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    /** Prefs */
    private val prefs: Prefs by lazy { Prefs(context) }
    /** Twitter。  */
    private val twitter: Twitter by lazy { getTwitterInstance(context)!! }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        return createForegroundFuture(context)
    }

    override fun doWork(): Result {
        val result = runBlocking {
            try {
                tweet()
                CommandResult.SUCCEEDED
            } catch (e: Exception) {
                logE(e)
                CommandResult.FAILED
            }
        }

        val succeeded = context.getString(R.string.message_post_success)
        val failed = context.getString(R.string.message_post_failure)
        showMessage(prefs, result, succeeded, failed, params.isAutomaticallyAction)
        return Result.success()
    }

    /**
     * Tweet.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun tweet(): CommandResult {
        return withContext(Dispatchers.Default) {
            if (!hasAccessToken(context)) {
                return@withContext CommandResult.AUTH_FAILED
            }

            // Get message
            val map = params.inputData.keyValueMap.map { it.key to it.value.toString() }.toMap()
            val message = getTweetMessage(context, map )
            if (message.isEmpty()) {
                return@withContext CommandResult.IGNORE
            }

            // Get album art uri
            val albumArtUri = if (prefs.getBoolean(R.string.prefkey_send_album_art, true)) {
                params.inputData.getString(AlbumArtProperty.SHARED_URI.keyName)?.toUri()
            } else {
                null
            }

            when (albumArtUri?.scheme) {
                ContentResolver.SCHEME_CONTENT -> context.contentResolver.openInputStream(albumArtUri)
                ContentResolver.SCHEME_FILE -> FileInputStream(albumArtUri.toFile())
                else -> null
            }.use { stream ->
                if (stream != null) {
                    twitter.updateStatus(StatusUpdate(message).media(albumArtUri?.lastPathSegment, stream))
                } else {
                    twitter.updateStatus(StatusUpdate(message))
                }
            }

            prefs.putString(PREFKEY_PREVIOUS_MEDIA_URI, params.inputData.getString(MediaProperty.DATA_URI.keyName))
            CommandResult.SUCCEEDED
        }
    }

    companion object {
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }
}