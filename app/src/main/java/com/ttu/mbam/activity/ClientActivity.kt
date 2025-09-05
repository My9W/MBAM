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
import com.google.firebase.firestore.FirebaseFirestore
import com.ttu.mbam.AppDatabase
import com.ttu.mbam.activity.HistoryActivity
import com.ttu.mbam.HistoryAdapter
import com.ttu.mbam.R
import com.ttu.mbam.dao.HistoryDao
import com.ttu.mbam.dao.UserDao
import com.ttu.mbam.model.History
import com.ttu.mbam.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ClientActivity : AppCompatActivity() {

    private lateinit var userDao: UserDao
    private lateinit var historyDao: HistoryDao
    private lateinit var tvClientName: TextView
    private lateinit var tvClientNik: TextView
    private lateinit var tvClientAddress: TextView
    private lateinit var tvClientUsage: TextView
    private lateinit var tvClientMeter: TextView
    private lateinit var tvClientLastPayment: TextView
    private lateinit var rvClientHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyRepo: HistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        // Bind views
        tvClientName = findViewById(R.id.tvClientName)
        tvClientNik = findViewById(R.id.tvClientNik)
        tvClientAddress = findViewById(R.id.tvClientAddress)
        tvClientUsage = findViewById(R.id.tvClientUsage)
        tvClientMeter = findViewById(R.id.tvClientMeter)
        tvClientLastPayment = findViewById(R.id.tvClientLastPayment)
        rvClientHistory = findViewById(R.id.rvClientHistory)

        val clientNik = intent.getStringExtra("client_nik") ?: return

        val db = AppDatabase.Companion.getDatabase(this)
        userDao = db.userDao()
        historyDao = db.historyDao()

        historyRepo = HistoryRepository(historyDao, FirebaseFirestore.getInstance())
        historyRepo.startSyncing()

        historyAdapter = HistoryAdapter(emptyList(), object : HistoryActivity() {
            override fun savePaymentNoteAsImage(history: History) {
                showPaymentNoteDialog(history)
            }
        })

        rvClientHistory.layoutManager = LinearLayoutManager(this)
        rvClientHistory.adapter = historyAdapter

        CoroutineScope(Dispatchers.IO).launch {
            val user = userDao.getUserByNik(clientNik)
            withContext(Dispatchers.Main) {
                user?.let {
                    tvClientName.text = "Nama: ${it.name}"
                    tvClientNik.text = "NIK: ${it.nik}"
                    tvClientAddress.text = "Alamat: ${it.address}"
                    tvClientUsage.text = "Penggunaan Bulan Ini: ${it.usageThisMonth}"
                    tvClientMeter.text = "Meter Saat Ini: ${it.meterReading}"
                    tvClientLastPayment.text = "Terakhir Pembayaran: ${it.lastPaymentDate}"
                }
            }
        }

        historyDao.getHistoryByNik(clientNik).observe(this) { list ->
            historyAdapter.updateHistory(list)
        }
    }

    private fun showPaymentNoteDialog(history: History) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.payment_note, null)

        view.findViewById<TextView>(R.id.tvNoteUserName).text = history.name
        view.findViewById<TextView>(R.id.tvNoteNIK).text = "NIK: ${history.nik}"
        view.findViewById<TextView>(R.id.tvNoteOldReading).text = "Lama: ${history.oldReading}"
        view.findViewById<TextView>(R.id.tvNoteNewReading).text = "Baru: ${history.newReading}"
        view.findViewById<TextView>(R.id.tvNoteUsage).text = "Penggunaan: ${history.usage}"
        view.findViewById<TextView>(R.id.tvNoteTotalPrice).text = "Rp ${"%,.2f".format(history.totalPrice)}"
        view.findViewById<TextView>(R.id.tvNotePaymentDate).text = "Date: ${history.date}"

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val dialog = AlertDialog.Builder(this).create()
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        dialog.setView(imageView)
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Simpan") { _, _ ->
            saveBitmapToDCIM(bitmap)
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Batal") { d, _ -> d.dismiss() }
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
            Toast.makeText(this, "Saved at ${file.absolutePath}", Toast.LENGTH_LONG).show()
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }
}