package com.ttu.mbam.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.R
import com.ttu.mbam.dao.UserDao
import com.ttu.mbam.model.User
import com.ttu.mbam.repository.UserRepository
import kotlinx.coroutines.launch

class EditUserActivity : AppCompatActivity() {

    private lateinit var etEditNik: EditText
    private lateinit var etEditName: EditText
    private lateinit var etEditAddress: EditText
    private lateinit var btnSaveEdit: Button
    private lateinit var userDao: UserDao
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        etEditNik = findViewById(R.id.etEditNik)
        etEditName = findViewById(R.id.etEditName)
        etEditAddress = findViewById(R.id.etEditAddress)
        btnSaveEdit = findViewById(R.id.btnSaveEdit)

        userDao = AppDatabase.Companion.getDatabase(this).userDao()

        val user = intent.getSerializableExtra("user") as? User
        if (user != null) {
            userId = user.id
            etEditNik.setText(user.nik)
            etEditName.setText(user.name)
            etEditAddress.setText(user.address)
        }

        btnSaveEdit.setOnClickListener { saveChanges() }
    }

    private fun saveChanges() {
        val updatedNik = etEditNik.text.toString().trim()
        val updatedName = etEditName.text.toString().trim()
        val updatedAddress = etEditAddress.text.toString().trim()

        if (updatedNik.length != 16 || !updatedNik.matches(Regex("\\d+"))) {
            Toast.makeText(this, "NIK must be 16 digits!", Toast.LENGTH_SHORT).show()
            return
        }
        if (updatedName.length <= 3) {
            Toast.makeText(this, "Name must be more than 3 characters!", Toast.LENGTH_SHORT).show()
            return
        }
        if (updatedAddress.isEmpty()) {
            Toast.makeText(this, "Address cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val existingUser = userDao.getUserById(userId)
            if (existingUser != null) {
                val updatedUser = existingUser.copy(
                    nik = updatedNik,
                    name = updatedName,
                    address = updatedAddress,
                    updatedAt = System.currentTimeMillis()

                )
                userDao.updateUser(updatedUser)
                val repo = UserRepository(userDao, FirebaseFirestore.getInstance())
                repo.syncUserToFirestore(updatedUser)

                Toast.makeText(this@EditUserActivity, "User updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@EditUserActivity, "User not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}