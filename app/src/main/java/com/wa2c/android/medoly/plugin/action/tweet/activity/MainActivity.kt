package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import com.wa2c.android.prefs.Prefs
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.auth.RequestToken


/**
 * Main activity
 */
class MainActivity : Activity() {

    /** preferences manager. */
    private lateinit var  prefs: Prefs
    /** Callback URL. */
    private lateinit var callbackURL: String
    /** Twitter. */
    private var twitter: Twitter? = null
    /** Request token. */
    private var requestToken: RequestToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        // ActionBar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        // Set permission
        requestPermission()

        callbackURL = getString(R.string.twitter_callback_url)
        twitter = TwitterUtils.getTwitterInstance(this)

        // Twitter Auth
        twitterOAuthButton.setOnClickListener {
            startAuthorize()
        }

        // Edit
        editButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, EditActivity::class.java))
        }

        // Settings
        settingsButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        // Launch Medoly
        launchMedolyButton.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
            if (intent == null) {
                AppUtils.showToast(this, R.string.message_no_medoly)
                return@setOnClickListener
            }
            startActivity(intent)
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
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        if (!prefs.getBoolean(R.string.prefkey_send_album_art, true))
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
     * Update authentication message
     */
    private fun updateAuthMessage() {
        val token = TwitterUtils.loadAccessToken(this)
        if (token != null) {
            twitterAuthTextView.text = getString(R.string.message_account_auth)
        } else {
            twitterAuthTextView.text = getString(R.string.message_account_not_auth)
        }
    }

    /**
     * Start OAuth.
     */
    private fun startAuthorize() {
        GlobalScope.launch(Dispatchers.Main) {
            val url = async(Dispatchers.Default) {
                try {
                    val t = twitter ?: return@async null
                    t.oAuthAccessToken = null // リセット
                    requestToken = t.getOAuthRequestToken(callbackURL)
                    return@async t.getOAuthRequestToken(callbackURL)?.authorizationURL
                } catch (e: Exception) {
                    Timber.e(e)
                    return@async null
                }
            }.await()

            if (url != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                AppUtils.showToast(this@MainActivity, R.string.message_auth_failure)
            }
        }
    }

    /**
     * OAuth completion
     * @param intent intent.。
     */
    private fun completeAuthorize(intent: Intent?) {
        if (intent == null || intent.data == null || !intent.data.toString().startsWith(callbackURL)) {
            return
        }

        // 認証結果取得
        val verifier = intent.data.getQueryParameter("oauth_verifier")

        GlobalScope.launch(Dispatchers.Main) {
            val token = async(Dispatchers.Default) {
                return@async try {
                    twitter?.getOAuthAccessToken(requestToken, verifier)
                } catch (e: TwitterException) {
                    Timber.e(e)
                    null
                }
            }.await()

            if (token != null) {
                // 認証成功
                AppUtils.showToast(this@MainActivity, R.string.message_auth_success)
                // 認証情報を保存
                TwitterUtils.storeAccessToken(this@MainActivity, token)
            } else {
                // 認証失敗
                AppUtils.showToast(this@MainActivity, R.string.message_auth_failure)
                TwitterUtils.storeAccessToken(this@MainActivity, null)
            }
            updateAuthMessage()
        }
    }



    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
    }

}
