package com.ttu.mbam.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "history")
data class History(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val nik: String = "",
    val oldReading: Int = 0,
    val newReading: Int = 0,
    val usage: Int = 0,
    val totalPrice: Double = 0.0,
    val date: String = "",
    val updatedAt: Long = 0L
)