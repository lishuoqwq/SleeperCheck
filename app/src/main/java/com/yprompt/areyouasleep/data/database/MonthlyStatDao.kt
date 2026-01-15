package com.yprompt.areyouasleep.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yprompt.areyouasleep.data.model.MonthlyStat
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: MonthlyStat)

    @Query("SELECT * FROM monthly_stats WHERE month = :month LIMIT 1")
    fun getStatByMonth(month: String): Flow<MonthlyStat?>

    @Query("SELECT * FROM monthly_stats ORDER BY month DESC")
    fun getAllStats(): Flow<List<MonthlyStat>>
}