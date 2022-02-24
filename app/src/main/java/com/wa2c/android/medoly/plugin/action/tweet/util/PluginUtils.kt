package com.wa2c.android.medoly.plugin.action.tweet.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.twitter.twittertext.TwitterTextParser
import com.wa2c.android.medoly.library.MediaPluginIntent
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.Token
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import com.wa2c.android.medoly.plugin.action.tweet.service.CommandResult
import com.wa2c.android.prefs.Prefs
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.util.regex.Pattern

private const val INTENT_SRC_PACKAGE = "INTENT_SRC_PACKAGE"
private const val INTENT_SRC_CLASS = "INTENT_SRC_CLASS"
private const val INTENT_ACTION_ID = "INTENT_ACTION_ID"
private const val INTENT_ACTION_LABEL = "INTENT_ACTION_LABEL"
private const val INTENT_ACTION_PRIORITY = "INTENT_ACTION_PRIORITY"
private const val INTENT_ACTION_IS_AUTOMATICALLY = "INTENT_ACTION_IS_AUTOMATICALLY"

/** Notification ID */
private const val NOTIFICATION_ID = 1
/** Notification Channel ID */
private const val NOTIFICATION_CHANNEL_ID = "Notification"

/** True if the action was run automatically. */
val WorkerParameters.isAutomaticallyAction: Boolean
    get() = inputData.getBoolean(INTENT_ACTION_IS_AUTOMATICALLY, false)

/**
 * Create WorkParams data from plugin intent.
 */
