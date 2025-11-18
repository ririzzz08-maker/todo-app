package com.coding.meet.todo_app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Checklist(
    var id: String = "",
    val title: String = "",
    val createdDate: Date = Date(),
    var imagePath: String? = null,
    val items: List<ChecklistItem> = emptyList()
) : Parcelable
// KITA HAPUS CONSTRUCTOR() KEDUA KARENA SUDAH TIDAK PERLU