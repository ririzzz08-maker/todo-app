package com.coding.meet.todo_app

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login2)

        // --- 1. Logika untuk Tombol Register (tetap) ---
        val registerButton: Button = findViewById(R.id.btn_register)
        registerButton.setOnClickListener {
            val intentPindah = Intent(this, RegisterActivity::class.java)
            startActivity(intentPindah)
        }

        // --- 2. Tambahkan Logika untuk Tombol Login (Masuk) ---
        val loginButton: Button = findViewById(R.id.btn_in) // Temukan tombol Masuk
        loginButton.setOnClickListener {
            // 1. Memulai Activity Tujuan (MainActivity)
            val intentLogin = Intent(this, MainActivity::class.java)
            startActivity(intentLogin)

            // 2. Menutup Activity Saat Ini
            finish()
        }
        // --------------------------------------------------

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}