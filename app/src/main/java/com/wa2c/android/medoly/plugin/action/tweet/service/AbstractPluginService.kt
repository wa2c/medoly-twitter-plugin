package com.wa2c.android.medoly.plugin.action.tweet.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.twitter.twittertext.TwitterTextParser
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem
import java.util.*
import java.util.regex.Pattern


/**
 * Plugin service base.
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
    protected lateinit var preferences: SharedPreferences
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

            val format = preferences.getString(context!!.getString(R.string.prefkey_content_format), context!!.getString(R.string.format_content_default))
            val TRIM_EXP = if (preferences.getBoolean(context!!.getString(R.string.prefkey_trim_before_empty_enabled), true)) "\\w*" else ""
            val priorityList = PropertyItem.loadPropertyPriority(context)
            val containsMap = LinkedHashSet<PropertyItem>()
            for (item in priorityList) {
                val matcher = Pattern.compile(item.propertyTag, Pattern.MULTILINE).matcher(format!!)
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
                    val removedText = getPropertyRemovedText(workText, containsMap)
                    val result = TwitterTextParser.parseTweet(removedText)
                    val remainWeight = 999 - result.permillage
                    if (remainWeight > 0) {
                        outputText = workText
                    } else {
                        if (propertyItem.shorten) {
                            workText = matcher.replaceFirst(trimWeightedText(propertyText, TwitterTextParser.parseTweet(propertyText).permillage + remainWeight))
                            outputText = getPropertyRemovedText(workText, containsMap)
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
            preferences = PreferenceManager.getDefaultSharedPreferences(context)
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


    private fun getPropertyRemovedText(workText: String, containsMap: Set<PropertyItem>): String {
        var parseText = workText
        for (pi in containsMap) {
            parseText = parseText.replace(("%" + pi.propertyKey + "%").toRegex(), "")
        }
        return parseText
    }

    private fun trimWeightedText(propertyText: String?, remainWeight: Int): String {
        if (propertyText.isNullOrEmpty() || propertyText!!.length < "...".length) {
            return ""
        }

        val newlineMatcher = Pattern.compile("\\r\\n|\\n|\\r").matcher(propertyText)
        if (newlineMatcher.find() && preferences.getBoolean(context!!.getString(R.string.prefkey_omit_newline), true)) {
            var returnText = ""
            while (newlineMatcher.find()) {
                val result = TwitterTextParser.parseTweet(propertyText.substring(0, newlineMatcher.start()) + "...")
                if (result.permillage >= remainWeight) {
                    break
                }
                returnText = propertyText.substring(0, newlineMatcher.start()) + "..."
            }
            return returnText
        } else {
            for (i in 1 until propertyText.length) {
                val result = TwitterTextParser.parseTweet(propertyText.substring(0, i) + "...")
                if (result.permillage >= remainWeight) {
                    return propertyText.substring(0, i - 1) + "..."
                }
            }
            return propertyText
        }
    }

}
