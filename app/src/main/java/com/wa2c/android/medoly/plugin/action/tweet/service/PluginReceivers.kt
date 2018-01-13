package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.MediaProperty
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PluginTypeCategory
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.Prefs

/**
 * Plugin receiver.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Logger.d("onReceive: " + this.javaClass.simpleName)

            val pluginIntent = MediaPluginIntent(intent)
            val propertyData = pluginIntent.propertyData ?: return
            val prefs = Prefs(context)

            if (this is EventPostTweetReceiver) {
                // checks
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return
                }
                val operation = try { PluginOperationCategory.valueOf(prefs.getString(R.string.prefkey_event_tweet_operation)) } catch (ignore : Exception) { null }
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    return
                }
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    return
                }
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return
                }

                // service
                pluginIntent.setClass(context, PluginPostService::class.java)
            } else if (this is ExecutePostTweetReceiver || this is ExecuteOpenTwitterReceiver) {
                // check
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
                    return
                }
                if (this is ExecutePostTweetReceiver) {
                    if (propertyData.isMediaEmpty) {
                        AppUtils.showToast(context, R.string.message_no_media)
                        return
                    }
                    if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                        return
                    }
                }

                // service
                pluginIntent.setClass(context, PluginRunService::class.java)
            }

            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
            context.stopService(pluginIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(pluginIntent)
            } else {
                context.startService(pluginIntent)
            }
        }
    }

    // Event

    class EventPostTweetReceiver : AbstractPluginReceiver()

    // Execution

    class ExecutePostTweetReceiver : AbstractPluginReceiver()

    class ExecuteOpenTwitterReceiver : AbstractPluginReceiver()

}
