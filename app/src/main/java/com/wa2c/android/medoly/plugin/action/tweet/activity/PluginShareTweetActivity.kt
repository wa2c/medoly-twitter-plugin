package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wa2c.android.medoly.library.AlbumArtProperty
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginBroadcastResult
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.logE
import com.wa2c.android.medoly.plugin.action.tweet.util.toast
import kotlinx.coroutines.runBlocking

/**
 * Track page open activity
 */
class PluginShareTweetActivity : AppCompatActivity(R.layout.layout_loading) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        shareTweet(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        shareTweet(intent)
    }

    /**
     * Share tweet message.
     */
    private fun shareTweet(launchIntent: Intent?) {
        runBlocking {
            val result: PluginBroadcastResult = let result@{
                try {
                    if (launchIntent == null) {
                        setResult(PluginBroadcastResult.CANCEL.resultCode)
                        return@result PluginBroadcastResult.COMPLETE
                    }

                    val intent = MediaPluginIntent(intent)
                    val propertyData: PropertyData = intent.propertyData.let {
                        if (it == null || it.isEmpty()) {
                            toast(R.string.message_no_media)
                            setResult(PluginBroadcastResult.CANCEL.resultCode)
                            return@result PluginBroadcastResult.COMPLETE
                        }
                        it
                    }

                    // Get message
                    val message = TwitterUtils.getTweetMessage(this@PluginShareTweetActivity , propertyData)
                    if (message.isEmpty()) {
                        return@result PluginBroadcastResult.IGNORE
                    }

                    // Get album art
                    val albumArtUri = try {
                        propertyData.getFirst(AlbumArtProperty.SHARED_URI)?.let { Uri.parse(it) }
                    } catch (e: Exception) {
                        null
                    }

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
                        }
                    }

                    startActivity(twitterIntent)
                    PluginBroadcastResult.COMPLETE
                } catch (e: Exception) {
                    logE(e)
                    toast(R.string.message_post_failure)
                    PluginBroadcastResult.CANCEL
                }
            }

            setResult(result.resultCode)
        }

        moveTaskToBack(true)
        finish()
    }
}
