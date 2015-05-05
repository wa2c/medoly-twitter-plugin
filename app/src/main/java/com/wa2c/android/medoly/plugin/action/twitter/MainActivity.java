package com.wa2c.android.medoly.plugin.action.twitter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wa2c.android.medoly.plugin.common.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends Activity {

	/** コールバックURI。 */
	private String callbackURL;
	/** ツイッターオブジェクト。 */
	private Twitter twitter;
	/** リクエストトークン。 */
	private RequestToken requestToken;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.twitterOAuthButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startAuthorize();
			}
		});

		findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SettingsActivity.class));
			}
		});

		callbackURL = getString(R.string.twitter_callback_url);
		twitter = TwitterUtils.getTwitterInstance(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * OAuth認証を開始。
	 */
	private void startAuthorize() {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					// 新規登録
					twitter.setOAuthAccessToken(null); // リセット
					requestToken = twitter.getOAuthRequestToken(callbackURL);
					return requestToken.getAuthorizationURL();
				} catch (IllegalStateException e) {
					//  登録済み
					//return twitter.getOAuth  requestToken.getAuthorizationURL();
					Logger.e(e);
				} catch (Exception e) {
					// エラー
					Logger.e(e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(String url) {
				if (url != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
				} else {
					AppUtils.showToast(MainActivity.this, R.string.message_oauth_completed);
				}
			}
		};
		task.execute();
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (intent == null|| intent.getData() == null || !intent.getData().toString().startsWith(callbackURL)) {
			return;
		}

		// 認証情報
		String verifier = intent.getData().getQueryParameter("oauth_verifier");

		AsyncTask<String, Void, AccessToken> task = new AsyncTask<String, Void, AccessToken>() {
			@Override
			protected AccessToken doInBackground(String... params) {
				try {
					return twitter.getOAuthAccessToken(requestToken, params[0]);
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(AccessToken accessToken) {
				if (accessToken != null) {
					// 認証成功
					AppUtils.showToast(MainActivity.this, R.string.message_oauth_completed);
					// 認証情報を保存
					TwitterUtils.storeAccessToken(MainActivity.this, accessToken);
				} else {
					// 認証失敗
					AppUtils.showToast(MainActivity.this, R.string.message_oauth_failed);
				}
			}
		};
		task.execute(verifier);
	}

}
