package com.coding.meet.todo_app

import android.app.Application
import com.cloudinary.android.MediaManager // <-- IMPORT PENTING

class MyTodoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- TAMBAHKAN BLOK KODE INI ---
        // Ini adalah konfigurasi untuk login ke akun Cloudinary Anda
        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dinsmack6"
        config["api_key"] = "535798271879332"
        config["api_secret"] = "2kpMlKQ_t2PwXJhYIpk1mA_-nBs" // (Aman untuk tes, tapi jangan di-hardcode di aplikasi rilis)

        // Inisialisasi Cloudinary dengan konfigurasi di atas
        MediaManager.init(this, config)
        // ---------------------------------
    }
}