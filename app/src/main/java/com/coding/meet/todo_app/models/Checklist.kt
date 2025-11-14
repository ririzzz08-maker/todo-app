package com.coding.meet.todo_app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.coding.meet.todo_app.converters.ChecklistItemsConverter // Ini perlu dibuat nanti
import java.util.Date

// Ini adalah entitas utama untuk sebuah Checklist (misalnya "Daftar Belanja", "Rencana Liburan")
@Entity(tableName = "Checklist")
@TypeConverters(ChecklistItemsConverter::class) // Kita akan membuat ini nanti untuk menyimpan List<ChecklistItem>
data class Checklist(
    @PrimaryKey(autoGenerate = false)
    val id: String,

    @ColumnInfo(name = "checklistTitle")
    val title: String,

    @ColumnInfo(name = "createdDate")
    val createdDate: Date,

    @ColumnInfo(name = "imagePath")
    val imagePath: String?, // Jalur ke gambar terkait (opsional)

    @ColumnInfo(name = "items")
    val items: List<ChecklistItem> // Daftar item di dalam checklist
)