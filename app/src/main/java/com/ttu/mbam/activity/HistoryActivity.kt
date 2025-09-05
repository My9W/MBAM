package com.ttu.mbam.activity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.HistoryAdapter
import com.ttu.mbam.activity.MainActivity
import com.ttu.mbam.R
import com.ttu.mbam.activity.SettingsActivity
import com.ttu.mbam.dao.HistoryDao
import com.ttu.mbam.model.History
import com.ttu.mbam.repository.HistoryRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyDao: HistoryDao
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val database = AppDatabase.Companion.getDatabase(this)
        historyDao = database.historyDao()
        val repo = HistoryRepository(historyDao, FirebaseFirestore.getInstance())
        repo.startSyncing()
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_history
        rvHistory = findViewById(R.id.rvHistory)
        historyAdapter = HistoryAdapter(listOf(), this)
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter


        historyDao.getAllHistory().observe(this) { history ->
            historyAdapter.updateHistory(history)
        }


        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_users -> {
                    startActivity(Intent(this, AddUserActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_history -> true
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

    open fun savePaymentNoteAsImage(history: History) {

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.payment_note, null)

        // Populate the layout with history data
        view.findViewById<TextView>(R.id.tvNoteUserName).text = history.name
        view.findViewById<TextView>(R.id.tvNoteNIK).text = "NIK: ${history.nik}"
        view.findViewById<TextView>(R.id.tvNoteOldReading).text = "Lama: ${history.oldReading}"
        view.findViewById<TextView>(R.id.tvNoteNewReading).text = "Baru: ${history.newReading}"
        view.findViewById<TextView>(R.id.tvNoteUsage).text = "Penggunaan: ${history.usage}"
        view.findViewById<TextView>(R.id.tvNoteTotalPrice).text = "Rp ${"%,.2f".format(history.totalPrice)}"
        view.findViewById<TextView>(R.id.tvNotePaymentDate).text = "Date: ${history.date}"

        // Measure and layout the view
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Create a bitmap and draw the view on the canvas
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        // Show the preview dialog
        showPreviewDialog(bitmap)
    }

    private fun showPreviewDialog(bitmap: Bitmap) {
        val dialog = AlertDialog.Builder(this).create()
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        dialog.setView(imageView)
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Simpan") { _, _ ->
            saveBitmapToDCIM(bitmap)
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Batal") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }

    private fun saveBitmapToDCIM(bitmap: Bitmap) {
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "PaymentNotes"
        )
        if (!storageDir.exists()) storageDir.mkdirs()

        val file = File(storageDir, "Payment_Note_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Notify user and add the image to gallery
            Toast.makeText(this, "Saved at ${file.absolutePath}", Toast.LENGTH_LONG).show()
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }
}