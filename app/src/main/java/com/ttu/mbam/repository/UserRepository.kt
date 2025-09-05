package com.ttu.mbam.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.dao.UserDao
import com.ttu.mbam.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) {
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        syncUserToFirestore(user)
    }

    fun syncUserToFirestore(user: User) {
        firestore.collection("users")
            .document(user.nik)
            .set(user)
            .addOnSuccessListener {
                Log.d("UserRepo", "Synced to Firestore")
            }
            .addOnFailureListener {
                Log.e("UserRepo", "Sync failed: ${it.message}")
            }
    }

    fun startSyncing() {
        CoroutineScope(Dispatchers.IO).launch {
            val localUsers = userDao.getAllUsersOnce()
            if (localUsers.isEmpty()) {
                fetchAllFromFirestore()  // now it will actually await until done
            } else {
                pushLocalChangesToFirestore(localUsers)
            }
        }
        listenToAllUsersRealtime()
    }

    private suspend fun fetchAllFromFirestore() {
        try {
            val snapshot = firestore.collection("users").get().await()
            val fetchedUsers = snapshot.toObjects(User::class.java)
            fetchedUsers.forEach { remoteUser ->
                userDao.insertUser(remoteUser)
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "Failed to fetch users: ${e.message}")
        }
    }


    private suspend fun pushLocalChangesToFirestore(localUsers: List<User>) {
        localUsers.forEach { user ->
            val docRef = firestore.collection("users").document(user.nik)
            docRef.get().addOnSuccessListener { snapshot ->
                val remoteUser = snapshot.toObject(User::class.java)
                if (remoteUser == null || user.updatedAt > (remoteUser.updatedAt ?: 0L)) {
                    docRef.set(user)
                }
            }
        }
    }

    fun listenToAllUsersRealtime() {
        firestore.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && !snapshot.isEmpty) {
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedUsers = snapshot.toObjects(User::class.java)
                    updatedUsers.forEach { remoteUser ->
                        val localUser = userDao.getUserByNik(remoteUser.nik)
                        if (localUser == null || remoteUser.updatedAt > (localUser.updatedAt)) {
                            userDao.insertUser(remoteUser)
                        }
                    }
                }
            }
        }
    }
    suspend fun getUserByNik(nik: String): User? {
        return userDao.getUserByNik(nik)
    }
}