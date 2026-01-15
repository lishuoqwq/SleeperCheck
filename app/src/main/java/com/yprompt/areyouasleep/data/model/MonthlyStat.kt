package com.yprompt.areyouasleep.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_stats")
data class MonthlyStat(
    @PrimaryKey
    val month: String, // Format: "YYYY-MM"

    @ColumnInfo(name = "total_check_ins")
    val totalCheckIns: Int = 0,

    @ColumnInfo(name = "total_stay_up_late_days")
    val totalStayUpLateDays: Int = 0,

    @ColumnInfo(name = "max_consecutive_check_ins")
    val maxConsecutiveCheckIns: Int = 0,

    @ColumnInfo(name = "status_bitmap")
    val statusBitmap: String
)