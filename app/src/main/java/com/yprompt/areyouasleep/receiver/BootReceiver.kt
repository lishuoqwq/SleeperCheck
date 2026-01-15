package com.yprompt.areyouasleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yprompt.areyouasleep.data.preferences.UserPreferencesRepository
import com.yprompt.areyouasleep.logic.ReminderManager
import com.yprompt.areyouasleep.worker.MorningCheckInWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            MorningCheckInWorker.schedule(context)

            CoroutineScope(Dispatchers.IO).launch {
                val prefs = UserPreferencesRepository(context).userPreferences.first()
                ReminderManager(context).scheduleReminder(prefs)
            }
        }
    }
}