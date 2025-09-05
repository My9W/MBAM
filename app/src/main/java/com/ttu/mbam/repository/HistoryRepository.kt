package com.ttu.mbam.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.dao.HistoryDao
import com.ttu.mbam.model.History
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryRepository(
    private val historyDao: HistoryDao,
    private val firestore: FirebaseFirestore
) {
    suspend fun insertHistory(history: History) {
        val item = history.copy(updatedAt = System.currentTimeMillis())
        historyDao.insertHistory(item)
        syncToFirestore(item)
    }

    fun syncToFirestore(history: History) {
        firestore.collection("history")
            .document(history.id.toString())
            .set(history)
            .addOnSuccessListener {
                Log.d("HistoryRepo", "Synced to Firestore")
            }
            .addOnFailureListener {
                Log.e("HistoryRepo", "Sync failed: ${it.message}")
            }
    }

    fun startSyncing() {
        CoroutineScope(Dispatchers.IO).launch {
            // Ambil semua dari lokal
            val localHistory = historyDao.getAllHistoryOnce() // <- kamu perlu bikin ini di DAO

            localHistory.forEach { local ->
                val docRef = firestore.collection("history").document(local.id.toString())
                docRef.get().addOnSuccessListener { snapshot ->
                    val remote = snapshot.toObject(History::class.java)
                    if (remote == null || local.updatedAt > (remote.updatedAt ?: 0L)) {
                        docRef.set(local)
                    }
                }.addOnFailureListener {
                    Log.e("HistoryRepo", "Failed to get remote: ${it.message}")
                }
            }

            // Dengarkan perubahan dari remote
            firestore.collection("history").addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val updates = snapshot.toObjects(History::class.java)
                        updates.forEach { historyDao.insertHistory(it) }
                    }
                }
            }
        }
    }

}