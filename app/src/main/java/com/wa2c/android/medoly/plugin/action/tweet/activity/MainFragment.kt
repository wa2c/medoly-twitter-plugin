package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wa2c.android.medoly.library.MedolyEnvironment
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.viewBinding
import com.wa2c.android.medoly.plugin.action.tweet.databinding.FragmentMainBinding
import com.wa2c.android.medoly.plugin.action.tweet.util.*
import kotlinx.coroutines.*
import twitter4j.Twitter
import twitter4j.auth.RequestToken

/**
 * Main Fragment
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    /** Binding */
    private val binding: FragmentMainBinding? by viewBinding()
    /** Callback URL. */
    private val callbackURL: String by lazy { getString(R.string.twitter_callback_url) }
    /** Twitter. */
    private val twitter: Twitter by lazy { getTwitterInstance(requireContext())!! }

    /** Request token. */
    private var requestToken: RequestToken? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.app_name)

        binding?.let { binding ->
            // Twitter Auth
            binding.twitterOAuthButton.setOnClickListener {
                startAuthorize()
            }

            // Edit
            binding.editButton.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, EditFragment())
                    .addToBackStack(null)
                    .commit()
            }

            // Settings
            binding.settingsButton.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
            }

            // Launch Medoly
            binding.launchMedolyButton.setOnClickListener {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(MedolyEnvironment.MEDOLY_PACKAGE)
                if (intent == null) {
                    toast(R.string.message_no_medoly)
                    return@setOnClickListener
                }
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateAuthMessage()
    }

    /**
     * Update authentication message
     */
    private fun updateAuthMessage() {
        val token = loadAccessToken(requireContext())
        binding?.twitterAuthTextView?.text = if (token != null) {
           getString(R.string.message_account_auth)
        } else {
           getString(R.string.message_account_not_auth)
        }
    }

    /**
     * Start OAuth.
     */
    private fun startAuthorize() {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val url = async(Dispatchers.Default) {
                try {
                    val t = twitter
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
    fun completeAuthorize(intent: Intent?) {
        if (intent == null || intent.data == null || !intent.data!!.toString().startsWith(callbackURL)) {
            return
        }

        // Get auth verifier
        val verifier = intent.data!!.getQueryParameter("oauth_verifier")

        CoroutineScope(Dispatchers.Main + Job()).launch {
            val token = async(Dispatchers.Default) {
                return@async try {
                    twitter.getOAuthAccessToken(requestToken, verifier)
                } catch (e: Exception) {
                    logE(e)
                    null
                }
            }.await()

            when {
                token != null -> {
                    // Auth succeeded
                    toast(R.string.message_auth_success)
                    storeAccessToken(requireContext(), token)
                }
                verifier == null -> {
                    // Auth canceled
                    toast(R.string.message_account_clear)
                    storeAccessToken(requireContext(), null)
                }
                else -> {
                    // Auth failed
                    toast(R.string.message_auth_failure)
                    storeAccessToken(requireContext(), null)
                }
            }
            updateAuthMessage()
        }
    }

}
