package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.wa2c.android.medoly.plugin.action.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterUtils {

    private static final String TOKEN = "token";
    private static final String TOKEN_SECRET = "token_secret";
    private static final String PREF_NAME = "twitter_access_token";

    /**
     * Twitterインスタンスを取得します。アクセストークンが保存されていれば自動的にセットします。
     *
     * @param context コンテキスト。
     * @return Twitterインスタンス。
     */
    @SuppressWarnings("unchecked")
    public static Twitter getTwitterInstance(Context context) {
        String t1;
        String t2;

        try {
            String k = context.getString(R.string.base_app_name) + "__" + context.getString(R.string.domain_name);
            t1 = Token.getConsumerKey(k);
            t2 = Token.getConsumerSecret(k);
        } catch (Exception e) {
            Logger.e(e);
            AppUtils.showToast(context, "There is no Token class.");
            return null;
        }

        // キー未取得
        if (TextUtils.isEmpty(t1) || TextUtils.isEmpty(t2)) {
            return null;
        }

        TwitterFactory factory = new TwitterFactory();
        Twitter twitter = factory.getInstance();
        twitter.setOAuthConsumer(t1, t2);

        if (hasAccessToken(context)) {
            twitter.setOAuthAccessToken(loadAccessToken(context));
        }
        return twitter;
    }

    /**
     * アクセストークンをプリファレンスに保存します。
     *
     * @param context コンテキスト。
     * @param accessToken アクセストークン。
     */
    public static void storeAccessToken(Context context, AccessToken accessToken) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        if (accessToken != null) {
            editor.putString(TOKEN, accessToken.getToken()).apply();
            editor.putString(TOKEN_SECRET, accessToken.getTokenSecret()).apply();
        } else {
            editor.remove(TOKEN).apply();
            editor.remove(TOKEN_SECRET).apply();
        }
    }

    /**
     * アクセストークンをプリファレンスから読み込みます。
     *
     * @param context コンテキスト。
     * @return アクセストークン。
     */
    public static AccessToken loadAccessToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(TOKEN, null);
        String tokenSecret = preferences.getString(TOKEN_SECRET, null);
        if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(tokenSecret)) {
            return new AccessToken(token, tokenSecret);
        } else {
            return null;
        }
    }

    /**
     * アクセストークンが存在する場合はtrueを返します。
     *
     * @return アクセストークンが存在する場合はtrue。
     */
    public static boolean hasAccessToken(Context context) {
        return loadAccessToken(context) != null;
    }
}
