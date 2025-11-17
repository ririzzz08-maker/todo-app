package com.coding.meet.todo_app.models

// Hapus import androidx.room...
import java.util.Date

// Hapus @Entity
data class Task(
    // Hapus @PrimaryKey dan @ColumnInfo
    var id: String,

    // Hapus @ColumnInfo
    val title: String,

    val description: String,
    val date: Date,

    // Hapus @ColumnInfo
    val imagePath: String? = null
) {
    // TAMBAHKAN INI:
    // Konstruktor kosong ini WAJIB untuk Firebase
    constructor() : this("", "", "", Date(), null)
}