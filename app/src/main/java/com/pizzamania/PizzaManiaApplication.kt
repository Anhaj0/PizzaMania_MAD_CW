package com.pizzamania

import android.app.Application
import com.pizzamania.push.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PizzaManiaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure default channel exists early (matches Manifest meta-data).
        NotificationHelper.ensureChannel(this)
    }
}
