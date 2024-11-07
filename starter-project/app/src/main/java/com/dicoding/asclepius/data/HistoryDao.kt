package com.dicoding.asclepius.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: History)

    @Query("SELECT * FROM history ORDER BY id DESC")
    suspend fun getAllHistories(): List<History>
}
