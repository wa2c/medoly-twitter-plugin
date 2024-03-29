package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mikepenz.aboutlibraries.LibsBuilder
import com.wa2c.android.medoly.plugin.action.tweet.BuildConfig
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.initSummary
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.preference
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.setListener
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.updatePrefSummary

/**
 * Settings fragment
 */
class SettingsFragment : PreferenceFragmentCompat() {

    /** On change settings. */
    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> updatePrefSummary(key) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_settings)
        setClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_screen_settings)
        initSummary(preferenceScreen)
        preference<Preference>(R.string.prefkey_info_app_version)?.summary = BuildConfig.VERSION_NAME
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(changeListener)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(changeListener)
    }


    private fun setClickListener() {
        // App Version
        setListener(R.string.prefkey_info_app_version) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_version_url))))
        }

        // Author
        setListener(R.string.prefkey_info_author) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_author_url))))
        }

        // Manual
        setListener(R.string.prefkey_info_manual) {
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_manual_url))).let {
                startActivity(it)
            }
        }

        // License
        setListener(R.string.prefkey_info_app_license) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_license_url))))
        }

        // Open Source License
        setListener(R.string.prefkey_info_library_license) {
            val libs = LibsBuilder().withAboutAppName(getString(R.string.app_name))
            val ft = parentFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, libs.supportFragment())
            ft.addToBackStack(getString(R.string.pref_title_info_library_license))
            ft.commit()
        }

        // Privacy Policy
        setListener(R.string.prefkey_info_privacy_policy) {
            val url = getString(R.string.app_privacy_policy_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // App Info
        setListener(R.string.prefkey_info_app) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().packageName)))
        }

        // App Store
        setListener(R.string.prefkey_info_store) {
            val url = getString(R.string.app_store_url)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

    }
}
