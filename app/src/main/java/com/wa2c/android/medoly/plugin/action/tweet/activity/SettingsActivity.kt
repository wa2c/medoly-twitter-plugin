package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.wa2c.android.medoly.plugin.action.tweet.R

/**
 * Settings activity
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Action bar
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)

            // title
            supportFragmentManager.addOnBackStackChangedListener {
                title = if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.getBackStackEntryAt(0).name
                } else {
                    getString(R.string.title_activity_settings)
                }
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(android.R.id.content, SettingsFragment()).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
