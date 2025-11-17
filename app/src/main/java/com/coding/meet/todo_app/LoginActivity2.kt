package com.coding.meet.todo_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast // <-- IMPORT PENTING
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// --- IMPORT UNTUK BINDING DAN FIREBASE ---
import com.coding.meet.todo_app.databinding.ActivityLogin2Binding // <-- Ganti sesuai nama file XML Anda
import com.google.firebase.auth.FirebaseAuth

// 3. Ini untuk properti ekstensi '.auth' pada objek 'Firebase'


class LoginActivity2 : AppCompatActivity() {

    // Deklarasi binding dan auth
    private lateinit var binding: ActivityLogin2Binding
    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()

        // Cek apakah pengguna saat ini sudah login (tidak null)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Jika sudah login, langsung pindah ke MainActivity
            Toast.makeText(this, "Login otomatis...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Tutup halaman login agar tidak bisa kembali
        }
        // Jika currentUser adalah null, tidak terjadi apa-apa
        // dan halaman login akan ditampilkan seperti biasa.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gunakan ViewBinding
        binding = ActivityLogin2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Panggil fungsi untuk semua listener
        setupActionListeners()

        // Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Fungsi untuk menampung semua listener (lebih rapi)
    private fun setupActionListeners() {
        // --- 1. Logika untuk Tombol Register (sudah benar) ---
        binding.btnRegister.setOnClickListener {
            val intentPindah = Intent(this, RegisterActivity::class.java)
            startActivity(intentPindah)
        }

        // --- 2. Logika untuk Tombol Login (Masuk) ---
        binding.btnIn.setOnClickListener {
            // Ambil data dari EditText
            // (Saya asumsikan ID-nya 'etEmail' dan 'etPassword')
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Panggil fungsi login
            loginUser(email, password)
        }
    }

    // Fungsi terpisah untuk memproses login ke Firebase
    private fun loginUser(email: String, password: String) {

        // Validasi sederhana: pastikan tidak kosong
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        // (Opsional: Tampilkan loading/progress bar di sini)
        // ...

        // --- INI FUNGSI UTAMA LOGIN FIREBASE ---
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // (Opsional: Sembunyikan loading di sini)
                // ...

                if (task.isSuccessful) {
                    // Jika Login BERHASIL
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()

                    // Pindahkan ke MainActivity dan tutup halaman login
                    val intentLogin = Intent(this, MainActivity::class.java)
                    startActivity(intentLogin)
                    finish() // Tutup LoginActivity

                } else {
                    // Jika Login GAGAL
                    val errorMessage = task.exception?.message ?: "Login gagal, coba lagi."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

}