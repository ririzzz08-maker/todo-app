package com.coding.meet.todo_app.converters // Pastikan package ini benar

import androidx.room.TypeConverter
import com.coding.meet.todo_app.models.ChecklistItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChecklistItemsConverter {

    private val gson = Gson()

    // Mengonversi List<ChecklistItem> menjadi String JSON untuk disimpan di DB
    @TypeConverter
    fun fromChecklistItemsList(items: List<ChecklistItem>?): String? {
        return gson.toJson(items)
    }

    // Mengonversi String JSON dari DB kembali menjadi List<ChecklistItem>
    @TypeConverter
    fun toChecklistItemsList(json: String?): List<ChecklistItem>? {
        if (json == null) {
            return emptyList()
        }
        val type = object : TypeToken<List<ChecklistItem>>() {}.type
        return gson.fromJson(json, type)
    }
}