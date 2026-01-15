package com.yprompt.areyouasleep.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yprompt.areyouasleep.data.model.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyRecord)

    @Update
    suspend fun update(record: DailyRecord)

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsForPeriod(startDate: String, endDate: String): Flow<List<DailyRecord>>
    
    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records ORDER BY date ASC")
    suspend fun getAllRecordsSync(): List<DailyRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DailyRecord>)
}