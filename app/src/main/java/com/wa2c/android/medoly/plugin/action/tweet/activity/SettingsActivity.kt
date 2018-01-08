package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.MenuItem
import com.wa2c.android.medoly.plugin.action.tweet.R

/**
 * Settings activity
 */
class SettingsActivity : PreferenceActivity() {

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
        setContentView(R.layout.activity_settings)

        // Action bar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_settings)
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }


//    /**
//     * 設定画面フラグメント。
//     */
//    class SettingsFragment : PreferenceFragment() {
//
//
//        /**
//         * アプリ情報。
//         */
//        private val applicationDetailsPreferenceClickListener = Preference.OnPreferenceClickListener {
//            val intent = Intent()
//            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//            intent.data = Uri.parse("package:" + activity.packageName)
//            startActivity(intent)
//            true
//        }
//        /**
//         * About.
//         */
//        private val aboutPreferenceClickListener = Preference.OnPreferenceClickListener {
//            val layoutView = View.inflate(activity, R.layout.layout_about, null) as RelativeLayout
//
//            // Version
//            try {
//                val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)
//                (layoutView.findViewById(R.id.aboutAppVersionTextView) as TextView).text = "Ver. " + packageInfo.versionName
//            } catch (e: NameNotFoundException) {
//                Logger.e(e)
//            }
//
//            // Developer
//            (layoutView.findViewById(R.id.aboutDeveloperNameTextView) as TextView).text = getString(R.string.app_author)
//
//            // Link
//            val filter = Linkify.TransformFilter { match, url -> getString(R.string.app_market_web) }
//            Linkify.addLinks(
//                    layoutView.findViewById(R.id.aboutGooglePlayTextView) as TextView,
//                    Pattern.compile("Google Play"),
//                    getString(R.string.app_market_web), null,
//                    filter)
//
//            // Contact
//            (layoutView.findViewById(R.id.aboutEmailTextView) as TextView).text = getString(R.string.app_mail_name) + "@" + getString(R.string.app_mail_domain)
//
//            // Library
//            val libraryNames = resources.getStringArray(R.array.about_library_names)
//            val libraryUrls = resources.getStringArray(R.array.about_library_urls)
//
//            for (i in libraryNames.indices) {
//                val libTextView: TextView
//                val libraryLayout = layoutView.findViewById(R.id.abountLibraryLayout) as LinearLayout
//                libTextView = TextView(activity)
//                libTextView.movementMethod = LinkMovementMethod.getInstance()
//                libTextView.text = Html.fromHtml("<a href=\"" + libraryUrls[i] + "\">" + libraryNames[i] + "</a>")
//                libTextView.gravity = Gravity.CENTER_HORIZONTAL
//                libraryLayout.setPadding(2, 2, 2, 2)
//                libraryLayout.addView(libTextView)
//            }
//
//            // ダイアログ作成
//            val builder = AlertDialog.Builder(activity)
//            builder.setTitle(R.string.pref_title_about)
//            builder.setView(layoutView)
//            builder.setPositiveButton(android.R.string.ok, null)
//            builder.create().show()
//
//            true
//        }
//
//        /**
//         * 設定更新時の処理。
//         */
//        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
//            // サマリ更新
//            updatePrefSummary(findPreference(key))
//        }
//
//        /**
//         * onCreate event.
//         */
//        override fun onCreate(savedInstanceState: Bundle?) {
//            super.onCreate(savedInstanceState)
//            addPreferencesFromResource(R.xml.pref_settings)
//
//            // アプリ情報
//            findPreference(getString(R.string.prefkey_application_details)).onPreferenceClickListener = applicationDetailsPreferenceClickListener
//            // About
//            findPreference(getString(R.string.prefkey_about)).onPreferenceClickListener = aboutPreferenceClickListener
//
//            initSummary(preferenceScreen)
//        }
//
//        /**
//         * onResume event.
//         */
//        override fun onResume() {
//            super.onResume()
//            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
//        }
//
//        /**
//         * onPause event.
//         */
//        override fun onPause() {
//            super.onPause()
//            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
//        }
//
//        /**
//         * サマリを初期化する。
//         * @param p 対象項目。
//         */
//        private fun initSummary(p: Preference?) {
//            if (p == null) return
//
//            // サマリの長さ取得
//            val summary = p.summary
//            if (summary != null && summary.length > 0) {
//                if (summary.toString().lastIndexOf("\n") != 0) p.summary = summary.toString() + "\n" // 改行追加
//                summaryLengthMap.put(p, p.summary.length)
//            } else {
//                summaryLengthMap.put(p, 0)
//            }
//
//            // サマリ更新
//            if (p is PreferenceCategory) {
//                val pCat = p as PreferenceCategory?
//                for (i in 0 until pCat!!.preferenceCount) {
//                    initSummary(pCat!!.getPreference(i))
//                }
//            } else if (p is PreferenceScreen) {
//                val ps = p as PreferenceScreen?
//                for (i in 0 until ps!!.preferenceCount) {
//                    initSummary(ps!!.getPreference(i))
//                }
//            } else {
//                updatePrefSummary(p)
//            }
//        }
//
//        /**
//         * サマリを更新する。
//         * @param p 対象項目。
//         */
//        private fun updatePrefSummary(p: Preference?) {
//            if (p == null) return
//
//            val key = p.key
//            var summary = p.summary
//            if (TextUtils.isEmpty(key)) return
//            if (TextUtils.isEmpty(summary)) summary = ""
//
//            // 種別毎
//            if (p is ListPreference) {
//                // ListPreference
//                val pref = p as ListPreference?
//                pref!!.value = p.sharedPreferences.getString(pref.key, "") // 一度値を更新
//                p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, pref.entry))
//            } else if (p is MultiSelectListPreference) {
//                // MultiSelectListPreference
//                val pref = p as MultiSelectListPreference?
//                val stringSet = pref!!.sharedPreferences.getStringSet(pref.key, null)
//                var text = ""
//                if (stringSet != null && stringSet.size > 0) {
//                    pref.values = stringSet // 一度値を更新
//                    val builder = StringBuilder()
//                    for (i in 0 until pref.entries.size) {
//                        if (stringSet.contains(pref.entryValues[i])) {
//                            builder.append(pref.entries[i]).append(",")
//                        }
//                    }
//                    if (builder.length > 0) {
//                        text = builder.substring(0, builder.length - 1) // 末尾のカンマ削除
//                    }
//                }
//                p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, text))
//            } else if (p is EditTextPreference) {
//                // EditTextPreference
//                val pref = p as EditTextPreference?
//                var text = p.sharedPreferences.getString(pref!!.key, "") // 値が更新されない場合があるので、pref.getText() は使用しない
//
//                // 数値型の補正
//                val inputType = pref.editText.inputType
//                try {
//                    if (inputType and InputType.TYPE_CLASS_NUMBER > 0) {
//                        if (inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL > 0) {
//                            // 小数
//                            var `val` = java.lang.Float.valueOf(text)!!
//                            if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
//                                `val` = 0f
//                            }
//                            text = `val`.toString()
//                        } else {
//                            // 整数
//                            var `val` = Integer.valueOf(text)!!
//                            if (inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == 0 && `val` < 0) {
//                                `val` = 0
//                            }
//                            text = `val`.toString()
//                        }
//                    }
//                } catch (e: NumberFormatException) {
//                    text = "0"
//                }
//
//                pref.text = text // 一度値を更新
//                p.setSummary(summary.subSequence(0, summaryLengthMap[p]).toString() + getString(R.string.settings_summary_current_value, text))
//            }
//        }
//
//        companion object {
//
//            /** サマリの長さマップ。  */
//            private val summaryLengthMap = LinkedHashMap<Preference, Int>()
//        }
//
//    }

}
