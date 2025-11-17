// Pastikan package ini benar sesuai lokasi file Anda
package com.coding.meet.todo_app

import android.app.Application
import com.cloudinary.android.MediaManager

class MyTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Konfigurasi untuk Cloudinary
        val config = mutableMapOf<String, String>()

        // INI CARA YANG BENAR (menggunakan Cloud Name "dinsmack6")
        config["cloud_name"] = "dinsmack6"

        // INI CARA YANG BENAR
        MediaManager.init(this, config)
    }
}