package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import timber.log.Timber

/**
 * Run plugin service.
 */
class PluginRunService : AbstractPluginService(PluginRunService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        try {
            if (receivedClassName == PluginReceivers.ExecutePostTweetReceiver::class.java.name) {
                shareTweet()
            } else if (receivedClassName == PluginReceivers.ExecuteOpenTwitterReceiver::class.java.name) {
                openTwitter()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    /**
     * Share tweet message.
     */
    private fun shareTweet() {
        var result: CommandResult = CommandResult.IGNORE
        try {
            // Get message
            val message = TwitterUtils.getTweetMessage(context, propertyData)
            if (message.isEmpty()) {
                result = CommandResult.IGNORE
                return
            }

            // Get album art
            val albumArtUri = propertyData.albumArtUri

            val twitterIntent = Intent()
            twitterIntent.action = Intent.ACTION_SEND
            twitterIntent.type = "text/plain"
            twitterIntent.putExtra(Intent.EXTRA_TEXT, message)
            twitterIntent.putExtra(Intent.EXTRA_STREAM, albumArtUri)
            twitterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // add URI permission
            if (albumArtUri != null) {
                // content:// scheme needs permission.
                if (ContentResolver.SCHEME_CONTENT == albumArtUri.scheme) {
                    twitterIntent.data = albumArtUri
                    twitterIntent.type = propertyData.getFirst(AlbumArtProperty.MIME_TYPE)
                    twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val resolveInfoList = packageManager.queryIntentActivities(twitterIntent, PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER)
                    for (resolveInfo in resolveInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        context.applicationContext.grantUriPermission(packageName, albumArtUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }

            context.startActivity(twitterIntent)
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Timber.e(e)
            result = CommandResult.FAILED
        } finally {
            val failed = getString(R.string.message_post_failure)
            showMessage(result, null, failed)
        }
    }

    /**
     * Open twitter.
     */
    private fun openTwitter() {
        var result = CommandResult.IGNORE
        try {
            val siteUri = Uri.parse(context.getString(R.string.twitter_uri))
            startPage(siteUri)
            result = CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Timber.d(e)
            result = CommandResult.FAILED
        } finally {
            showMessage(result, null, getString(R.string.message_page_failure))
        }
    }

    /**
     * Start page.
     * @param uri The URI.
     */
    private fun startPage(uri: Uri?) {
        if (uri == null)
            return
        val launchIntent = Intent(Intent.ACTION_VIEW, uri)
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(launchIntent)
    }
}
