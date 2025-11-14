package com.coding.meet.todo_app.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.coding.meet.todo_app.models.Checklist

@Dao
interface ChecklistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist)

    @Update
    suspend fun updateChecklist(checklist: Checklist) // <-- Ini sudah ada, bagus!

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    // Mengambil semua checklist diurutkan berdasarkan tanggal pembuatan (terbaru di atas)
    @Query("SELECT * FROM Checklist ORDER BY createdDate DESC")
    fun getAllChecklists(): LiveData<List<Checklist>>

    // Mencari checklist berdasarkan judul
    @Query("SELECT * FROM Checklist WHERE checklistTitle LIKE :query ORDER BY createdDate DESC")
    fun searchChecklist(query: String): LiveData<List<Checklist>>

    // BARU: Fungsi untuk mengambil satu checklist berdasarkan ID-nya
    @Query("SELECT * FROM Checklist WHERE id = :checklistId")
    fun getChecklistById(checklistId: String): LiveData<Checklist>
}