package com.wa2c.android.medoly.plugin.action.tweet.activity.component

import androidx.annotation.StringRes
import androidx.preference.*
import com.wa2c.android.medoly.plugin.action.tweet.R


fun PreferenceFragmentCompat.setListener(@StringRes prefKeyRes: Int, listener: (() -> Unit)) {
    (findPreference(getString(prefKeyRes)) as? Preference)?.setOnPreferenceClickListener {
        listener.invoke()
        true
    }
}

fun <T : Preference> PreferenceFragmentCompat.preference(@StringRes prefKeyRes: Int): T? {
    return preference(getString(prefKeyRes))
}

fun <T : Preference> PreferenceFragmentCompat.preference(prefKey: String): T? {
    return findPreference(prefKey)
}

fun PreferenceFragmentCompat.updatePrefSummary(@StringRes prefKeyRes: Int, force: Boolean = false) {
    this.updatePrefSummary(requireContext().getString(prefKeyRes), force)
}

/**
 * Initialize summary.
 * @param p target item.
 */
fun PreferenceFragmentCompat.initSummary(p: Preference) {
    // update summary
    when (p) {
        is PreferenceCategory -> {
            for (i in 0 until p.preferenceCount) {
                initSummary(p.getPreference(i))
            }
        }
        is PreferenceScreen -> {
            for (i in 0 until p.preferenceCount) {
                initSummary(p.getPreference(i))
            }
        }
        else -> updatePrefSummary(p.key)
    }
}

fun PreferenceFragmentCompat.updatePrefSummary(prefKey: String, force: Boolean = false) {
    val p = preference(prefKey) as? Preference ?: return

    // for instance type
    when (p) {
        is CheckBoxPreference -> {
            return
        }
        is ListPreference -> {
            // ListPreference
            p.value = p.sharedPreferences.getString(p.key, "")
            p.setSummaryValue(p.entry)
        }
        is MultiSelectListPreference -> {
            // MultiSelectListPreference
            val stringSet = p.sharedPreferences.getStringSet(p.key, null)
            var text = ""
            if (stringSet != null && stringSet.size > 0) {
                p.values = stringSet // update value once
                val builder = StringBuilder()
                (p.entries.indices)
                    .filter { stringSet.contains(p.entryValues[it]) }
                    .forEach { builder.append(p.entries[it]).append(",") }
                if (builder.isNotEmpty()) {
                    text = builder.substring(0, builder.length - 1) // remove end comma
                }
            }
            p.setSummaryValue(text)
        }
        is EditTextPreference -> {
            // EditTextPreference
            val text = p.sharedPreferences.getString(p.key, "")
            p.text = text // update once
            p.setSummaryValue(text)
        }
        else -> {
            if (force) {
                val text = p.sharedPreferences.getString(p.key, "")
                p.setSummaryValue(text)
            }
        }
    }
}

/**
 * Add value on preference summary.
 */
private fun Preference.setSummaryValue(value: CharSequence?) {
    val mask = "********"
    val escapeMask = """\*\*\*\*\*\*\*\*"""
    val literal = (this.context.getString(R.string.settings_summary_current_value, mask)).let {
        val from = Regex("(?!\\\\)(\\W)").replace(it, "\\\\\$1") // NOTE: Escape regex characters
        from.replace(escapeMask, ".*")
    }
    val plainSummary = Regex("\n$literal$").replace(this.summary, "")
    val valueSummary = this.context.getString(R.string.settings_summary_current_value, value ?: "")
    this.summary = plainSummary + "\n" + valueSummary
}
