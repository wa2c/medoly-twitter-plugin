package com.wa2c.android.medoly.plugin.action.tweet.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.prefs.Prefs
import timber.log.Timber
import java.io.InvalidObjectException


/**
 * Abstract plugin service.
 */
abstract class AbstractPluginService(name: String) : IntentService(name) {

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
    /** Notification manager. */
    private var notificationManager : NotificationManager? = null


    @SuppressLint("NewApi")
    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_notification)
            startForeground(NOTIFICATION_ID, builder.build())
        }

        if (intent == null)
            throw InvalidObjectException("Null intent")

        context = applicationContext
        prefs = Prefs(this)
        pluginIntent = MediaPluginIntent(intent)
        propertyData = pluginIntent.propertyData ?: PropertyData()
        receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.let {
            it.cancel(NOTIFICATION_ID)
            stopForeground(true)
        }
        Timber.d("onDestroy: %s", this.javaClass.simpleName)
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



    companion object {
        /** Notification ID */
        private const val NOTIFICATION_ID = 1
        /** Notification Channel ID */
        private const val NOTIFICATION_CHANNEL_ID = "Notification"

        /** Received receiver class name.  */
        const val RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
        /** Previous data key.  */
        const val PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri"

        /**
         * Create notification
         */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
