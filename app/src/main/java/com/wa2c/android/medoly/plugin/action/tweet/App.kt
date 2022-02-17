package com.wa2c.android.medoly.plugin.action.tweet

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.wa2c.android.medoly.plugin.action.tweet.service.AbstractPluginService
import timber.log.Timber

/**
 * App
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            //Timber.plant(CrashlyticsTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }

        // Create channel
        AbstractPluginService.createChannel(this)
        // Migrator
        Migrator(this).versionUp()
    }

}
