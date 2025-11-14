package com.coding.meet.todo_app // Pastikan package Anda benar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    // Definisikan semua komponen UI
    private lateinit var imageViewProfile: ImageView
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var buttonSimpan: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ini masih sama, memuat layout XML
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hubungkan variabel Kotlin dengan ID di XML
        imageViewProfile = view.findViewById(R.id.imageViewProfile)
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        buttonSimpan = view.findViewById(R.id.buttonSimpan)

        // 1. Tambahkan listener untuk klik gambar profil
        imageViewProfile.setOnClickListener {
            // Nanti, kode untuk membuka galeri foto akan ditaruh di sini
            Toast.makeText(requireContext(), "Buka Galeri...", Toast.LENGTH_SHORT).show()
        }

        // 2. Tambahkan listener untuk tombol simpan
        buttonSimpan.setOnClickListener {
            // Ambil data dari setiap EditText
            val nama = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validasi sederhana (pastikan tidak kosong)
            if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua data", Toast.LENGTH_SHORT).show()
            } else {
                // Tampilkan data yang "disimpan" (karena belum ada database)
                val feedback = "Disimpan:\nNama: $nama\nEmail: $email"
                Toast.makeText(requireContext(), feedback, Toast.LENGTH_LONG).show()

                // Di sini Anda akan menjalankan kode untuk simpan ke database
            }
        }
    }
}