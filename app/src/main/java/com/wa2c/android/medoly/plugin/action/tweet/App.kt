package com.wa2c.android.medoly.plugin.action.tweet

import android.app.Application
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

        // Migrator
        Migrator(this).versionUp()
    }

}
