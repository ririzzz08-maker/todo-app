package com.coding.meet.todo_app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.repository.TaskRepository
import com.coding.meet.todo_app.utils.Resource

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    val taskStateFlow get() =  taskRepository.taskStateFlow
    val statusLiveData get() =  taskRepository.statusLiveData
    val sortByLiveData get() =  taskRepository.sortByLiveData

    // --- FUNGSI BARU UNTUK FIREBASE ---
    /**
     * Inisialisasi Repository dengan UID pengguna yang sedang login.
     * Ini harus dipanggil dari Fragment setelah Auth berhasil.
     */
    fun setFirebaseUser(uid: String) {
        taskRepository.initUser(uid)
    }
    // ------------------------------------

    fun setSortBy(sort:Pair<String,Boolean>){
        taskRepository.setSortBy(sort)
    }

    // --- FUNGSI TASK ---
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

    // --- (Kode MediatorLiveData Anda sudah bagus dan tetap berfungsi) ---
    val allChecklists: LiveData<List<Checklist>> = MediatorLiveData<List<Checklist>>().apply {
        var currentSource: LiveData<List<Checklist>>? = null

        addSource(sortByLiveData) { sortPair ->
            currentSource?.let { removeSource(it) }

            // 'getAllChecklistsLiveData' akan kita modifikasi di Repository
            val newSource = taskRepository.getAllChecklistsLiveData(sortPair.second, sortPair.first)

            currentSource = newSource

            addSource(newSource) { checklistList ->
                value = checklistList
            }
        }
    }

    // --- (Fungsi searchChecklist Anda sudah bagus) ---
    fun searchChecklist(query: String, isAsc: Boolean, sortByName: String): LiveData<List<Checklist>> {
        return taskRepository.searchChecklist(query, isAsc, sortByName)
    }

    fun getChecklistById(checklistId: String): LiveData<Checklist> {
        return taskRepository.getChecklistById(checklistId)
    }
    fun getTaskByIdFromNavMenu(taskId: String): Task? {
        return taskStateFlow.value.data?.firstOrNull { it.id == taskId }
    }

    private val _taskToEdit = MutableLiveData<Task?>()
    val taskToEdit: LiveData<Task?> = _taskToEdit

    /**
     * Dipanggil oleh MainActivity saat item di nav drawer diklik
     */
    fun selectTaskForEdit(task: Task) {
        _taskToEdit.value = task
    }

    /**
     * Dipanggil oleh TaskListFragment setelah dialog ditampilkan
     */
    fun doneEditingTask() {
        _taskToEdit.value = null
    }
    private val _checklistToEdit = MutableLiveData<Checklist?>()
    val checklistToEdit: LiveData<Checklist?> = _checklistToEdit

    fun selectChecklistForEdit(checklist: Checklist) { _checklistToEdit.value = checklist }
    fun doneEditingChecklist() { _checklistToEdit.value = null }

    
}