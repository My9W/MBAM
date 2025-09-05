package com.ttu.mbam.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var nik: String = "",
    var name: String = "",
    var address: String = "",
    var meterReading: Int = 0,
    var lastPaymentDate: String = "",
    var usageThisMonth: Int = 0,
    var updatedAt: Long = 0L
) : Serializable {
    // Firestore requires this no-arg constructor
    constructor() : this(0, "", "", "", 0, "", 0, 0L)
}