package com.ttu.mbam.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.R
import com.ttu.mbam.model.Admin
import com.ttu.mbam.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var btnGoogleSignUp: Button
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerWithEmail(email, password)
            } else {
                Toast.makeText(this, "Email dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogleSignUp.setOnClickListener {
            signUpWithGoogle()
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }


    private fun registerWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.let { user ->
                        val admin = Admin(
                            uid = user.uid,
                            name = user.displayName ?: "Admin",
                            email = user.email ?: "",
                            profilePicture = user.photoUrl?.toString()
                        )

                        val database = AppDatabase.Companion.getDatabase(applicationContext)
                        val repo =
                            AdminRepository(database.adminDao(), FirebaseFirestore.getInstance())

                        CoroutineScope(Dispatchers.IO).launch {
                            repo.saveAdmin(admin)
                        }
                    }

                    Toast.makeText(this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Pendaftaran gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val googleSignUpLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val credential = Identity.getSignInClient(this).getSignInCredentialFromIntent(result.data)
                val googleCredential = GoogleAuthProvider.getCredential(credential.googleIdToken, null)

                auth.signInWithCredential(googleCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            currentUser?.let { user ->
                                val admin = Admin(
                                    uid = user.uid,
                                    name = user.displayName ?: "Admin",
                                    email = user.email ?: "",
                                    profilePicture = user.photoUrl?.toString()
                                )

                                val database = AppDatabase.Companion.getDatabase(applicationContext)
                                val repo = AdminRepository(
                                    database.adminDao(),
                                    FirebaseFirestore.getInstance()
                                )

                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.saveAdmin(admin)
                                }
                            }

                            Toast.makeText(this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Google Sign-Up gagal!", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-Up gagal!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signUpWithGoogle() {
        val request = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        Identity.getSignInClient(this).beginSignIn(request)
            .addOnSuccessListener { result ->
                googleSignUpLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
            }
    }
}