fun MediaPluginIntent.toWorkParams(): Data {
    return Data.Builder().apply {
        putString(INTENT_SRC_PACKAGE, srcPackage)
        putString(INTENT_SRC_CLASS, srcClass)
        putString(INTENT_ACTION_ID, actionId)
        putString(INTENT_ACTION_LABEL, actionLabel)
        putInt(INTENT_ACTION_PRIORITY, actionPriority ?: 0)
        putBoolean(INTENT_ACTION_IS_AUTOMATICALLY, isAutomatically)
        putAll(propertyData?.keys?.mapNotNull {
            (it ?: return@mapNotNull null) to (propertyData?.getFirst(it) ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
        putAll(extraData?.keys?.mapNotNull {
            (it ?: return@mapNotNull null) to (propertyData?.getFirst(it) ?: return@mapNotNull null)
        }?.toMap() ?: emptyMap())
    }.build()
}

/**
 * Get worker future.
 */
@SuppressLint("RestrictedApi")
fun createForegroundFuture(context: Context): ListenableFuture<ForegroundInfo> {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).apply {
        setContentTitle(context.getString(R.string.app_name))
        setSmallIcon(R.drawable.ic_notification)
    }.build()

    return SettableFuture.create<ForegroundInfo>().apply {
        set(ForegroundInfo(NOTIFICATION_ID, notification))
    }
}

/**
 * Show message.
 */
fun Worker.showMessage(prefs: Prefs, result: CommandResult, succeededMessage: String?, failedMessage: String?, isAutomatically: Boolean) {
    if (result == CommandResult.AUTH_FAILED) {
        applicationContext.toast(R.string.message_account_not_auth)
    } else if (result == CommandResult.NO_MEDIA) {
        applicationContext.toast(R.string.message_no_media)
    } else if (result == CommandResult.SUCCEEDED && !succeededMessage.isNullOrEmpty()) {
        if (!isAutomatically || prefs.getBoolean(R.string.prefkey_tweet_success_message_show, defRes = R.bool.pref_default_tweet_success_message_show)) {
            applicationContext.toast(succeededMessage)
        }
    } else if (result == CommandResult.FAILED && !failedMessage.isNullOrEmpty()) {
        if (!isAutomatically || prefs.getBoolean(R.string.prefkey_tweet_failure_message_show, defRes = R.bool.pref_default_tweet_failure_message_show)) {
            applicationContext.toast(failedMessage)
        }
    }
}

private const val TOKEN = "token"
private const val TOKEN_SECRET = "token_secret"
private const val PREF_NAME = "twitter_access_token"

/**
 * Get a twitter instance. Auto setting the saved Access token.
 *
 * @param context A context
 * @return A twitter instance.
 */
fun getTwitterInstance(context: Context): Twitter? {
    val t1: String?
    val t2: String?

    try {
        t1 = Token.getConsumerKey()
        t2 = Token.getConsumerSecret()
    } catch (e: Exception) {
        logE(e)
        context.toast("There is no Token class.")
        return null
    }

    // Get key
    if (t1.isNullOrEmpty() || t2.isNullOrEmpty()) {
        return null
    }

    val factory = TwitterFactory()
    val twitter = factory.instance
    twitter.setOAuthConsumer(t1, t2)

    if (hasAccessToken(context)) {
        twitter.oAuthAccessToken = loadAccessToken(context)
    }
    return twitter
}

/**
 * Save the access token.
 *
 * @param context A context
 * @param accessToken The access token.
 */
fun storeAccessToken(context: Context, accessToken: AccessToken?) {
    val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val editor = preferences.edit()
    if (accessToken != null) {
        editor.putString(TOKEN, accessToken.token).apply()
        editor.putString(TOKEN_SECRET, accessToken.tokenSecret).apply()
    } else {
        editor.remove(TOKEN).apply()
        editor.remove(TOKEN_SECRET).apply()
    }
}

/**
 * Load the access token.
 *
 * @param context A context.
 * @return The access token.
 */
fun loadAccessToken(context: Context): AccessToken? {
    val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val token = preferences.getString(TOKEN, null)
    val tokenSecret = preferences.getString(TOKEN_SECRET, null)
    return if (!token.isNullOrEmpty() && !tokenSecret.isNullOrEmpty()) {
        AccessToken(token, tokenSecret)
    } else {
        null
    }
}

/**
 * Return true if an access token exists.
 *
 * @return True if an access token exists.
 */
fun hasAccessToken(context: Context): Boolean {
    return loadAccessToken(context) != null
}


/**
 * Get property tag removed text.
 */
private fun getPropertyRemovedText(workText: String, containsMap: Set<PropertyItem>): String {
    var parseText = workText
    for (pi in containsMap) {
        parseText = parseText.replace(("%" + pi.propertyKey + "%").toRegex(), "")
    }
    return parseText
}

/**
 * Get the text does not exceeded the limit length on Twitter.
 */
private fun trimWeightedText(propertyText: String?, remainWeight: Int, omitNewLine : Boolean): String {
    if (propertyText.isNullOrEmpty() || propertyText.length < "...".length) {
        return ""
    }

    val newlineMatcher = Pattern.compile("\\r\\n|\\n|\\r").matcher(propertyText)
    if (newlineMatcher.find() && omitNewLine) {
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

/**
 * Get tweet message text.
 * @return The message text.
 */
fun getTweetMessage(context: Context, propertyData: Map<String, String>): String {
    val prefs = Prefs(context)
    val format = prefs.getString(R.string.prefkey_content_format, defRes = R.string.format_content_default)
    val trimExp = if (prefs.getBoolean(R.string.prefkey_trim_before_empty_enabled, defRes = R.bool.pref_default_trim_before_empty_enabled)) "\\w*" else ""
    val priorityList = PropertyItem.loadPropertyPriority(context)
    val containsMap = LinkedHashSet<PropertyItem>()
    for (item in priorityList) {
        val matcher = Pattern.compile(item.propertyTag, Pattern.MULTILINE).matcher(format)
        if (matcher.find())
            containsMap.add(item)
    }

    var outputText: String = format
    for (propertyItem in containsMap) {
        var propertyText = propertyItem.propertyKey?.let { propertyData[it] }
        var regexpText = propertyItem.propertyTag
        if (propertyText.isNullOrEmpty()) {
            propertyText = ""
            regexpText = trimExp + regexpText
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
                    val omit = prefs.getBoolean(R.string.prefkey_omit_newline, defRes = R.bool.pref_default_omit_newline)
                    workText = matcher.replaceFirst(trimWeightedText(propertyText, TwitterTextParser.parseTweet(propertyText).permillage + remainWeight, omit))
                    outputText = getPropertyRemovedText(workText, containsMap)
                }
                break
            }
        }
    }

    return outputText
}
