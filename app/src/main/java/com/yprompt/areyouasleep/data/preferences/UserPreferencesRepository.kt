package com.yprompt.areyouasleep.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object Keys {
        val TARGET_SLEEP_TIME_HOUR = intPreferencesKey("target_sleep_time_hour")
        val TARGET_SLEEP_TIME_MINUTE = intPreferencesKey("target_sleep_time_minute")
        val STAY_UP_LATE_THRESHOLD_HOUR = intPreferencesKey("stay_up_late_threshold_hour")
        val IS_DAILY_REMINDER_ENABLED = booleanPreferencesKey("is_daily_reminder_enabled")
        val REMINDER_TIME_HOUR = intPreferencesKey("reminder_time_hour")
        val REMINDER_TIME_MINUTE = intPreferencesKey("reminder_time_minute")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .map { preferences ->
            UserPreferences(
                targetSleepTimeHour = preferences[Keys.TARGET_SLEEP_TIME_HOUR] ?: 23,
                targetSleepTimeMinute = preferences[Keys.TARGET_SLEEP_TIME_MINUTE] ?: 0,
                stayUpLateThresholdHour = preferences[Keys.STAY_UP_LATE_THRESHOLD_HOUR] ?: 1,
                isDailyReminderEnabled = preferences[Keys.IS_DAILY_REMINDER_ENABLED] ?: true,
                reminderTimeHour = preferences[Keys.REMINDER_TIME_HOUR] ?: 22,
                reminderTimeMinute = preferences[Keys.REMINDER_TIME_MINUTE] ?: 30
            )
        }

    suspend fun updateTargetSleepTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.TARGET_SLEEP_TIME_HOUR] = hour
            preferences[Keys.TARGET_SLEEP_TIME_MINUTE] = minute
        }
    }
    
    suspend fun updateStayUpLateThreshold(hour: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.STAY_UP_LATE_THRESHOLD_HOUR] = hour
        }
    }

    suspend fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_DAILY_REMINDER_ENABLED] = enabled
            preferences[Keys.REMINDER_TIME_HOUR] = hour
            preferences[Keys.REMINDER_TIME_MINUTE] = minute
        }
    }
}

data class UserPreferences(
    val targetSleepTimeHour: Int,
    val targetSleepTimeMinute: Int,
    val stayUpLateThresholdHour: Int,
    val isDailyReminderEnabled: Boolean,
    val reminderTimeHour: Int,
    val reminderTimeMinute: Int
)