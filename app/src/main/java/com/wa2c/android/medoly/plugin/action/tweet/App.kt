package com.wa2c.android.medoly.plugin.action.tweet

import android.app.Application
import android.content.Context
import timber.log.Timber


/**
 * App
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        versionUp(this)
    }

    /**
     * Version up.
     * @param context Context.
     */
    private fun versionUp(context: Context) {
        // Migration
    }
}
