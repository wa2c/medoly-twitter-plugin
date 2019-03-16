package com.wa2c.android.medoly.plugin.action.tweet.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.prefs.Prefs
import timber.log.Timber


/**
 * Abstract plugin service.
 */
abstract class AbstractPluginService(name: String) : IntentService(name) {

    companion object {
        /** Notification ID */
        private const val NOTIFICATION_ID = 1
        /** Notification Channel ID */
        private const val NOTIFICATION_CHANNEL_ID = "Notification"

        /** Received receiver class name.  */
        const val RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"
    }

    /** Context.  */
    protected lateinit var context: Context
    /** Preferences.  */
    protected lateinit var prefs: Prefs
    /** Plugin intent.  */
    protected lateinit var pluginIntent: MediaPluginIntent
    /** Property data.  */
    protected lateinit var propertyData: PropertyData
    /** Received class name.  */
    protected lateinit var receivedClassName: String

    @SuppressLint("NewApi")
    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent")

        var notificationManager : NotificationManager? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
            val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_notification)
            startForeground(NOTIFICATION_ID, builder.build())
        }

        if (intent == null)
            return

        try {
            context = applicationContext
            prefs = Prefs(this)
            pluginIntent = MediaPluginIntent(intent)
            propertyData = pluginIntent.propertyData ?: PropertyData()
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            if (notificationManager != null) {
                notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
                notificationManager.cancel(NOTIFICATION_ID)
                stopForeground(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy: " + this.javaClass.simpleName)
    }


    /**
     * Show message.
     */
    fun showMessage(result: CommandResult, succeededMessage: String?, failedMessage: String?) {
        if (result == CommandResult.AUTH_FAILED) {
            AppUtils.showToast(context, R.string.message_account_not_auth)
        } else if (result == CommandResult.NO_MEDIA) {
            AppUtils.showToast(context, R.string.message_no_media)
        } else if (result == CommandResult.SUCCEEDED && !succeededMessage.isNullOrEmpty()) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_tweet_success_message_show, defRes = R.bool.pref_default_tweet_success_message_show)) {
                AppUtils.showToast(context, succeededMessage)
            }
        } else if (result == CommandResult.FAILED && !failedMessage.isNullOrEmpty()) {
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || prefs.getBoolean(R.string.prefkey_tweet_failure_message_show, defRes = R.bool.pref_default_tweet_failure_message_show)) {
                AppUtils.showToast(context, failedMessage)
            }
        }
    }
}
