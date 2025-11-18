package com.coding.meet.todo_app // Pastikan package Anda benar

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Perlu ini untuk navigasi
import coil.load // Perlu ini untuk memuat gambar
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.materialswitch.MaterialSwitch // Perlu ini
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    // Definisikan semua komponen UI
    private lateinit var ivAvatar: ImageView // Mengubah nama menjadi ivAvatar sesuai layout terbaru
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var buttonSimpan: Button
    private lateinit var btnLogout: Button // Tombol Logout yang baru
    private lateinit var switchDarkMode: MaterialSwitch // Switch Mode Gelap yang baru

    // Tambahan untuk fungsionalitas
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var loadingDialog: Dialog
    private var imageUri: Uri? = null // Untuk gambar baru yang dipilih

    private lateinit var sharedPreferences: SharedPreferences // Untuk tema

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase dan SharedPreferences
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        loadingDialog = createLoadingDialog(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Hubungkan variabel Kotlin dengan ID di XML (SESUAIKAN DENGAN ID XML TERBARU)
        ivAvatar = view.findViewById(R.id.ivAvatar) // Menggunakan ID ivAvatar
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        buttonSimpan = view.findViewById(R.id.buttonSimpan)
        btnLogout = view.findViewById(R.id.btnLogout) // ID tombol Logout
        switchDarkMode = view.findViewById(R.id.switchDarkMode) // ID switch Mode Gelap

        // Hapus: etPassword tidak lagi ada di layout
        // etPassword = view.findViewById(R.id.etPassword)

        loadUserProfile()
        setupListeners()
        setupDarkModeSwitch()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Anda belum login.", Toast.LENGTH_SHORT).show()
            // Navigasi ke LoginActivity2 Anda
            findNavController().navigate(R.id.action_profileFragment_to_loginActivity2)
            requireActivity().finish()
            return
        }

        etEmail.setText(currentUser.email) // Email (read-only)
        etEmail.isEnabled = false // Pastikan tidak bisa diedit

        etName.setText(currentUser.displayName ?: "")

        val photoUrl = currentUser.photoUrl?.toString()
        if (!photoUrl.isNullOrEmpty()) {
            ivAvatar.load(photoUrl) {
                placeholder(R.drawable.incon) // Gunakan ikon default Anda
                error(R.drawable.incon)
                crossfade(true)
            }
        } else {
            ivAvatar.setImageResource(R.drawable.incon)
        }
    }

    private fun setupListeners() {
        ivAvatar.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(galleryIntent)
        }

        buttonSimpan.setOnClickListener {
            saveUserProfile()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Berhasil Logout", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginActivity2)
            requireActivity().finish()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                ivAvatar.setImageURI(uri)
            }
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return
        val newName = etName.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()

        if (imageUri != null) {
            MediaManager.get().upload(imageUri).callback(object : UploadCallback {
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val newPhotoUrl = resultData?.get("secure_url") as? String
                    updateFirebaseProfile(currentUser, newName, newPhotoUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Gagal upload gambar: ${error?.description}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Upload error: ${error?.description}")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                override fun onStart(requestId: String?) {}
            }).dispatch()
        } else {
            updateFirebaseProfile(currentUser, newName, currentUser.photoUrl?.toString())
        }
    }

    private fun updateFirebaseProfile(currentUser: FirebaseUser, newName: String, newPhotoUrl: String?) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .setPhotoUri(if (newPhotoUrl != null) Uri.parse(newPhotoUrl) else null)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val uid = currentUser.uid
                    val profileData = mapOf(
                        "name" to newName,
                        "photoUrl" to (newPhotoUrl ?: "")
                    )
                    database.reference.child("users").child(uid).child("profile").setValue(profileData)
                        .addOnCompleteListener { dbTask ->
                            loadingDialog.dismiss()
                            if (dbTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            } else {
                                Toast.makeText(requireContext(), "Gagal simpan data profil.", Toast.LENGTH_SHORT).show()
                                Log.e("ProfileFragment", "RTDB save error: ${dbTask.exception?.message}")
                            }
                        }
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Gagal memperbarui profil: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileFragment", "Auth profile update error: ${authTask.exception?.message}")
                }
            }
    }

    private fun setupDarkModeSwitch() {
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false)
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("DARK_MODE", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            requireActivity().recreate()
        }
    }

    // Fungsi utilitas untuk dialog loading
    private fun createLoadingDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_loading_dialog) // Pastikan layout ini ada
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }
}