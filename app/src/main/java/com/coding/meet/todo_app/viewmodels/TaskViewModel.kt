package com.coding.meet.todo_app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData // <-- INI ALTERNATIF BARU KITA
import androidx.lifecycle.MutableLiveData
import androidx.room.Query
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.repository.TaskRepository
import com.coding.meet.todo_app.utils.Resource

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    val taskStateFlow get() =  taskRepository.taskStateFlow
    val statusLiveData get() =  taskRepository.statusLiveData
    val sortByLiveData get() =  taskRepository.sortByLiveData

    fun setSortBy(sort:Pair<String,Boolean>){
        taskRepository.setSortBy(sort)
    }

    // --- FUNGSI TASK --- (Diabaikan untuk singkatnya)
    fun getTaskList(isAsc : Boolean, sortByName:String) { taskRepository.getTaskList(isAsc, sortByName) }
    fun insertTask(task: Task){ taskRepository.insertTask(task) }
    fun deleteTask(task: Task) { taskRepository.deleteTask(task) }
    fun deleteTaskUsingId(taskId: String){ taskRepository.deleteTaskUsingId(taskId) }
    fun updateTask(task: Task) { taskRepository.updateTask(task) }
    fun updateTaskPaticularField(taskId: String,title:String,description:String) { taskRepository.updateTaskPaticularField(taskId, title, description) }
    fun searchTaskList(query: String){ taskRepository.searchTaskList(query) }

    // --- FUNGSI CHECKLIST ---
    fun insertChecklist(checklist: Checklist) { taskRepository.insertChecklist(checklist) }
    fun updateChecklist(checklist: Checklist) { taskRepository.updateChecklist(checklist) }
    fun deleteChecklist(checklist: Checklist) { taskRepository.deleteChecklist(checklist) }

    // --- ALTERNATIF: MENGGANTI Transformations dengan MediatorLiveData ---
    val allChecklists: LiveData<List<Checklist>> = MediatorLiveData<List<Checklist>>().apply {
        var currentSource: LiveData<List<Checklist>>? = null

        addSource(sortByLiveData) { sortPair ->
            currentSource?.let { removeSource(it) }

            val newSource = taskRepository.getAllChecklistsLiveData(sortPair.second, sortPair.first)

            currentSource = newSource

            addSource(newSource) { checklistList ->
                value = checklistList // Meneruskan nilai dari LiveData baru
            }
        }
    }

    // --- searchChecklist (yang memiliki error yang sama) ---
    fun searchChecklist(query: String, isAsc: Boolean, sortByName: String): LiveData<List<Checklist>> {
        return taskRepository.searchChecklist(query, isAsc, sortByName)
    }

    fun getChecklistById(checklistId: String): LiveData<Checklist> {
        return taskRepository.getChecklistById(checklistId)
    }
}