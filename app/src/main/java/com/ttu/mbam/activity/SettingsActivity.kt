package com.ttu.mbam.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.ttu.mbam.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerAccuracy: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnSignOut: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        spinnerAccuracy = findViewById(R.id.spinnerAccuracy)
        btnSave = findViewById(R.id.btnSave)
        btnSignOut = findViewById(R.id.btnSignOut)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        val accuracyLevels = Array(10) { i -> String.format("%.1f", (i + 1) * 0.1) }
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, accuracyLevels)
        spinnerAccuracy.adapter = adapter

        // Load saved accuracy
        val savedAccuracy = sharedPreferences.getFloat("ai_accuracy", 0.6f).toString()
        val position = accuracyLevels.indexOf(savedAccuracy)
        if (position >= 0) {
            spinnerAccuracy.setSelection(position)
        }

        btnSave.setOnClickListener {
            val selectedAccuracy = spinnerAccuracy.selectedItem.toString().toFloat()
            sharedPreferences.edit().putFloat("ai_accuracy", selectedAccuracy).apply()
        }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bottom Navigation Handling
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
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> true

                else -> false
            }
        }
    }
}