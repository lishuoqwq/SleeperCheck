package com.yprompt.areyouasleep.logic

import android.content.Context
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.data.model.MonthlyStat
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsManager(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dailyRecordDao = db.dailyRecordDao()
    private val monthlyStatDao = db.monthlyStatDao()

    suspend fun updateMonthlyStats(date: String) {
        val month = date.substring(0, 7) // Extract "YYYY-MM"

        val startDate = "$month-01"
        val endDate = "$month-31"

        val records = dailyRecordDao.getRecordsForPeriod(startDate, endDate).first()

        val totalCheckIns = records.count { it.didCheckIn }
        val totalStayUpLateDays = records.count { it.isStayUpLate }
        val maxConsecutive = calculateMaxConsecutiveCheckIns(records)
        val bitmap = generateStatusBitmap(records, month)

        val stat = MonthlyStat(
            month = month,
            totalCheckIns = totalCheckIns,
            totalStayUpLateDays = totalStayUpLateDays,
            maxConsecutiveCheckIns = maxConsecutive,
            statusBitmap = bitmap
        )

        monthlyStatDao.insertOrUpdate(stat)
    }

    private fun calculateMaxConsecutiveCheckIns(records: List<DailyRecord>): Int {
        if (records.isEmpty()) return 0

        val sortedRecords = records.sortedBy { it.date }
        var maxStreak = 0
        var currentStreak = 0

        for (record in sortedRecords) {
            if (record.didCheckIn) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return maxStreak
    }

    private fun generateStatusBitmap(records: List<DailyRecord>, month: String): String {
        val daysInMonth = getDaysInMonth(month)
        val bitmap = CharArray(daysInMonth) { '0' }

        for (record in records) {
            val day = record.date.substring(8, 10).toInt() - 1
            if (day in bitmap.indices) {
                bitmap[day] = when {
                    !record.didCheckIn -> '0'
                    record.isStayUpLate -> '2'
                    else -> '1'
                }
            }
        }

        return String(bitmap)
    }

    private fun getDaysInMonth(month: String): Int {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM", Locale.US)
        calendar.time = format.parse(month) ?: return 31
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
