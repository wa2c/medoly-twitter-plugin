package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.wa2c.android.medoly.plugin.action.tweet.R

/**
 * Main activity
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    /**
     * Back stack change listener
     */
    private val backStackChangedListener = FragmentManager.OnBackStackChangedListener {
        // Tool bar
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
            val isHome = it is MainFragment
            supportActionBar?.setDisplayShowHomeEnabled(isHome)
            supportActionBar?.setDisplayHomeAsUpEnabled(!isHome)
        }
        // Title
        val backStackCount = supportFragmentManager.backStackEntryCount
        if (backStackCount > 0) {
            val name = supportFragmentManager.getBackStackEntryAt(backStackCount - 1).name
            if (!name.isNullOrEmpty()) title = name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ActionBar
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setIcon(R.drawable.ic_launcher)
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, MainFragment())
                    .commit()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Authorization
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? MainFragment)?.completeAuthorize(intent)
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

    override fun onStart() {
        super.onStart()
        supportFragmentManager.addOnBackStackChangedListener(backStackChangedListener)

    }

    override fun onStop() {
        super.onStop()
        supportFragmentManager.removeOnBackStackChangedListener(backStackChangedListener)
    }

}
