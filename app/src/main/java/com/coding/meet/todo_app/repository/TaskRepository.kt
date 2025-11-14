package com.coding.meet.todo_app.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Query
import com.coding.meet.todo_app.database.TaskDatabase
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Resource
import com.coding.meet.todo_app.utils.Resource.Error
import com.coding.meet.todo_app.utils.Resource.Loading
import com.coding.meet.todo_app.utils.Resource.Success
import com.coding.meet.todo_app.utils.StatusResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskRepository(application: Application) {

    private val taskDao = TaskDatabase.getInstance(application).taskDao
    private val checklistDao = TaskDatabase.getInstance(application).checklistDao


    private val _taskStateFlow = MutableStateFlow<Resource<Flow<List<Task>>>>(Loading())
    val taskStateFlow: StateFlow<Resource<Flow<List<Task>>>>
        get() = _taskStateFlow

    private val _checklistStateFlow = MutableStateFlow<Resource<Flow<List<Checklist>>>>(Loading())
    val checklistStateFlow: StateFlow<Resource<Flow<List<Checklist>>>>
        get() = _checklistStateFlow

    private val _statusLiveData = MutableLiveData<Resource<StatusResult>>()
    val statusLiveData: LiveData<Resource<StatusResult>>
        get() = _statusLiveData


    private val _sortByLiveData = MutableLiveData<Pair<String,Boolean>>().apply {
        postValue(Pair("title",true))
    }
    val sortByLiveData: LiveData<Pair<String,Boolean>>
        get() = _sortByLiveData


    fun setSortBy(sort:Pair<String,Boolean>){
        _sortByLiveData.postValue(sort)
    }

    // --- FUNGSI UNTUK TASK ---

    fun getTaskList(isAsc : Boolean, sortByName:String) {
        // ... (kode tetap sama) ...
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _taskStateFlow.emit(Loading())
                delay(500)
                val result = if (sortByName == "title"){
                    taskDao.getTaskListSortByTaskTitle(isAsc)
                }else{
                    taskDao.getTaskListSortByTaskDate(isAsc)
                }
                _taskStateFlow.emit(Success("loading", result))
            } catch (e: Exception) {
                _taskStateFlow.emit(Error(e.message.toString()))
            }
        }
    }


    fun insertTask(task: Task) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                val result = taskDao.insertTask(task)
                handleResult(result.toInt(), "Inserted Task Successfully", StatusResult.Added)
            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun deleteTask(task: Task) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                val result = taskDao.deleteTask(task)
                handleResult(result, "Deleted Task Successfully", StatusResult.Deleted)

            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun deleteTaskUsingId(taskId: String) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                val result = taskDao.deleteTaskUsingId(taskId)
                handleResult(result, "Deleted Task Successfully", StatusResult.Deleted)

            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }


    fun updateTask(task: Task) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                val result = taskDao.updateTask(task)
                handleResult(result, "Updated Task Successfully", StatusResult.Updated)

            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun updateTaskPaticularField(taskId: String, title: String, description: String) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                val result = taskDao.updateTaskPaticularField(taskId, title, description)
                handleResult(result, "Updated Task Successfully", StatusResult.Updated)

            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun searchTaskList(query: String) {
        // ... (kode tetap sama) ...
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _taskStateFlow.emit(Loading())
                val result = taskDao.searchTaskList("%${query}%")
                _taskStateFlow.emit(Success("loading", result))
            } catch (e: Exception) {
                _taskStateFlow.emit(Error(e.message.toString()))
            }
        }
    }

    // --- FUNGSI UNTUK CHECKLIST ---

    fun insertChecklist(checklist: Checklist) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                checklistDao.insertChecklist(checklist)
                handleResult(1, "Inserted Checklist Successfully", StatusResult.Added)
            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun updateChecklist(checklist: Checklist) {
        // ... (kode tetap sama) ...
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                checklistDao.updateChecklist(checklist)
                handleResult(1, "Updated Checklist Successfully", StatusResult.Updated)
            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    // BARU: Fungsi untuk Hapus Checklist
    fun deleteChecklist(checklist: Checklist) {
        try {
            _statusLiveData.postValue(Loading())
            CoroutineScope(Dispatchers.IO).launch {
                checklistDao.deleteChecklist(checklist)
                // Kita gunakan StatusResult.Deleted
                handleResult(1, "Deleted Checklist Successfully", StatusResult.Deleted)
            }
        } catch (e: Exception) {
            _statusLiveData.postValue(Error(e.message.toString()))
        }
    }

    fun getAllChecklistsLiveData(): LiveData<List<Checklist>> {
        // ... (kode tetap sama) ...
        return checklistDao.getAllChecklists()
    }

    fun searchChecklist(query: String): LiveData<List<Checklist>> {
        // ... (kode tetap sama) ...
        return checklistDao.searchChecklist("%${query}%")
    }

    fun getChecklistById(checklistId: String): LiveData<Checklist> {
        // ... (kode tetap sama) ...
        return checklistDao.getChecklistById(checklistId)
    }


    // --- FUNGSI INTERNAL ---

    private fun handleResult(result: Int, message: String, statusResult: StatusResult) {
        // ... (kode tetap sama) ...
        if (result != -1) {
            _statusLiveData.postValue(Success(message, statusResult))
        } else {
            _statusLiveData.postValue(Error("Something Went Wrong", statusResult))
        }
    }
}