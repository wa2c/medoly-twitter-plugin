package com.wa2c.android.medoly.plugin.action.tweet.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.twitter.twittertext.TwitterTextParser
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.Prefs
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import java.util.*
import java.util.regex.Pattern


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



    /**
     * Get tweet message text.
     * @return The message text.
     */
    protected val tweetMessage: String
        get() {
            val format = prefs.getString(R.string.prefkey_content_format, defRes = R.string.format_content_default)!!
            val TRIM_EXP = if (prefs.getBoolean(R.string.prefkey_trim_before_empty_enabled, true)) "\\w*" else ""
            val priorityList = PropertyItem.loadPropertyPriority(context)
            val containsMap = LinkedHashSet<PropertyItem>()
            for (item in priorityList) {
                val matcher = Pattern.compile(item.propertyTag, Pattern.MULTILINE).matcher(format)
                if (matcher.find())
                    containsMap.add(item)
            }

            var outputText: String = format
            for (propertyItem in containsMap) {
                var propertyText = propertyData.getFirst(propertyItem.propertyKey)
                var regexpText = propertyItem.propertyTag
                if (propertyText.isNullOrEmpty()) {
                    propertyText = ""
                    regexpText = TRIM_EXP + regexpText
                }

                var workText = outputText
                val matcher = Pattern.compile(regexpText).matcher(workText)
                while (matcher.find()) {
                    workText = matcher.replaceFirst(propertyText)
                    val removedText = TwitterUtils.getPropertyRemovedText(workText, containsMap)
                    val result = TwitterTextParser.parseTweet(removedText)
                    val remainWeight = 999 - result.permillage
                    if (remainWeight > 0) {
                        outputText = workText
                    } else {
                        if (propertyItem.shorten) {

                            //, prefs.getBoolean(R.string.prefkey_omit_newline, true)
                            workText = matcher.replaceFirst(TwitterUtils.trimWeightedText(propertyText, TwitterTextParser.parseTweet(propertyText).permillage + remainWeight, prefs.getBoolean(R.string.prefkey_omit_newline, true)))
                            outputText = TwitterUtils.getPropertyRemovedText(workText, containsMap)
                        }
                        break
                    }
                }
            }

            return outputText
        }


    @SuppressLint("NewApi")
    override fun onHandleIntent(intent: Intent?) {
        Logger.d("onHandleIntent")

        var notificationManager : NotificationManager? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
            val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("")
                    .setSmallIcon(R.drawable.ic_launcher)
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
            Logger.e(e)
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
        Logger.d("onDestroy: " + this.javaClass.simpleName)
    }

}
