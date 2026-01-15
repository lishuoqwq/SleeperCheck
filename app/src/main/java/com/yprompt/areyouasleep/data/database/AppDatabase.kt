package com.yprompt.areyouasleep.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.data.model.MonthlyStat

@Database(entities = [DailyRecord::class, MonthlyStat::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun monthlyStatDao(): MonthlyStatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "are_you_asleep_db"
                )
                .fallbackToDestructiveMigration() // Develop only: careful in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}