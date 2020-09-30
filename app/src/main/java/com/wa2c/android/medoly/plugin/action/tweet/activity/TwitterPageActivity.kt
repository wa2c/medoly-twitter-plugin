package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wa2c.android.medoly.library.PluginBroadcastResult
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.logD
import com.wa2c.android.medoly.plugin.action.tweet.util.toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Track page open activity
 */
class TwitterPageActivity : AppCompatActivity(R.layout.layout_loading) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        openTwitterPage()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openTwitterPage()
    }

    /**
     * Open Last.fm page.
     */
    private fun openTwitterPage() {
        runBlocking {
            GlobalScope.launch {
                val result = try {
                    val uri = Uri.parse(getString(R.string.twitter_uri))
                    val launchIntent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(launchIntent)
                    PluginBroadcastResult.COMPLETE
                } catch (e: ActivityNotFoundException) {
                    logD(e)
                    toast(R.string.message_page_failure)
                    PluginBroadcastResult.CANCEL
                }

                setResult(result.resultCode)
                finish()
            }
        }
    }
}
