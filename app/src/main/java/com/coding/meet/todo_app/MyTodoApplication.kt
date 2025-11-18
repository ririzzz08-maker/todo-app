package com.coding.meet.todo_app

import android.app.Application
import android.content.Context // <-- IMPORT INI
import androidx.appcompat.app.AppCompatDelegate // <-- IMPORT INI
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp // <-- IMPORT INI, jika Anda menggunakan Firebase

class MyTodoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase (jika Anda menggunakan Firebase Auth atau Database)
        FirebaseApp.initializeApp(this)

        // Konfigurasi Cloudinary Anda
        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dinsmack6"
        config["api_key"] = "535798271879332"
        config["api_secret"] = "2kpMlKQ_t2PwXJhYIpk1mA_-nBs"
        MediaManager.init(this, config)

        // ===== PENTING: TAMBAHAN UNTUK TEMA GELAP UNIVERSAL =====
        // Baca preferensi tema dari SharedPreferences
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false) // Default: false (mode terang)

        // Terapkan tema berdasarkan preferensi yang disimpan
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // =========================================================
    }
}