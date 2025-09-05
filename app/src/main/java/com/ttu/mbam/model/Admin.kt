package com.ttu.mbam.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admins")
data class Admin(
    @PrimaryKey(autoGenerate = false)
    val uid: String, // Unique ID (Google UID or email)
    val name: String,
    val email: String,
    val profilePicture: String? = null
)