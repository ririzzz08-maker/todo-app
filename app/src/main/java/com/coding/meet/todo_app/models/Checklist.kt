package com.coding.meet.todo_app.models

// Hapus semua import androidx.room...
// import com.coding.meet.todo_app.converters.ChecklistItemsConverter // <-- TIDAK DIPERLUKAN LAGI
import java.util.Date

// Hapus @Entity dan @TypeConverters
data class Checklist(
    // Hapus @PrimaryKey
    var id: String, // <-- DIUBAH MENJADI VAR

    // Hapus @ColumnInfo
    val title: String, // <-- Ganti nama 'checklistTitle' menjadi 'title' agar konsisten

    // Hapus @ColumnInfo
    val createdDate: Date,

    // Hapus @ColumnInfo
    var imagePath: String?,

    // Hapus @ColumnInfo
    // Kabar baik: Firebase bisa menyimpan List<ChecklistItem> secara langsung!
    val items: List<ChecklistItem>
) {
    // TAMBAHKAN KONSTRUKTOR KOSONG INI (WAJIB UNTUK FIREBASE)
    constructor() : this("", "", Date(), null, emptyList())
}