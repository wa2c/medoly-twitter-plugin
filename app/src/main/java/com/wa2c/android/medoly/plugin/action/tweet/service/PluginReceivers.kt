package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.prefs.Prefs
import timber.log.Timber

/**
 * Plugin receiver.
 */
class PluginReceivers {

    abstract class AbstractPluginReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("onReceive: %s", this.javaClass.simpleName)
            val result = receive(context, MediaPluginIntent(intent))
            setResult(result.resultCode, null, null)
        }

        /**
         * Receive data.
         */
        private fun receive(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult  {
            var result =  PluginBroadcastResult.CANCEL

            val propertyData = pluginIntent.propertyData ?: return result
            val prefs = Prefs(context)

            if (this is EventPostTweetReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                    return result
                }
                // media
                if (propertyData.isMediaEmpty) {
                    AppUtils.showToast(context, R.string.message_no_media)
                    return result
                }
                // property
                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                    return result
                }
                // operation
                val operation = prefs.getString(R.string.prefkey_event_tweet_operation, defRes = R.string.pref_default_event_tweet_operation)
                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
                    return result
                }
                // previous media
                val mediaUriText = propertyData.mediaUri?.toString()
                val previousMediaUri = prefs.getStringOrNull(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI)
                val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
                if (!previousMediaEnabled && !mediaUriText.isNullOrEmpty() && !previousMediaUri.isNullOrEmpty() && mediaUriText == previousMediaUri) {
                    return result
                }

                // service
                pluginIntent.setClass(context, PluginPostService::class.java)
                result = PluginBroadcastResult.COMPLETE
            } else if (this is ExecutePostTweetReceiver || this is ExecuteOpenTwitterReceiver) {
                // category
                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_RUN)) {
                    return result
                }
                if (this is ExecutePostTweetReceiver) {
                    // media
                    if (propertyData.isMediaEmpty) {
                        AppUtils.showToast(context, R.string.message_no_media)
                        return result
                    }
                    // property
                    if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
                        return result
                    }
                }

                // service
                pluginIntent.setClass(context, PluginRunService::class.java)
                result = PluginBroadcastResult.COMPLETE
            }

            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
            ContextCompat.startForegroundService(context, pluginIntent)
            return result
        }
    }

    // Event

    class EventPostTweetReceiver : AbstractPluginReceiver()

    // Execution

    class ExecutePostTweetReceiver : AbstractPluginReceiver()

    class ExecuteOpenTwitterReceiver : AbstractPluginReceiver()

}
