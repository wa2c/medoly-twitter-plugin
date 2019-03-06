package com.wa2c.android.medoly.plugin.action.tweet.util

import android.content.Context
import com.twitter.twittertext.TwitterTextParser
import com.wa2c.android.medoly.plugin.action.tweet.Token
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import timber.log.Timber
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.util.regex.Pattern

/**
 * Twitter Utils
 */
object TwitterUtils {

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
            Timber.e(e)
            AppUtils.showToast(context, "There is no Token class.")
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
    fun getPropertyRemovedText(workText: String, containsMap: Set<PropertyItem>): String {
        var parseText = workText
        for (pi in containsMap) {
            parseText = parseText.replace(("%" + pi.propertyKey + "%").toRegex(), "")
        }
        return parseText
    }

    /**
     * Get the text does not exceeded the limit length on Twitter.
     */
    fun trimWeightedText(propertyText: String?, remainWeight: Int, omitNewLine : Boolean): String {
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

}
