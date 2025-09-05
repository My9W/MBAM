package com.ttu.mbam.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.dao.AdminDao
import com.ttu.mbam.model.Admin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminRepository(
    private val adminDao: AdminDao,
    private val firestore: FirebaseFirestore
) {

    fun syncAdminToFirestore(admin: Admin) {
        firestore.collection("admins").document(admin.uid).set(admin)
    }


    suspend fun saveAdminLocally(admin: Admin) {
        adminDao.insertAdmin(admin)
    }


    suspend fun saveAdmin(admin: Admin) {
        saveAdminLocally(admin)
        syncAdminToFirestore(admin)
    }

    //blm di pakai
    fun fetchAdminFromFirestore(uid: String, onComplete: (Admin?) -> Unit) {
        firestore.collection("admins").document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val admin = snapshot.toObject(Admin::class.java)
                if (admin != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        adminDao.insertAdmin(admin)
                    }
                }
                onComplete(admin)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    suspend fun getAdmin(uid: String): Admin? {
        return adminDao.getAdmin(uid)
    }

    fun listenToAdminChanges(uid: String) {
        firestore.collection("admins").document(uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(Admin::class.java)?.let { admin ->
                    CoroutineScope(Dispatchers.IO).launch {
                        adminDao.insertAdmin(admin)
                    }
                }
            }
    }
}