package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.databinding.ActivityMainBinding
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.logE
import com.wa2c.android.medoly.plugin.action.tweet.util.toast
import com.wa2c.android.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import twitter4j.Twitter
import twitter4j.auth.RequestToken


/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var binding: ActivityMainBinding

    /** Callback URL. */
    private lateinit var callbackURL: String
    /** Twitter. */
    private var twitter: Twitter? = null
    /** Request token. */
    private var requestToken: RequestToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // ActionBar
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setIcon(R.drawable.ic_launcher)
        }

        callbackURL = getString(R.string.twitter_callback_url)
        twitter = TwitterUtils.getTwitterInstance(this)

        // Twitter Auth
        binding.twitterOAuthButton.setOnClickListener {
            startAuthorize()
        }

        // Edit
        binding.editButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, EditActivity::class.java))
        }

        // Settings
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        // Launch Medoly
        binding.launchMedolyButton.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
            if (intent == null) {
                toast(R.string.message_no_medoly)
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
                toast(R.string.message_storage_permission_denied)
            }
        }
    }


    /**
     * Update authentication message
     */
    private fun updateAuthMessage() {
        val token = TwitterUtils.loadAccessToken(this)
        if (token != null) {
            binding.twitterAuthTextView.text = getString(R.string.message_account_auth)
        } else {
            binding.twitterAuthTextView.text = getString(R.string.message_account_not_auth)
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
                    return@async requestToken?.authorizationURL
                } catch (e: Exception) {
                    logE(e)
                    return@async null
                }
            }.await()

            if (url != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                toast(R.string.message_auth_failure)
            }
        }
    }

    /**
     * OAuth completion
     * @param intent intent.。
     */
    private fun completeAuthorize(intent: Intent?) {
        if (intent == null || intent.data == null || !intent.data!!.toString().startsWith(callbackURL)) {
            return
        }

        // Get auth verifier
        val verifier = intent.data!!.getQueryParameter("oauth_verifier")

        GlobalScope.launch(Dispatchers.Main) {
            val token = async(Dispatchers.Default) {
                return@async try {
                    twitter?.getOAuthAccessToken(requestToken, verifier)
                } catch (e: Exception) {
                    logE(e)
                    null
                }
            }.await()

            when {
                token != null -> {
                    // Auth succeeded
                    toast(R.string.message_auth_success)
                    TwitterUtils.storeAccessToken(this@MainActivity, token)
                }
                verifier == null -> {
                    // Auth canceled
                    toast(R.string.message_account_clear)
                    TwitterUtils.storeAccessToken(this@MainActivity, null)
                }
                else -> {
                    // Auth failed
                    toast(R.string.message_auth_failure)
                    TwitterUtils.storeAccessToken(this@MainActivity, null)
                }
            }
            updateAuthMessage()
        }
    }



    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
    }

}
