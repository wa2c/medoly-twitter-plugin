package com.wa2c.android.medoly.plugin.action.tweet

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.wa2c.android.medoly.library.PluginOperationCategory
import com.wa2c.android.medoly.plugin.action.tweet.util.logE
import com.wa2c.android.prefs.Prefs

/**
 * Migrator
 */
class Migrator(private val context: Context) {
    private val prefs = Prefs(context)

    /**
     * Get current app version code.
     * @return Current version.
     */
    private val currentVersionCode: Int
        get() {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            } catch (e: Exception) {
                logE(e)
                0
            }
        }

    /**
     * Get saved app version code. (previous version)
     * @return Saved version.
     */
    private val savedVersionCode: Int
        get() = prefs.getInt(R.string.prefkey_app_version_code, 0)

    /**
     * Save current version.
     */
    private fun saveCurrentVersionCode() {
        val version = currentVersionCode
        prefs[R.string.prefkey_app_version_code] = version
    }



    /**
     * Version up.
     */
    fun versionUp(): Boolean {
        val prevVersionCode = savedVersionCode
        val currentVersionCode = currentVersionCode

        if (currentVersionCode <= prevVersionCode)
            return false

        // migration
        versionUpFrom10(prevVersionCode)

        // save version
        saveCurrentVersionCode()
        return true
    }

    /**
     * Ver > Ver. 2.2.0 (10)
     */
    private fun versionUpFrom10(prevVersionCode: Int) {
        if (prevVersionCode > 10)
            return

        // Get Lyrics
        val albumArt: String? = prefs[R.string.prefkey_event_tweet_operation]
        if (albumArt == "OPERATION_MEDIA_OPEN")
            prefs[R.string.prefkey_event_tweet_operation] = PluginOperationCategory.OPERATION_MEDIA_OPEN.categoryValue
        else if (albumArt == "OPERATION_PLAY_START")
            prefs[R.string.prefkey_event_tweet_operation] = PluginOperationCategory.OPERATION_PLAY_START.categoryValue
    }

}
