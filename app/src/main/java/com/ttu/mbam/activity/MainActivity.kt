package com.ttu.mbam.activity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.R
import com.ttu.mbam.activity.SettingsActivity
import com.ttu.mbam.UserAdapter
import com.ttu.mbam.activity.UserDetailActivity
import com.ttu.mbam.dao.UserDao
import com.ttu.mbam.model.User
import com.ttu.mbam.repository.UserRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnSearchUser: ImageView
    private lateinit var rvUsers: RecyclerView
    private lateinit var etNik: EditText
    private lateinit var userAdapter: UserAdapter
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = AppDatabase.Companion.getDatabase(this)
        userDao = database.userDao()

        val repo = UserRepository(userDao, FirebaseFirestore.getInstance())
        repo.startSyncing()


        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnSearchUser = findViewById(R.id.btnSearchUser)
        etNik = findViewById(R.id.etNik)
        rvUsers = findViewById(R.id.rvUsers)
        bottomNavigation.selectedItemId = R.id.nav_home


        userAdapter = UserAdapter(
            listOf(),
            onUserClicked = { user ->
                val intent = Intent(this, UserDetailActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
            },
            onUserEdited = { user ->
                val intent = Intent(this, EditUserActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
            },
            onUserDeleted = { user ->
                showDeleteConfirmationDialog(user)
            }
        )

        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = userAdapter


        userDao.getAllUsers().observe(this) { users ->
            userAdapter.updateUsers(users)
        }


        btnSearchUser.setOnClickListener {
            val nik = etNik.text.toString()
            if (nik.isNotEmpty()) {
                lifecycleScope.launch {
                    val matchedUser = userDao.getUserByNik(nik)
                    if (matchedUser != null) {
                        userAdapter.updateUsers(listOf(matchedUser))
                        Toast.makeText(this@MainActivity, "Menampilkan Hasil Pencarian: $nik", Toast.LENGTH_SHORT).show()
                    } else {
                        userAdapter.updateUsers(emptyList())
                        Toast.makeText(this@MainActivity, "User Tidak Ditemukan: $nik", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                userDao.getAllUsers().observe(this) { users ->
                    userAdapter.updateUsers(users)
                }
                Toast.makeText(this, "Menampilkan Semua User.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Bottom Navigation Clicks
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_users -> {
                    startActivity(Intent(this, AddUserActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmationDialog(user: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hapus Pengguna")
        builder.setMessage("Apakah Anda yakin ingin menghapus user ${user.name}?")

        builder.setPositiveButton("Ya") { _, _ ->
            lifecycleScope.launch {
                try {
                    val repo = UserRepository(userDao, FirebaseFirestore.getInstance())
                    userDao.deleteUser(user)
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.nik)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this@MainActivity, "User ${user.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@MainActivity, "Gagal hapus dari Firestore: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Gagal menghapus user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

}