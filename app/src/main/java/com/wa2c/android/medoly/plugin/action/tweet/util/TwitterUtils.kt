package com.wa2c.android.medoly.plugin.action.tweet.util

import android.content.Context
import android.text.TextUtils
import com.twitter.twittertext.TwitterTextParser

import com.wa2c.android.medoly.plugin.action.tweet.Token
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem

import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.util.regex.Pattern

object TwitterUtils {

    private const val TOKEN = "token"
    private const val TOKEN_SECRET = "token_secret"
    private const val PREF_NAME = "twitter_access_token"

    /**
     * Twitterインスタンスを取得します。アクセストークンが保存されていれば自動的にセットします。
     *
     * @param context コンテキスト。
     * @return Twitterインスタンス。
     */
    fun getTwitterInstance(context: Context): Twitter? {
        val t1: String?
        val t2: String?

        try {
            t1 = Token.getConsumerKey(context)
            t2 = Token.getConsumerSecret(context)
        } catch (e: Exception) {
            Logger.e(e)
            AppUtils.showToast(context, "There is no Token class.")
            return null
        }

        // キー未取得
        if (TextUtils.isEmpty(t1) || TextUtils.isEmpty(t2)) {
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
     * アクセストークンをプリファレンスに保存します。
     *
     * @param context コンテキスト。
     * @param accessToken アクセストークン。
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
     * アクセストークンをプリファレンスから読み込みます。
     *
     * @param context コンテキスト。
     * @return アクセストークン。
     */
    fun loadAccessToken(context: Context): AccessToken? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val token = preferences.getString(TOKEN, null)
        val tokenSecret = preferences.getString(TOKEN_SECRET, null)
        return if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(tokenSecret)) {
            AccessToken(token!!, tokenSecret!!)
        } else {
            null
        }
    }

    /**
     * アクセストークンが存在する場合はtrueを返します。
     *
     * @return アクセストークンが存在する場合はtrue。
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
        if (propertyText.isNullOrEmpty() || propertyText!!.length < "...".length) {
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
