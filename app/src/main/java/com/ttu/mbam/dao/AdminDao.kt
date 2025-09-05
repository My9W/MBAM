package com.ttu.mbam.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ttu.mbam.model.Admin

@Dao
interface AdminDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAdmin(admin: Admin)

    @Query("SELECT * FROM admins WHERE uid = :uid LIMIT 1")
    suspend fun getAdmin(uid: String): Admin?
}