package com.wa2c.android.medoly.plugin.action.tweet.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils

import com.twitter.twittertext.TwitterTextParseResults
import com.twitter.twittertext.TwitterTextParser
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.library.PropertyData
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem

import java.util.LinkedHashSet
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Plugin service base.
 */
abstract class AbstractPluginService
/**
 * Constructor.
 */
(name: String) : IntentService(name) {


    /** Context.  */
    protected var context: Context? = null
    /** Preferences.  */
    protected var sharedPreferences: SharedPreferences? = null
    /** Plugin intent.  */
    protected var pluginIntent: MediaPluginIntent
    /** Property data.  */
    protected var propertyData: PropertyData
    /** Received class name.  */
    protected var receivedClassName: String


    /**
     * Get tweet message text.
     * @return The message text.
     */
    protected// test data
            //propertyData.put(MediaProperty.TITLE, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６");
            //propertyData.put(MediaProperty.ARTIST, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９");
            //propertyData.put(MediaProperty.ALBUM, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９");
            // get contains tag
            // 1000丁度だと入らない可能性
    val tweetMessage: String
        get() {

            val format = sharedPreferences!!.getString(context!!.getString(R.string.prefkey_content_format), context!!.getString(R.string.format_content_default))
            val TRIM_EXP = if (sharedPreferences!!.getBoolean(context!!.getString(R.string.prefkey_trim_before_empty_enabled), true)) "\\w*" else ""
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
                if (TextUtils.isEmpty(propertyText)) {
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

    /**
     * Command result.
     */
    internal enum class CommandResult {
        /** Succeeded.  */
        SUCCEEDED,
        /** Failed.  */
        FAILED,
        /** Authorization failed.  */
        AUTH_FAILED,
        /** No media.  */
        NO_MEDIA,
        /** Post saved.  */
        SAVED,
        /** Ignore.  */
        IGNORE
    }

    override fun onHandleIntent(intent: Intent?) {
        Logger.d("onHandleIntent")
        if (intent == null)
            return

        try {
            context = applicationContext
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            pluginIntent = MediaPluginIntent(intent)
            propertyData = pluginIntent.propertyData
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME)
        } catch (e: Exception) {
            Logger.e(e)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("onDestroy" + this.javaClass.getSimpleName())
    }


    private fun getPropertyRemovedText(workText: String, containsMap: Set<PropertyItem>): String {
        var parseText = workText
        for (pi in containsMap) {
            parseText = parseText.replace(("%" + pi.propertyKey + "%").toRegex(), "")
        }
        return parseText
    }

    private fun trimWeightedText(propertyText: String, remainWeight: Int): String {
        if (TextUtils.isEmpty(propertyText) || propertyText.length < "...".length) {
            return ""
        }

        val newlineMatcher = Pattern.compile("\\r\\n|\\n|\\r").matcher(propertyText)
        if (newlineMatcher.find() && sharedPreferences!!.getBoolean(context!!.getString(R.string.prefkey_omit_newline), true)) {
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

    companion object {

        /** Received receiver class name.  */
        var RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME"
    }

}
