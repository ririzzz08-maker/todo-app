package com.coding.meet.todo_app.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.coding.meet.todo_app.models.Checklist

@Dao
interface ChecklistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist)

    @Update
    suspend fun updateChecklist(checklist: Checklist)

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    // --- MODIFIKASI: Mengambil semua checklist dengan urutan dinamis ---
    // Query ini menggunakan 'CASE' untuk memilih kolom pengurutan secara dinamis
    @Query("SELECT * FROM Checklist ORDER BY " +
            "CASE WHEN :sortByName = 'checklistTitle' AND :isAsc = 1 THEN checklistTitle END ASC, " +
            "CASE WHEN :sortByName = 'checklistTitle' AND :isAsc = 0 THEN checklistTitle END DESC, " +
            "CASE WHEN :sortByName = 'createdDate' AND :isAsc = 1 THEN createdDate END ASC, " +
            "CASE WHEN :sortByName = 'createdDate' AND :isAsc = 0 THEN createdDate END DESC")
    fun getAllChecklists(isAsc: Boolean, sortByName: String): LiveData<List<Checklist>>

    // --- MODIFIKASI: Mencari checklist dengan urutan dinamis ---
    @Query("SELECT * FROM Checklist WHERE checklistTitle LIKE :query ORDER BY " +
            "CASE WHEN :sortByName = 'checklistTitle' AND :isAsc = 1 THEN checklistTitle END ASC, " +
            "CASE WHEN :sortByName = 'checklistTitle' AND :isAsc = 0 THEN checklistTitle END DESC, " +
            "CASE WHEN :sortByName = 'createdDate' AND :isAsc = 1 THEN createdDate END ASC, " +
            "CASE WHEN :sortByName = 'createdDate' AND :isAsc = 0 THEN createdDate END DESC")
    fun searchChecklist(query: String, isAsc: Boolean, sortByName: String): LiveData<List<Checklist>>

    // Fungsi ini untuk 'Edit', biarkan saja
    @Query("SELECT * FROM Checklist WHERE id = :checklistId")
    fun getChecklistById(checklistId: String): LiveData<Checklist>
}