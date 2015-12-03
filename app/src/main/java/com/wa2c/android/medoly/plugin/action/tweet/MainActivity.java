package com.wa2c.android.medoly.plugin.action.tweet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.wa2c.android.medoly.library.MedolyParam;
import com.wa2c.android.medoly.utils.Logger;

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

        callbackURL = getString(R.string.twitter_callback_url);
        twitter = TwitterUtils.getTwitterInstance(this);



        // Twitter認証
        findViewById(R.id.twitterOAuthButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAuthorize();
            }
        });

        // 編集
        findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditActivity.class));
            }
        });

        // 設定
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Medoly起動
        final Intent launchIntent =  getPackageManager().getLaunchIntentForPackage(MedolyParam.MEDOLY_PACKAGE);
        findViewById(R.id.launchMedolyButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (launchIntent != null) startActivity(launchIntent);
            }
        });
        if (launchIntent == null) {
            findViewById(R.id.launchMedolyButton).setVisibility(View.GONE);
            findViewById(R.id.noMedolyTextView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.launchMedolyButton).setVisibility(View.VISIBLE);
            findViewById(R.id.noMedolyTextView).setVisibility(View.GONE);
        }

        updateAuthMessage();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        completeAuthorize(intent);
    }

    /**
     * 認証状態のメッセージを更新する。
     */
    private void updateAuthMessage() {
        AccessToken token = TwitterUtils.loadAccessToken(this);
        if (token != null) {
            ((TextView)findViewById(R.id.twitterAuthTextView)).setText(getString(R.string.message_account_auth));
        } else {
            ((TextView)findViewById(R.id.twitterAuthTextView)).setText(getString(R.string.message_account_not_auth));
        }
    }


    /**
     * OAuth認証開始。
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
                } catch (Exception e) {
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
                    AppUtils.showToast(MainActivity.this, R.string.message_auth_failure);
                }
            }
        };
        task.execute();

    }

    /**
     * OAuth認証完了。
     * @param intent 処理結果のインテント。
     */
    private void completeAuthorize(Intent intent) {
        if (intent == null || intent.getData() == null || !intent.getData().toString().startsWith(callbackURL)) {
            return;
        }

        // 認証結果取得
        String verifier = intent.getData().getQueryParameter("oauth_verifier");

        AsyncTask<String, Void, AccessToken> task = new AsyncTask<String, Void, AccessToken>() {
            @Override
            protected AccessToken doInBackground(String... params) {
                if (params != null && params.length > 0 && !TextUtils.isEmpty(params[0])) {
                    try {
                        return twitter.getOAuthAccessToken(requestToken, params[0]);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AccessToken accessToken) {
                if (accessToken != null) {
                    // 認証成功
                    AppUtils.showToast(MainActivity.this, R.string.message_auth_success);
                    // 認証情報を保存
                    TwitterUtils.storeAccessToken(MainActivity.this, accessToken);
                } else {
                    // 認証失敗
                    AppUtils.showToast(MainActivity.this, R.string.message_auth_failure);
                    TwitterUtils.storeAccessToken(MainActivity.this, null);
                }
                updateAuthMessage();
            }
        };
        task.execute(verifier);
    }

}
