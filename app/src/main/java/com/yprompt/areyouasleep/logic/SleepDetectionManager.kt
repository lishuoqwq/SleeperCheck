package com.yprompt.areyouasleep.logic

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.yprompt.areyouasleep.data.preferences.UserPreferences
import java.util.Calendar

class SleepDetectionManager(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun hasUsageStatsPermission(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Analyzes usage stats for a given night to determine if the user stayed up late.
     * This is a retrospective query and should be called the day after.
     *
     * @param analysisDate The date to analyze (e.g., "2024-10-27"). The function will check the night of the 27th to the morning of the 28th.
     * @param prefs The current user preferences to define sleep and stay-up-late times.
     * @return A [SleepAnalysisResult] indicating if the user stayed up late and the last detected interaction time.
     */
    suspend fun analyzeSleep(analysisDate: Calendar, prefs: UserPreferences): SleepAnalysisResult {
        if (!hasUsageStatsPermission()) {
            return SleepAnalysisResult.PermissionDenied
        }

        // Define the query window: from target sleep time on analysisDate to 4 AM the next day.
        val startTime = (analysisDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, prefs.targetSleepTimeHour)
            set(Calendar.MINUTE, prefs.targetSleepTimeMinute)
            set(Calendar.SECOND, 0)
        }

        val endTime = (analysisDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 4) // Hardcoded to 4 AM for simplicity, can be made configurable
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        // Define the "stay up late" threshold time
        val stayUpLateTime = (analysisDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1) // It's the next morning
            set(Calendar.HOUR_OF_DAY, prefs.stayUpLateThresholdHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val usageEvents = usageStatsManager.queryEvents(startTime.timeInMillis, endTime.timeInMillis)
        
        var lastInteractionTime: Long? = null
        var didStayUpLate = false

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            // We only care about events that indicate active user interaction
            if (event.eventType == UsageEvents.Event.USER_INTERACTION ||
                event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                
                lastInteractionTime = event.timeStamp

                // If the interaction happened after the stay-up-late threshold, mark it.
                if (event.timeStamp > stayUpLateTime.timeInMillis) {
                    didStayUpLate = true
                }
            }
        }

        return SleepAnalysisResult.Success(
            isStayUpLate = didStayUpLate,
            lastInteractionTime = lastInteractionTime
        )
    }
}

sealed class SleepAnalysisResult {
    data class Success(val isStayUpLate: Boolean, val lastInteractionTime: Long?) : SleepAnalysisResult()
    object PermissionDenied : SleepAnalysisResult()
}