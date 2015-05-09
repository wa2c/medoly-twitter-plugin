package com.wa2c.android.medoly.plugin.action.twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.widget.Toast;

import com.wa2c.android.medoly.plugin.action.Logger;

import java.lang.reflect.InvocationTargetException;

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
		String t1 = null;
		String t2 = null;

		try {
			Class cl = Class.forName(TwitterUtils.class.getPackage().getName() + ".Token");
			Class para[] = new Class[] { String.class };
			t1 = String.valueOf(cl.getMethod("getConsumerKey", para).invoke(null, context.getString(R.string.base_app_name) + "__" + context.getString(R.string.domain_name)));
			t2 = String.valueOf(cl.getMethod("getConsumerSecret", para).invoke(null, context.getString(R.string.base_app_name) + "__" + context.getString(R.string.domain_name)));
		} catch (Exception e) {
			Logger.e(e);
			AppUtils.showToast(context, "There is no Token class.");
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
		editor.putString(TOKEN, accessToken.getToken()).apply();
		editor.putString(TOKEN_SECRET, accessToken.getTokenSecret()).apply();
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
		if (token != null && tokenSecret != null) {
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
