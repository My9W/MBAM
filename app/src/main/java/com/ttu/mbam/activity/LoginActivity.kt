package com.ttu.mbam.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.activity.MainActivity
import com.ttu.mbam.R
import com.ttu.mbam.activity.SignUpActivity
import com.ttu.mbam.dao.UserDao
import com.ttu.mbam.model.Admin
import com.ttu.mbam.model.User
import com.ttu.mbam.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)
        userDao = AppDatabase.Companion.getDatabase(this).userDao()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        tvSignUp = findViewById(R.id.tvSignup)

        btnLogin.setOnClickListener {
            val input = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/NIK dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isEmail = Patterns.EMAIL_ADDRESS.matcher(input).matches()

            if (isEmail) {
                // Admin login with Firebase Auth
                auth.signInWithEmailAndPassword(input, password)
                    .addOnSuccessListener { navigateToMainActivity() }
                    .addOnFailureListener {
                        Toast.makeText(this, "Login admin gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Client login with NIK (NIK = password)
                CoroutineScope(Dispatchers.IO).launch {
                    var user = userDao.getUserByNik(input)
                    if (user == null) {
                        // Try to fetch from Firestore
                        val snapshot = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(input)
                            .get()
                            .await()

                        if (snapshot.exists()) {
                            user = snapshot.toObject(User::class.java)
                            user?.let { userDao.insertUser(it) } // Save to local DB
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (user != null && password == input) {
                            val intent = Intent(this@LoginActivity, ClientActivity::class.java)
                            intent.putExtra("client_nik", user!!.nik)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Login client gagal: NIK tidak ditemukan atau salah",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        btnGoogleLogin.setOnClickListener { signInWithGoogle() }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val googleCredential = GoogleAuthProvider.getCredential(credential.googleIdToken, null)

                    auth.signInWithCredential(googleCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.let {
                                    val admin = Admin(
                                        uid = it.uid,
                                        name = it.displayName ?: "Admin",
                                        email = it.email ?: "",
                                        profilePicture = it.photoUrl?.toString()
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
                                navigateToMainActivity()
                            } else {
                                Toast.makeText(this, "Google Sign-In gagal!", Toast.LENGTH_SHORT).show()
                            }
                        }
                } catch (e: Exception) {
                    Log.e("GoogleSignIn", "Error: ${e.message}")
                }
            }
        }

    private fun signInWithGoogle() {
        val request = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            ).build()

        oneTapClient.beginSignIn(request)
            .addOnSuccessListener { result ->
                googleSignInLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener {
                Log.e("GoogleSignIn", "Error: ${it.message}")
            }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}