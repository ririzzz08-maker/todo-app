package com.coding.meet.todo_app.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Resource
import com.coding.meet.todo_app.utils.Resource.Error
import com.coding.meet.todo_app.utils.Resource.Loading
import com.coding.meet.todo_app.utils.Resource.Success
import com.coding.meet.todo_app.utils.StatusResult
// --- IMPORT FIREBASE ---
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
// ----------------------
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskRepository(application: Application) {

    // --- HAPUS ROOM DAO ---
    // private val taskDao = TaskDatabase.getInstance(application).taskDao
    // private val checklistDao = TaskDatabase.getInstance(application).checklistDao

    // --- TAMBAHKAN REFERENSI FIREBASE ---
    private var taskRef: DatabaseReference? = null
    private var checklistRef: DatabaseReference? = null
    private val database = FirebaseDatabase.getInstance()
    private var currentTaskListener: ValueEventListener? = null
    private var currentTaskQuery: Query? = null
    // ---------------------------------

    // --- UBAH TIPE: StateFlow tidak lagi butuh Flow di dalamnya ---
    private val _taskStateFlow = MutableStateFlow<Resource<List<Task>>>(Loading())
    val taskStateFlow: StateFlow<Resource<List<Task>>>
        get() = _taskStateFlow

    // (Live data lain tetap sama)
    private val _statusLiveData = MutableLiveData<Resource<StatusResult>>()
    val statusLiveData: LiveData<Resource<StatusResult>>
        get() = _statusLiveData

    private val _sortByLiveData = MutableLiveData<Pair<String,Boolean>>().apply {
        postValue(Pair("title",true))
    }
    val sortByLiveData: LiveData<Pair<String,Boolean>>
        get() = _sortByLiveData

    /**
     * FUNGSI BARU: Dipanggil oleh ViewModel untuk inisialisasi
     */
    fun initUser(uid: String) {
        Log.d("TaskRepository", "Inisialisasi user: $uid")
        // Buat referensi spesifik ke data milik user ini
        taskRef = database.reference.child("users").child(uid).child("tasks")
        checklistRef = database.reference.child("users").child(uid).child("checklists")
    }

    fun setSortBy(sort:Pair<String,Boolean>){
        _sortByLiveData.postValue(sort)
    }

    // --- FUNGSI TASK (DITULIS ULANG) ---

    // **INI ADALAH FUNGSI getTaskList YANG SUDAH DIPERBAIKI**
    // GANTI FUNGSI LAMA ANDA DENGAN YANG INI

    fun getTaskList(isAsc : Boolean, sortByName:String) {
        if (taskRef == null) {
            _taskStateFlow.value = Error("User not initialized")
            return
        }

        _taskStateFlow.value = Loading()

        val dbSortColumn = if (sortByName == "title") "title" else "date"
        val query = taskRef!!.orderByChild(dbSortColumn)

        // --- MODIFIKASI DIMULAI ---
        // Hapus listener lama jika ada, sebelum memasang yang baru
        if (currentTaskListener != null && currentTaskQuery != null) {
            currentTaskQuery!!.removeEventListener(currentTaskListener!!)
        }

        currentTaskQuery = query // Simpan query yang sedang aktif

        // Buat dan simpan listener yang baru
        currentTaskListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<Task>()
                for (childSnapshot in snapshot.children) {
                    try {
                        val task = childSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            task.id = childSnapshot.key ?: task.id
                            taskList.add(task)
                        }
                    } catch (e: Exception) {
                        Log.e("TaskRepository", "Gagal parse Task: ${e.message}")
                    }
                }
                if (!isAsc) taskList.reverse()
                _taskStateFlow.value = Success("Loaded", taskList)
            }
            override fun onCancelled(error: DatabaseError) {
                _taskStateFlow.value = Error(error.message)
            }
        }

        // Pasang listener yang baru
        query.addValueEventListener(currentTaskListener!!)
        // --- MODIFIKASI SELESAI ---
    }


    fun insertTask(task: Task) {
        if (taskRef == null) {
            _statusLiveData.postValue(Error("User not initialized"))
            return
        }
        _statusLiveData.postValue(Loading())

        // Gunakan ID unik dari Firebase
        val taskId = taskRef!!.push().key ?: task.id
        task.id = taskId

        taskRef!!.child(taskId).setValue(task)
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Inserted Task Successfully", StatusResult.Added))
            }
            .addOnFailureListener { e ->
                _statusLiveData.postValue(Error(e.message.toString()))
            }
    }

    fun deleteTask(task: Task) {
        deleteTaskUsingId(task.id)
    }

    fun deleteTaskUsingId(taskId: String) {
        if (taskRef == null) {
            _statusLiveData.postValue(Error("User not initialized"))
            return
        }
        _statusLiveData.postValue(Loading())
        taskRef!!.child(taskId).removeValue()
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Deleted Task Successfully", StatusResult.Deleted))
            }
            .addOnFailureListener { e ->
                _statusLiveData.postValue(Error(e.message.toString()))
            }
    }

    fun updateTask(task: Task) {
        if (taskRef == null) {
            _statusLiveData.postValue(Error("User not initialized"))
            return
        }
        _statusLiveData.postValue(Loading())
        taskRef!!.child(task.id).setValue(task)
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Updated Task Successfully", StatusResult.Updated))
            }
            .addOnFailureListener { e ->
                _statusLiveData.postValue(Error(e.message.toString()))
            }
    }

    fun updateTaskPaticularField(taskId: String, title: String, description: String) {
        if (taskRef == null) {
            _statusLiveData.postValue(Error("User not initialized"))
            return
        }
        _statusLiveData.postValue(Loading())

        val updates = mapOf(
            "title" to title,
            "description" to description
            // Anda bisa tambahkan "date" jika ingin update timestamp
        )

        taskRef!!.child(taskId).updateChildren(updates)
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Updated Task Successfully", StatusResult.Updated))
            }
            .addOnFailureListener { e ->
                _statusLiveData.postValue(Error(e.message.toString()))
            }
    }

    // Fungsi searchTaskList sekarang harus me-query Firebase
    fun searchTaskList(query: String) {
        if (taskRef == null) {
            _taskStateFlow.value = Error("User not initialized")
            return
        }

        // --- MODIFIKASI DIMULAI ---
        // 1. Matikan listener "get all" yang sedang aktif
        if (currentTaskListener != null && currentTaskQuery != null) {
            currentTaskQuery!!.removeEventListener(currentTaskListener!!)
            currentTaskListener = null
            currentTaskQuery = null
        }

        // 2. Cek apakah query sekarang kosong (pencarian dihapus)
        if (query.isEmpty()) {
            // Jika kosong, hidupkan lagi listener "get all"
            val sortPair = _sortByLiveData.value ?: Pair("title", true)
            getTaskList(sortPair.second, sortPair.first)
            return // Selesai
        }
        // --- MODIFIKASI SELESAI ---

        // 3. Jika query TIDAK kosong, jalankan pencarian satu kali
        _taskStateFlow.value = Loading()

        val searchQuery = taskRef!!.orderByChild("title")
            .startAt(query)
            .endAt(query + "\uf8ff")

        searchQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<Task>()
                for (childSnapshot in snapshot.children) {
                    val task = childSnapshot.getValue(Task::class.java)
                    if (task != null) {
                        task.id = childSnapshot.key ?: task.id
                        taskList.add(task)
                    }
                }
                // Kirim hasil search
                _taskStateFlow.value = Success("Search complete", taskList)
            }
            override fun onCancelled(error: DatabaseError) {
                _taskStateFlow.value = Error(error.message)
            }
        })
    }

    // --- FUNGSI UNTUK CHECKLIST (DITULIS ULANG) ---

    fun insertChecklist(checklist: Checklist) {
        if (checklistRef == null) return
        _statusLiveData.postValue(Loading())

        val checklistId = checklistRef!!.push().key ?: checklist.id
        checklist.id = checklistId

        checklistRef!!.child(checklistId).setValue(checklist)
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Inserted Checklist Successfully", StatusResult.Added))
            }
            .addOnFailureListener { e -> _statusLiveData.postValue(Error(e.message.toString())) }
    }

    fun updateChecklist(checklist: Checklist) {
        if (checklistRef == null) return
        _statusLiveData.postValue(Loading())
        checklistRef!!.child(checklist.id).setValue(checklist)
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Updated Checklist Successfully", StatusResult.Updated))
            }
            .addOnFailureListener { e -> _statusLiveData.postValue(Error(e.message.toString())) }
    }

    fun deleteChecklist(checklist: Checklist) {
        if (checklistRef == null) return
        _statusLiveData.postValue(Loading())
        checklistRef!!.child(checklist.id).removeValue()
            .addOnSuccessListener {
                _statusLiveData.postValue(Success("Deleted Checklist Successfully", StatusResult.Deleted))
            }
            .addOnFailureListener { e -> _statusLiveData.postValue(Error(e.message.toString())) }
    }

    // --- (Ini adalah bagian yang diamati oleh MediatorLiveData) ---
    // Kita akan membuat LiveData kustom yang membersihkan listener-nya
    fun getAllChecklistsLiveData(isAsc: Boolean, sortByName: String): LiveData<List<Checklist>> {
        if (checklistRef == null) return MutableLiveData() // Kembalikan data kosong

        val dbSortColumn = if (sortByName == "title") "Title" else "createdDate"
        val query = checklistRef!!.orderByChild(dbSortColumn)

        // LiveData kustom
        return object : LiveData<List<Checklist>>() {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val checklistList = mutableListOf<Checklist>()
                    for (child in snapshot.children) {
                        try {
                            val item = child.getValue(Checklist::class.java)
                            if (item != null) {
                                item.id = child.key ?: item.id
                                checklistList.add(item)
                            }
                        } catch (e: Exception) { Log.e("TaskRepo", "Gagal parse Checklist") }
                    }
                    if (!isAsc) checklistList.reverse()
                    value = checklistList // Post value ke LiveData
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskRepo", "Checklist listener error: ${error.message}")
                }
            }

            // Saat LiveData aktif, pasang listener
            override fun onActive() {
                super.onActive()
                query.addValueEventListener(listener)
            }

            // Saat LiveData tidak aktif, hapus listener
            override fun onInactive() {
                super.onInactive()
                query.removeEventListener(listener)
            }
        }
    }

    // --- (Fungsi searchChecklist) ---
    fun searchChecklist(query: String, isAsc: Boolean, sortByName: String): LiveData<List<Checklist>> {
        if (checklistRef == null) return MutableLiveData()

        // --- PERBAIKAN ---
        // Query pencarian HARUS SELALU berdasarkan "title".
        // Kita tidak bisa menggunakan dbSortColumn di sini.
        val searchQuery = checklistRef!!.orderByChild("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
        // --- AKHIR PERBAIKAN ---

        // Kita gunakan LiveData kustom lagi
        return object : LiveData<List<Checklist>>() {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Checklist>()
                    for (child in snapshot.children) {
                        val item = child.getValue(Checklist::class.java)
                        if (item != null) {
                            item.id = child.key ?: item.id
                            list.add(item)
                        }
                    }
                    // Kita tidak bisa mengurutkan di sini karena query Firebase
                    // sudah mengurutkan berdasarkan 'title'.
                    // if (!isAsc) list.reverse() // (Hapus pengurutan 'isAsc' saat mencari)
                    value = list
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            override fun onActive() { searchQuery.addValueEventListener(listener) }
            override fun onInactive() { searchQuery.removeEventListener(listener) }
        }
    }

    // --- (Fungsi getChecklistById) ---
    fun getChecklistById(checklistId: String): LiveData<Checklist> {
        if (checklistRef == null) return MutableLiveData()

        val query = checklistRef!!.child(checklistId)

        return object : LiveData<Checklist>() {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = snapshot.getValue(Checklist::class.java)
                    item?.id = snapshot.key ?: ""
                    value = item // Post checklist tunggal
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            override fun onActive() { query.addValueEventListener(listener) }
            override fun onInactive() { query.removeEventListener(listener) }
        }
    }

    // --- FUNGSI INTERNAL ---
    // (Fungsi ini tidak lagi kita perlukan karena kita menggunakan addOnSuccessListener)
    /*
    private fun handleResult(result: Int, message: String, statusResult: StatusResult) {
        if (result != -1) {
            _statusLiveData.postValue(Success(message, statusResult))
        } else {
            _statusLiveData.postValue(Error("Something Went Wrong", statusResult))
        }
    }
    */
}