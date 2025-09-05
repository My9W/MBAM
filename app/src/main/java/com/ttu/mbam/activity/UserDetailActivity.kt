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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.MeterDetectionHelper
import com.ttu.mbam.R
import com.ttu.mbam.model.History
import com.ttu.mbam.model.User
import com.ttu.mbam.repository.HistoryRepository
import com.ttu.mbam.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDetailActivity : AppCompatActivity() {

    private lateinit var tvUserInfo: TextView
    private lateinit var imageIv: ImageView
    private lateinit var btnTakeImage: Button
    private lateinit var btnRecognizeText: Button
    private lateinit var tvRecognizedText: TextView
    private lateinit var btnUpdateMeterReading: Button
    private lateinit var etPricePerUnit: EditText
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnCalculatePrice: Button

    private var imageUri: Uri? = null
    private lateinit var user: User
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        tvUserInfo = findViewById(R.id.tvUserInfo)
        imageIv = findViewById(R.id.imageIv)
        btnTakeImage = findViewById(R.id.btnTakeImage)
        btnRecognizeText = findViewById(R.id.btnRecognizeText)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        btnUpdateMeterReading = findViewById(R.id.btnUpdateMeterReading)
        btnCalculatePrice = findViewById(R.id.btnCalculatePrice)
        etPricePerUnit = findViewById(R.id.etPricePerUnit)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)

        requestPermissionsIfNeeded()

        user = intent.getSerializableExtra("user") as User
        displayUserInfo()

        btnTakeImage.setOnClickListener {
            showImageSourceDialog()
        }
        btnRecognizeText.setOnClickListener { recognizeTextFromImage() }
        btnUpdateMeterReading.setOnClickListener {
            showConfirmationDialog("Apakah Anda yakin ingin mengupdate pembacaan meteran?") {
                updateMeterReading()
            }
        }
        btnCalculatePrice.setOnClickListener {
            showConfirmationDialog("Apakah Anda yakin ingin menghitung total harga?") {
                calculateAndDisplayPrice()
            }
        }
    }
    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil dari Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> captureImage()         // Kamera
                    1 -> pickImageFromGallery() // Galeri
                }
            }
            .show()
    }
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }

    private fun showConfirmationDialog(message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage(message)
            .setPositiveButton("Ya") { _, _ -> onConfirm() }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun displayUserInfo() {
        tvUserInfo.text = """
            NIK : ${user.nik}
            Nama : ${user.name}
            Alamat : ${user.address}
            Meteran Saat Ini: ${user.meterReading}
            Terakhir Pembayaran : ${user.lastPaymentDate}
            Penggunaan Bulan Ini : ${user.usageThisMonth}
        """.trimIndent()
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
            Toast.makeText(this, "Tidak Ada Kamera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                100 -> { // Dari kamera
                    imageUri?.let {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                        val meterDetector = MeterDetectionHelper(this)

                        if (meterDetector.isMeterDetected(bitmap)) {
                            startCrop()
                        } else {
                            Toast.makeText(this, "Foto bukan meteran air! Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                            imageUri = null
                        }
                    }
                }

                101 -> { // Dari galeri
                    imageUri = data?.data
                    imageUri?.let {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                        val meterDetector = MeterDetectionHelper(this)

                        if (meterDetector.isMeterDetected(bitmap)) {
                            startCrop()
                        } else {
                            Toast.makeText(this, "Gambar yang dipilih bukan meteran air!", Toast.LENGTH_SHORT).show()
                            imageUri = null
                        }
                    }
                }
            }
        }
    }


    private fun startCrop() {
        if (imageUri == null) {
            Toast.makeText(this, "Tolong Ambil Gambar Terlebih Dahulu!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Tolong Ambil Gambar Terlebih Dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            val startTime = System.currentTimeMillis()

            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val duration = System.currentTimeMillis() - startTime
                    Log.d("PERFORMANCE_TEST", "recognizeTextFromImage selesai dalam ${duration}ms")

                    val recognizedText = text.text.filter { it.isDigit() }
                    tvRecognizedText.text = if (recognizedText.isNotEmpty()) {
                        "Deteksi Meteran: $recognizedText"
                    } else {
                        "Tidak Ada Angka Dalam Gambar!"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal Mendeteksi Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saat Proses Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateMeterReading() {
        if (imageUri == null) {
            Toast.makeText(this, "Tolong Ambil Gambar Terlebih Dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            val startTime = System.currentTimeMillis()

            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val durationOcr = System.currentTimeMillis() - startTime
                    Log.d("PERFORMANCE_TEST", "updateMeterReading OCR selesai dalam ${durationOcr}ms")

                    val recognizedText = text.text.filter { it.isDigit() }

                    if (recognizedText.isNotEmpty()) {
                        val recognizedMeterReading = recognizedText.toIntOrNull()
                        if (recognizedMeterReading != null) {
                            val oldReading = user.meterReading
                            val usage = if (recognizedMeterReading >= oldReading) {
                                recognizedMeterReading - oldReading
                            } else 0

                            user.meterReading = recognizedMeterReading
                            user.lastPaymentDate = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(Date())
                            user.usageThisMonth = usage
                            user.updatedAt = System.currentTimeMillis()

                            val startSave = System.currentTimeMillis()
                            lifecycleScope.launch {
                                try {
                                    val database = AppDatabase.Companion.getDatabase(this@UserDetailActivity)
                                    database.userDao().updateUser(user)

                                    // Firestore Sync
                                    val repo = UserRepository(
                                        database.userDao(),
                                        FirebaseFirestore.getInstance()
                                    )
                                    repo.syncUserToFirestore(user)

                                    displayUserInfo()
                                    val durationSave = System.currentTimeMillis() - startSave
                                    Log.d("PERFORMANCE_TEST", "Database update selesai dalam ${durationSave}ms")
                                    Toast.makeText(this@UserDetailActivity, "Meteran Telah Diupdate!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@UserDetailActivity, "Error Menyimpan Data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Gagal Mengubah ke Angka Pada Gambar!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Tidak Ada Meteran Terdeteksi!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal Mendeteksi Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saat Proses Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun calculateAndDisplayPrice() {
        val pricePerUnitText = etPricePerUnit.text.toString()
        if (pricePerUnitText.isNotEmpty()) {
            val pricePerUnit = pricePerUnitText.toDoubleOrNull()
            if (pricePerUnit != null && pricePerUnit >= 0) {
                val totalPrice = user.usageThisMonth * pricePerUnit
                tvTotalPrice.text = "Total Harga: Rp ${"%,.2f".format(totalPrice)}"

                lifecycleScope.launch {
                    try {
                        val database = AppDatabase.Companion.getDatabase(this@UserDetailActivity)
                        val history = History(
                            name = user.name,
                            nik = user.nik,
                            oldReading = user.meterReading - user.usageThisMonth,
                            newReading = user.meterReading,
                            usage = user.usageThisMonth,
                            totalPrice = totalPrice,
                            date = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(Date())
                        )
                        val repo = HistoryRepository(
                            database.historyDao(),
                            FirebaseFirestore.getInstance()
                        )
                        repo.insertHistory(history)
                        Toast.makeText(this@UserDetailActivity, "Riwayat Pembayaran Telah Disimpan!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@UserDetailActivity, "Gagal Menyimpan Riwayat Pembayaran: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Harga per m³ tidak valid!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Masukkan harga per unit terlebih dahulu!", Toast.LENGTH_SHORT).show()
        }
    }
}