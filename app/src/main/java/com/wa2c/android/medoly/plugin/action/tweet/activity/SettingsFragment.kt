package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.dialog.AboutDialogFragment
import java.util.*


/**
 * Settings fragment
 */
class SettingsFragment : PreferenceFragment() {

    companion object {
        /** Summary length map.  */
        private val summaryLengthMap = LinkedHashMap<Preference, Int>()
    }



    /**
     * App info.
     */
    private val applicationDetailsPreferenceClickListener = Preference.OnPreferenceClickListener {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.parse("package:" + activity.packageName)
        startActivity(intent)
        true
    }

    /**
     * About.
     */
    private val aboutPreferenceClickListener = Preference.OnPreferenceClickListener {
        AboutDialogFragment.newInstance().show(activity)
        true
    }

    /**
     * On change settings.
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> updatePrefSummary(findPreference(key)) }

    /**
     * onCreate event.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_settings)

        // App info
        findPreference(getString(R.string.prefkey_application_details)).onPreferenceClickListener = applicationDetailsPreferenceClickListener
        // About
        findPreference(getString(R.string.prefkey_about)).onPreferenceClickListener = aboutPreferenceClickListener

        initSummary(preferenceScreen)
    }

    /**
     * onResume event.
     */
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * onPause event.
     */
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Initialize summary.
     * @param p target item.
     */
    private fun initSummary(p: Preference) {
        // get summary length
        val summary = p.summary
        if (!TextUtils.isEmpty(summary)) {
            if (summary.toString().lastIndexOf("\n") != 0) p.summary = summary.toString() + "\n" // add break
            summaryLengthMap.put(p, p.summary.length)
        } else {
            summaryLengthMap.put(p, 0)
        }

        // update summary
        when (p) {
            is PreferenceCategory -> {
                val pCat = p as PreferenceCategory?
                for (i in 0 until pCat!!.preferenceCount) {
                    initSummary(pCat.getPreference(i))
                }
            }
            is PreferenceScreen -> {
                val ps = p as PreferenceScreen?
                for (i in 0 until ps!!.preferenceCount) {
                    initSummary(ps.getPreference(i))
                }
            }
            else -> updatePrefSummary(p)
        }
    }

    /**
     * Update summary.
     * @param p target preference.
     */
    private fun updatePrefSummary(p: Preference) {
        val key = p.key
        var summary = p.summary
        if (TextUtils.isEmpty(key)) return
        if (TextUtils.isEmpty(summary)) summary = ""

        val labelSize = summaryLengthMap[p] ?: 0

        // for instance type
        when (p) {
            is ListPreference -> {
                // ListPreference
                p.value = p.sharedPreferences.getString(p.key, "")
                p.setSummary(summary.subSequence(0, labelSize).toString() + getString(R.string.settings_summary_current_value, p.entry))
            }
            is MultiSelectListPreference -> {
                // MultiSelectListPreference
                val stringSet = p.sharedPreferences.getStringSet(p.key, null)
                var text = ""
                if (stringSet != null && stringSet.size > 0) {
                    p.values = stringSet // update value once
                    val builder = StringBuilder()
                    (0 until p.entries.size)
                            .filter { stringSet.contains(p.entryValues[it]) }
                            .forEach { builder.append(p.entries[it]).append(",") }
                    if (builder.isNotEmpty()) {
                        text = builder.substring(0, builder.length - 1) // remove end comma
                    }
                }
                p.setSummary(summary.subSequence(0, labelSize).toString() + getString(R.string.settings_summary_current_value, text))
            }
            is EditTextPreference -> {
                // EditTextPreference
                var text = p.sharedPreferences.getString(p.key, "")

                // adjust numeric values
                val inputType = p.editText.inputType
                try {
                    if (inputType and InputType.TYPE_CLASS_NUMBER > 0) {
                        if (inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0) {
                            // float
                            var `val` = java.lang.Float.valueOf(text)!!
                            if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
                                `val` = 0f
                            }
                            text = `val`.toString()
                        } else {
                            // integer
                            var `val` = Integer.valueOf(text)!!
                            if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
                                `val` = 0
                            }
                            text = `val`.toString()
                        }
                    }
                } catch (e: NumberFormatException) {
                    text = "0"
                }

                p.text = text // update once
                p.setSummary(summary.subSequence(0, labelSize).toString() + getString(R.string.settings_summary_current_value, text))
            }
        }
    }

}
