package com.yprompt.areyouasleep.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey
    val date: String, // Format: "YYYY-MM-DD"

    @ColumnInfo(name = "did_check_in")
    val didCheckIn: Boolean = false,

    @ColumnInfo(name = "check_in_timestamp")
    val checkInTimestamp: Long? = null,

    @ColumnInfo(name = "is_stay_up_late")
    val isStayUpLate: Boolean = false,

    @ColumnInfo(name = "last_screen_off_time")
    val lastScreenOffTime: Long? = null,

    @ColumnInfo(name = "note")
    val note: String? = null
)