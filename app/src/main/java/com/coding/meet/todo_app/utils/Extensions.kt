package com.coding.meet.todo_app.utils

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.coding.meet.todo_app.R // <-- Pastikan R diimpor
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// Fungsi untuk Toast
fun Context.longToastShow(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

// Fungsi untuk Validasi EditText
fun validateEditText(editText: TextInputEditText, layout: TextInputLayout): Boolean {
    return if (editText.text.toString().trim().isEmpty()) {
        layout.error = "Mohon isi kolom ini" // Anda bisa ganti pesan error di sini
        false
    } else {
        layout.error = null
        true
    }
}

// Fungsi untuk Membersihkan EditText
fun clearEditText(editText: TextInputEditText, layout: TextInputLayout) {
    editText.text = null
    layout.error = null
}

// Fungsi untuk Sembunyikan Keyboard
fun Context.hideKeyBoard(view: View) {
    try {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Fungsi untuk Setup Dialog
fun Dialog.setupDialog(layoutResID: Int) {
    setContentView(layoutResID)
    window!!.setBackgroundDrawableResource(android.R.color.transparent)
    setCancelable(false)
    setCanceledOnTouchOutside(false)
}