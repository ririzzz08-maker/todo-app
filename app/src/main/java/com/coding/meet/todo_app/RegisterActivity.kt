package com.coding.meet.todo_app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// 1. IMPORT VIEW BINDING (SANGAT PENTING)
import com.coding.meet.todo_app.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
// HAPUS: import com.google.firebase.auth.auth
// HAPUS: import com.google.firebase.Firebase

class RegisterActivity : AppCompatActivity() {

    // 2. Deklarasikan 'binding' dan 'auth'
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 3. Gunakan ViewBinding untuk 'menggambar' layout
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth (GAYA JAVA)
        auth = FirebaseAuth.getInstance() // <-- PERUBAHAN DI SINI

        // 4. Panggil satu fungsi bersih untuk semua aksi/listener
        setupActionListeners()

        // 5. Terapkan Insets ke root layout dari 'binding'
        ViewCompat.setOnApplyWindowInsetsListener(binding.registerRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 6. Buat fungsi terpisah untuk semua listener (LEBIH RAPI)
    private fun setupActionListeners() {
        // 'btnIn' adalah ID dari 'btn_in' di XML Anda
        binding.btnIn.setOnClickListener {
            // Ambil data langsung dari binding (lebih aman)
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etPassword2.text.toString().trim()

            // Panggil fungsi register
            registerUser(email, password, confirmPassword)
        }
    }

    // Fungsi ini SUDAH BAGUS, tidak perlu diubah
    private fun registerUser(email: String, password: String, confirmPassword: String) {

        // 1. Validasi input (pastikan tidak kosong)
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Isi Email dan Password!", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. VALIDASI KONFIRMASI PASSWORD
        if (password != confirmPassword) {
            Toast.makeText(this, "Password dan Konfirmasi Password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. VALIDASI PANJANG PASSWORD (Firebase minimal 6)
        if (password.length < 6) {
            // Anda menulis 8 di pesan sebelumnya, tapi Firebase minimal 6
            Toast.makeText(this, "Password minimal harus 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        // (Opsional: Tampilkan ProgressBar di sini jika ada)
        // binding.progressBar.visibility = View.VISIBLE
        // binding.btnIn.isEnabled = false // Nonaktifkan tombol

        // Panggil fungsi Firebase untuk membuat akun
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // (Opsional: Sembunyikan ProgressBar di sini)
                // binding.progressBar.visibility = View.GONE
                // binding.btnIn.isEnabled = true // Aktifkan tombol kembali

                if (task.isSuccessful) {
                    // Jika register BERHASIL
                    Toast.makeText(this, "Pendaftaran berhasil!", Toast.LENGTH_SHORT).show()

                    // Arahkan pengguna ke halaman Login
                    finish()
                } else {
                    // Jika register GAGAL
                    Toast.makeText(this, "Pendaftaran Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}