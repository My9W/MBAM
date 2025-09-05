package com.ttu.mbam.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ttu.mbam.model.History

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertHistory(history: History)

    @Delete
    fun deleteHistory(history: History)

    @Query("SELECT * FROM history WHERE strftime('%Y-%m', date) = :month ORDER BY date DESC")
    fun getHistoryByMonth(month: String): LiveData<List<History>>

    @Query("SELECT * FROM history WHERE nik = :nik ORDER BY date DESC")
    fun getHistoryByNik(nik: String): LiveData<List<History>>

    @Query("SELECT * FROM history")
    suspend fun getAllHistoryOnce(): List<History>

    @Query("SELECT * FROM history ORDER BY date DESC")
    fun getAllHistory(): LiveData<List<History>>
}