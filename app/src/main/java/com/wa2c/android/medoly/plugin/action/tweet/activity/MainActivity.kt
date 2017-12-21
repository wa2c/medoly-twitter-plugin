package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.Manifest
import android.app.ActionBar
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.View
import android.widget.TextView

import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils

import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken


class MainActivity : Activity() {

    /** コールバックURI。  */
    private var callbackURL: String? = null
    /** ツイッターオブジェクト。  */
    private var twitter: Twitter? = null
    /** リクエストトークン。  */
    private var requestToken: RequestToken? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ActionBar
        val actionBar = actionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayShowTitleEnabled(true)
        }

        // パーミッション設定
        requestPermission()

        callbackURL = getString(R.string.twitter_callback_url)
        twitter = TwitterUtils.getTwitterInstance(this)

        // Twitter認証
        findViewById(R.id.twitterOAuthButton).setOnClickListener { startAuthorize() }

        // 編集
        findViewById(R.id.editButton).setOnClickListener { startActivity(Intent(this@MainActivity, EditActivity::class.java)) }

        // 設定
        findViewById(R.id.settingsButton).setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }

        // Medoly起動
        val launchIntent = packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
        findViewById(R.id.launchMedolyButton).setOnClickListener { if (launchIntent != null) startActivity(launchIntent) }
        if (launchIntent == null) {
            findViewById(R.id.launchMedolyButton).visibility = View.GONE
            findViewById(R.id.noMedolyTextView).visibility = View.VISIBLE
        } else {
            findViewById(R.id.launchMedolyButton).visibility = View.VISIBLE
            findViewById(R.id.noMedolyTextView).visibility = View.GONE
        }

        updateAuthMessage()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        completeAuthorize(intent)
    }

    /**
     * Require storage permission.
     */
    fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (!pref.getBoolean(getString(R.string.prefkey_send_album_art), true))
            return

        // Check permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Require permission.
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

    /**
     * Receive permission result.
     * @param requestCode The request code.
     * @param permissions Permissions.
     * @param grantResults The result
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }

        for (i in permissions.indices) {
            if (Manifest.permission.READ_EXTERNAL_STORAGE == permissions[i] && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                AppUtils.showToast(this, R.string.message_storage_permission_denied)
            }
        }
    }


    /**
     * 認証状態のメッセージを更新する。
     */
    private fun updateAuthMessage() {
        val token = TwitterUtils.loadAccessToken(this)
        if (token != null) {
            (findViewById(R.id.twitterAuthTextView) as TextView).text = getString(R.string.message_account_auth)
        } else {
            (findViewById(R.id.twitterAuthTextView) as TextView).text = getString(R.string.message_account_not_auth)
        }
    }

    /**
     * OAuth認証開始。
     */
    private fun startAuthorize() {
        val task = object : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void): String? {
                try {
                    // 新規登録
                    twitter!!.oAuthAccessToken = null // リセット
                    requestToken = twitter!!.getOAuthRequestToken(callbackURL)
                    return requestToken!!.authorizationURL
                } catch (e: Exception) {
                    Logger.e(e)
                }

                return null
            }

            override fun onPostExecute(url: String?) {
                if (url != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } else {
                    AppUtils.showToast(this@MainActivity, R.string.message_auth_failure)
                }
            }
        }
        task.execute()

    }

    /**
     * OAuth認証完了。
     * @param intent 処理結果のインテント。
     */
    private fun completeAuthorize(intent: Intent?) {
        if (intent == null || intent.data == null || !intent.data!!.toString().startsWith(callbackURL!!)) {
            return
        }

        // 認証結果取得
        val verifier = intent.data!!.getQueryParameter("oauth_verifier")

        val task = object : AsyncTask<String, Void, AccessToken>() {
            override fun doInBackground(vararg params: String): AccessToken? {
                if (params != null && params.size > 0 && !TextUtils.isEmpty(params[0])) {
                    try {
                        return twitter!!.getOAuthAccessToken(requestToken, params[0])
                    } catch (e: TwitterException) {
                        e.printStackTrace()
                    }

                }
                return null
            }

            override fun onPostExecute(accessToken: AccessToken?) {
                if (accessToken != null) {
                    // 認証成功
                    AppUtils.showToast(this@MainActivity, R.string.message_auth_success)
                    // 認証情報を保存
                    TwitterUtils.storeAccessToken(this@MainActivity, accessToken)
                } else {
                    // 認証失敗
                    AppUtils.showToast(this@MainActivity, R.string.message_auth_failure)
                    TwitterUtils.storeAccessToken(this@MainActivity, null)
                }
                updateAuthMessage()
            }
        }
        task.execute(verifier)
    }

    companion object {


        private val PERMISSION_REQUEST_CODE = 0
    }

}
