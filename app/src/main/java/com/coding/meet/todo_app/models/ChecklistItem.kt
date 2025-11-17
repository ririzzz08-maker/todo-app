package com.coding.meet.todo_app.models

// Ini adalah item individu dalam sebuah checklist (misalnya "Beli susu", "Jemur pakaian")
// Biasanya tidak disimpan sebagai entitas terpisah di Room jika hanya bagian dari satu Checklist.
// Kita akan menggunakannya sebagai bagian dari List di model Checklist utama.
// Jika Anda ingin menyimpan ini terpisah, Anda harus menambahkan foreign key ke Checklist.

data class ChecklistItem(
    var id: String, // ID unik untuk setiap item
    var text: String, // Teks item (misalnya "Beli bahan makanan")
    var isChecked: Boolean = false // Status apakah item sudah dicentang
)
{
    // TAMBAHKAN KONSTRUKTOR KOSONG INI
    constructor() : this("", "", false)
}