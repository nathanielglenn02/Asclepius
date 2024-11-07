package com.dicoding.asclepius.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val prediction: String,
    val confidence: Float
)
