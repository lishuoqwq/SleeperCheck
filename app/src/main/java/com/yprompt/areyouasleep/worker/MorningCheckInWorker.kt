package com.yprompt.areyouasleep.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yprompt.areyouasleep.R
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.data.preferences.UserPreferencesRepository
import com.yprompt.areyouasleep.logic.SleepAnalysisResult
import com.yprompt.areyouasleep.logic.SleepDetectionManager
import com.yprompt.areyouasleep.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MorningCheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getDatabase(context)
    private val userPrefsRepo = UserPreferencesRepository(context)
    private val sleepManager = SleepDetectionManager(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val analysisResult = sleepManager.analyzeSleep(yesterday, userPrefsRepo.userPreferences.first())

        if (analysisResult is SleepAnalysisResult.Success) {
            val yesterdayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday.time)
            
            // Save the analysis result to the database
            val record = DailyRecord(
                date = yesterdayDateString,
                isStayUpLate = analysisResult.isStayUpLate,
                lastScreenOffTime = analysisResult.lastInteractionTime,
                didCheckIn = false // User has not confirmed yet
            )
            db.dailyRecordDao().insertOrUpdate(record)

            // Create a notification for the user
            createNotification(analysisResult.isStayUpLate)
            return Result.success()
        }
        
        return Result.retry()
    }

    private fun createNotification(didStayUpLate: Boolean) {
        val channelId = "sleep_check_in_channel"
        val channelName = "Sleep Check-in"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val title = if (didStayUpLate) "昨晚熬夜了吗?" else "昨晚睡得不错!"
        val text = "点击确认，记录你的睡眠习惯。"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bed) // Placeholder icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    companion object {
        private const val WORK_NAME = "MorningCheckIn"

        fun schedule(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresCharging(false)
                .build()

            // Run once a day
            val workRequest = PeriodicWorkRequestBuilder<MorningCheckInWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                // Set initial delay to run around 8 AM tomorrow
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        private fun calculateInitialDelay(): Long {
            val now = Calendar.getInstance()
            val tomorrow8AM = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            return tomorrow8AM.timeInMillis - now.timeInMillis
        }
    }
}