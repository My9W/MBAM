package com.ttu.mbam.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ttu.mbam.model.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): LiveData<List<User>>

    @Query("SELECT * FROM users WHERE nik = :nik LIMIT 1")
    suspend fun getUserByNik(nik: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Update
    suspend fun updateUser(user: User)
    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<User>

    @Delete
    suspend fun deleteUser(user: User)

    companion object {
        fun updateUser(user: User) {

        }
    }
}