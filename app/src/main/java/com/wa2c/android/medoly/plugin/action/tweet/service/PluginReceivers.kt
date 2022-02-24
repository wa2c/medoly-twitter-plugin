package com.wa2c.android.medoly.plugin.action.tweet.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.logD
import com.wa2c.android.medoly.plugin.action.tweet.util.toWorkParams
import com.wa2c.android.medoly.plugin.action.tweet.util.toast
import com.wa2c.android.prefs.Prefs


abstract class AbstractPluginReceiver : BroadcastReceiver() {

    lateinit var prefs: Prefs

    override fun onReceive(context: Context, intent: Intent) {
        logD("onReceive: %s", this.javaClass.simpleName)
        prefs = Prefs(context)
        val pluginIntent = MediaPluginIntent(intent)
        val result = runPlugin(context, pluginIntent)
        setResult(result.resultCode, null, null)
    }

    abstract fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult

    /**
     * Validate property data.
     */
    protected fun validatePropertyData(context: Context, pluginIntent: MediaPluginIntent): PropertyData? {
        val propertyData = pluginIntent.propertyData

        // media
        if (propertyData == null || propertyData.isMediaEmpty) {
            context.toast(R.string.message_no_media)
            return null
        }
        // property
        if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
            return null
        }

        return propertyData
    }

    /**
     * Launch worker.
     */
    protected inline fun <reified T : Worker> launchWorker(context: Context, params: Data) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val request = OneTimeWorkRequestBuilder<T>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(params)
            .build()
        workManager.enqueue(request)
    }
}

// Event

class EventPostTweetReceiver : AbstractPluginReceiver() {
    override fun runPlugin(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult {
        val propertyData = validatePropertyData(context, pluginIntent) ?: return PluginBroadcastResult.CANCEL

        // operation
        val operation = prefs.getString(R.string.prefkey_event_tweet_operation, defRes = R.string.pref_default_event_tweet_operation)
        if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
            return PluginBroadcastResult.CANCEL
        }
        // previous media
        val mediaUriText = propertyData.mediaUri?.toString()
        val previousMediaUri = prefs.getStringOrNull(PluginPostTweetWorker.PREFKEY_PREVIOUS_MEDIA_URI)
        val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
        if (!previousMediaEnabled && !mediaUriText.isNullOrEmpty() && !previousMediaUri.isNullOrEmpty() && mediaUriText == previousMediaUri) {
            return PluginBroadcastResult.CANCEL
        }

        launchWorker<PluginPostTweetWorker>(context.applicationContext, pluginIntent.toWorkParams())
        return PluginBroadcastResult.COMPLETE
    }
}

///**
// * Plugin receiver.
// */
//class PluginReceivers {
//
//    abstract class AbstractPluginReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            logD("onReceive: %s", this.javaClass.simpleName)
//            val result = receive(context, MediaPluginIntent(intent))
//            setResult(result.resultCode, null, null)
//        }
//
//        /**
//         * Receive data.
//         */
//        private fun receive(context: Context, pluginIntent: MediaPluginIntent): PluginBroadcastResult  {
//            var result =  PluginBroadcastResult.CANCEL
//
//            val propertyData = pluginIntent.propertyData ?: return result
//            val prefs = Prefs(context)
//
//            if (this is EventPostTweetReceiver) {
//                // category
//                if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
//                    return result
//                }
//                // media
//                if (propertyData.isMediaEmpty) {
//                    context.toast(R.string.message_no_media)
//                    return result
//                }
//                // property
//                if (propertyData.getFirst(MediaProperty.TITLE).isNullOrEmpty() || propertyData.getFirst(MediaProperty.ARTIST).isNullOrEmpty()) {
//                    return result
//                }
//                // operation
//                val operation = prefs.getString(R.string.prefkey_event_tweet_operation, defRes = R.string.pref_default_event_tweet_operation)
//                if (!pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) && !pluginIntent.hasCategory(operation)) {
//                    return result
//                }
//                // previous media
//                val mediaUriText = propertyData.mediaUri?.toString()
//                val previousMediaUri = prefs.getStringOrNull(AbstractPluginService.PREFKEY_PREVIOUS_MEDIA_URI)
//                val previousMediaEnabled = prefs.getBoolean(R.string.prefkey_previous_media_enabled, defRes = R.bool.pref_default_previous_media_enabled)
//                if (!previousMediaEnabled && !mediaUriText.isNullOrEmpty() && !previousMediaUri.isNullOrEmpty() && mediaUriText == previousMediaUri) {
//                    return result
//                }
//
//                // service
//                pluginIntent.setClass(context, PluginPostService::class.java)
//                result = PluginBroadcastResult.COMPLETE
//            }
//
//            pluginIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.javaClass.name)
//            ContextCompat.startForegroundService(context, pluginIntent)
//            return result
//        }
//    }
//
//    // Event
//
//    class EventPostTweetReceiver : AbstractPluginReceiver()
//
//}
