package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.text.TextUtils

import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger


/**
 * Download intent service.
 */
/**
 * Constructor.
 */
class PluginRunService : AbstractPluginService(PluginRunService::class.java!!.getSimpleName()) {

    override fun onHandleIntent(intent: Intent?) {
        super.onHandleIntent(intent)

        try {
            if (receivedClassName == PluginReceivers.ExecutePostTweetReceiver::class.java!!.getName()) {
                shareTweet()
            } else if (receivedClassName == PluginReceivers.ExecuteOpenTwitterReceiver::class.java!!.getName()) {
                openTwitter()
            }
        } catch (e: Exception) {
            Logger.e(e)
        }

    }


    /**
     * Share tweet message.
     */
    private fun shareTweet() {
        var result: AbstractPluginService.CommandResult = AbstractPluginService.CommandResult.IGNORE
        try {
            if (propertyData == null || propertyData.isMediaEmpty) {
                result = AbstractPluginService.CommandResult.NO_MEDIA
                return
            }

            // Get message
            val message = tweetMessage
            if (TextUtils.isEmpty(message)) {
                result = AbstractPluginService.CommandResult.IGNORE
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
                        context!!.applicationContext.grantUriPermission(packageName, albumArtUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }

            context!!.startActivity(twitterIntent)
            result = AbstractPluginService.CommandResult.SUCCEEDED
        } catch (e: Exception) {
            Logger.e(e)
            result = AbstractPluginService.CommandResult.FAILED
        } finally {
            if (result == AbstractPluginService.CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media)
            } else if (result == AbstractPluginService.CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_post_failure)
            }
        }
    }

    /**
     * Open twitter.
     */
    private fun openTwitter() {
        // Twitter.com
        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(context!!.getString(R.string.twitter_uri)))
        try {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context!!.startActivity(launchIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            Logger.d(e)
        }

    }

}
