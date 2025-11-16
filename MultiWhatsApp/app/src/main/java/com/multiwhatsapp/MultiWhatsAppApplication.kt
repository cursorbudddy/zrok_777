package com.multiwhatsapp

import android.app.Application
import timber.log.Timber

class MultiWhatsAppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("MultiWhatsApp Application Started")
    }
}
