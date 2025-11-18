package com.coding.meet.todo_app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChecklistItem(
    val id: String = "",
    var text: String = "",
    var isChecked: Boolean = false
) : Parcelable
// KITA HAPUS CONSTRUCTOR() KEDUA KARENA SUDAH TIDAK PERLU