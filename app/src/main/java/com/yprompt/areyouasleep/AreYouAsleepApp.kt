package com.yprompt.areyouasleep

import android.app.Application
import com.yprompt.areyouasleep.worker.MorningCheckInWorker

class AreYouAsleepApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Schedule the daily check-in worker when the application starts.
        // This ensures the worker is enqueued even if the user doesn't open the app daily.
        MorningCheckInWorker.schedule(this)
    }
}