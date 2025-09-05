package com.ttu.mbam.activity

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.activity.HistoryActivity
import com.ttu.mbam.activity.MainActivity
import com.ttu.mbam.R
import com.ttu.mbam.activity.SettingsActivity
import com.ttu.mbam.model.User
import com.ttu.mbam.repository.UserRepository
import kotlinx.coroutines.launch

class AddUserActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var etNik: EditText
    private lateinit var etName: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSaveUser: Button
    private lateinit var btnCaptureMeter: Button
    private lateinit var imageIv: ImageView
    private lateinit var tvMeterReading: TextView
    private lateinit var btnDetectMeter: Button

    private var imageUri: Uri? = null

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var detectedMeterReading: Int = 0

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            imageUri = result.uriContent
            imageIv.setImageURI(imageUri)
            Toast.makeText(this, "Image cropped successfully!", Toast.LENGTH_SHORT).show()
        } else {
            val error = result.error
            Toast.makeText(this, "Cropping failed: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val userDao by lazy {
        AppDatabase.Companion.getDatabase(this).userDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        etNik = findViewById(R.id.etNik)
        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        btnSaveUser = findViewById(R.id.btnSaveUser)
        btnCaptureMeter = findViewById(R.id.btnCaptureMeter)
        imageIv = findViewById(R.id.imageIv)
        tvMeterReading = findViewById(R.id.tvMeterReading)
        btnDetectMeter = findViewById(R.id.btnDetectMeter)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_users
        btnDetectMeter.setOnClickListener { recognizeTextFromImage() }
        btnSaveUser.setOnClickListener { saveUser() }
        btnCaptureMeter.setOnClickListener { captureImage() }


        requestPermissionsIfNeeded()

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_users -> true
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

    private fun saveUser() {
        val nik = etNik.text.toString().trim()
        val name = etName.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (nik.isEmpty() || name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Tolong isi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        if (nik.length != 16 || !nik.matches(Regex("\\d+"))) {
            Toast.makeText(this, "NIK harus terdiri dari 16 digit angka!", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.length <= 3) {
            Toast.makeText(this, "Nama harus lebih dari 3 huruf!", Toast.LENGTH_SHORT).show()
            return
        }

        if (detectedMeterReading == 0) {
            Toast.makeText(this, "Tolong ambil dan proses gambar meteran terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val newUser = User(
            nik = nik,
            name = name,
            address = address,
            meterReading = detectedMeterReading,
            lastPaymentDate = "N/A"
        )

        lifecycleScope.launch {
            try {
                val db = AppDatabase.Companion.getDatabase(applicationContext)
                val repo = UserRepository(db.userDao(), FirebaseFirestore.getInstance())
                newUser.updatedAt = System.currentTimeMillis()
                repo.insertUser(newUser)

                Toast.makeText(this@AddUserActivity, "User berhasil ditambah!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@AddUserActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddUserActivity, "Error: NIK sudah ada atau masalah penyimpanan!", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun requestPermissionsIfNeeded() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1001
            )
        }
    }

    private fun captureImage() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Meter Image")
            put(MediaStore.Images.Media.DESCRIPTION, "Captured by Camera")
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, 100)
        } else {
            Toast.makeText(this, "Tidak Ada Kamera!", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startCrop()
        }
    }

    private fun startCrop() {
        if (imageUri == null) {
            Toast.makeText(this, "Tolong ambil gambar terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        val cropOptions = CropImageContractOptions(
            uri = imageUri,
            cropImageOptions = CropImageOptions().apply {
                guidelines = CropImageView.Guidelines.ON
            }
        )
        cropImageLauncher.launch(cropOptions)
    }

    private fun recognizeTextFromImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Tolong ambil gambar terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val recognizedText = text.text.filter { it.isDigit() }
                    if (recognizedText.isNotEmpty()) {
                        try {
                            detectedMeterReading = recognizedText.toInt()
                            tvMeterReading.text = "Hasil Deteksi Meteran: $detectedMeterReading"
                            Toast.makeText(this, "Meteran Terdeteksi: $detectedMeterReading", Toast.LENGTH_SHORT).show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(this, "Gagal membaca angka dari hasil OCR!", Toast.LENGTH_SHORT).show()
                            detectedMeterReading = 0
                        }
                    } else {
                        Toast.makeText(this, "Tidak ada angka yang terdeteksi!", Toast.LENGTH_SHORT).show()
                        tvMeterReading.text = "Hasil Deteksi Meteran: -"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AddUserActivity", "Gagal Mendeteksi Gambar: ${e.message}", e)
                    Toast.makeText(this, "Gagal Mendeteksi Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("AddUserActivity", "Error Saat Proses Gambar: ${e.message}", e)
            Toast.makeText(this, "Error Saat Proses Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}