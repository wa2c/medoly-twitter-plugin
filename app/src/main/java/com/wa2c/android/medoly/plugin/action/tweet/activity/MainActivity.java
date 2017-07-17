package com.wa2c.android.medoly.plugin.action.tweet.activity;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.wa2c.android.medoly.library.MedolyEnvironment;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils;

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

        // ActionBar
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        // パーミッション設定
        requestPermission();

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
        final Intent launchIntent =  getPackageManager().getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE);
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


    private static int PERMISSION_REQUEST_CODE = 0;

    /**
     * Require storage permission.
     */
    public void requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.getBoolean(getString(R.string.prefkey_send_album_art), true))
            return;

        // Check permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Require permission.
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    /**
     * Receive permission result.
     * @param requestCode The request code.
     * @param permissions Permissions.
     * @param grantResults The result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        for (int i = 0; i < permissions.length; i++) {
            if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) &&  grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                AppUtils.showToast(this, R.string.message_storage_permission_denied);
            }
        }
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
