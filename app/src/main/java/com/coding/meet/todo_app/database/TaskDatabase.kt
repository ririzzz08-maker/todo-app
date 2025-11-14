package com.coding.meet.todo_app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.coding.meet.todo_app.converters.ChecklistItemsConverter // BARU: Import converter checklist
import com.coding.meet.todo_app.converters.TypeConverter
import com.coding.meet.todo_app.dao.ChecklistDao // BARU: Import DAO checklist
import com.coding.meet.todo_app.dao.TaskDao
import com.coding.meet.todo_app.models.Checklist // BARU: Import model checklist
import com.coding.meet.todo_app.models.Task

@Database(
    entities = [Task::class, Checklist::class], // BARU: Tambahkan Checklist di sini
    version = 3, // BARU: Naikkan versi dari 1 ke 2
    exportSchema = false
)
@TypeConverters(TypeConverter::class, ChecklistItemsConverter::class) // BARU: Tambahkan Converter di sini
abstract class TaskDatabase : RoomDatabase() {

    abstract val taskDao : TaskDao
    abstract val checklistDao : ChecklistDao // BARU: Tambahkan akses ke DAO checklist

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                )
                    // BARU: Tambahkan ini agar aplikasi tidak crash saat versi naik (hati-hati, ini mereset data lama saat development)
                    .fallbackToDestructiveMigration()
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}