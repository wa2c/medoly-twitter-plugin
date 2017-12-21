package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager

import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.plugin.action.tweet.R

/**
 * Execute receiver.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val serviceIntent = MediaPluginIntent(intent)
            serviceIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.getName())

            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            if (this is EventPostTweetReceiver) {
                try {
                    val operation = PluginOperationCategory.valueOf(pref.getString(context.getString(R.string.prefkey_event_tweet_operation), ""))
                    if (serviceIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || serviceIntent.hasCategory(operation)) {
                        serviceIntent.setClass(context, PluginPostService::class.java!!)
                    }
                } catch (ignore: Exception) {
                }

            } else if (this is ExecutePostTweetReceiver || this is ExecuteOpenTwitterReceiver) {
                serviceIntent.setClass(context, PluginRunService::class.java!!)
            }

            context.stopService(serviceIntent)
            context.startService(serviceIntent)
        }
    }

    // Event

    class EventPostTweetReceiver : AbstractPluginReceiver()

    // Execution

    class ExecutePostTweetReceiver : AbstractPluginReceiver()

    class ExecuteOpenTwitterReceiver : AbstractPluginReceiver()

}
