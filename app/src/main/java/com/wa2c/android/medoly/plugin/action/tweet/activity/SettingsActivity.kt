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

        // Action bar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)
        actionBar.setTitle(R.string.title_activity_settings)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().add(android.R.id.content, SettingsFragment()).commit()
        }
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

}
