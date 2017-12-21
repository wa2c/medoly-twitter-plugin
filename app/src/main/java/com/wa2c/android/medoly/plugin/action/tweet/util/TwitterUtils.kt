package com.wa2c.android.medoly.plugin.action.tweet.util

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.text.TextUtils

import com.wa2c.android.medoly.plugin.action.tweet.Token

import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken

object TwitterUtils {

    private val TOKEN = "token"
    private val TOKEN_SECRET = "token_secret"
    private val PREF_NAME = "twitter_access_token"

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
}
