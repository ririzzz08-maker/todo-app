package com.coding.meet.todo_app

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.coding.meet.todo_app.adapters.TaskRVVBListAdapter
import com.coding.meet.todo_app.databinding.ActivityMainBinding
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Status
import com.coding.meet.todo_app.utils.StatusResult
import com.coding.meet.todo_app.utils.StatusResult.Added
import com.coding.meet.todo_app.utils.StatusResult.Deleted
import com.coding.meet.todo_app.utils.StatusResult.Updated
import com.coding.meet.todo_app.utils.clearEditText
import com.coding.meet.todo_app.utils.hideKeyBoard
import com.coding.meet.todo_app.utils.longToastShow
import com.coding.meet.todo_app.utils.setupDialog
import com.coding.meet.todo_app.utils.validateEditText
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var drawerLayout: DrawerLayout

    private val addTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.add_task_dialog)
        }
    }

    private val updateTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.update_task_dialog)
        }
    }

    private val loadingDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(this)[TaskViewModel::class.java]
    }

    private val isListMutableLiveData = MutableLiveData<Boolean>().apply {
        postValue(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        // Inisialisasi drawerLayout dari binding
        drawerLayout = mainBinding.drawerLayout

        // Pastikan drawer tertutup saat aplikasi mulai tanpa animasi
        drawerLayout.closeDrawer(GravityCompat.START, false)

        // Tombol menu untuk membuka drawer
        mainBinding.menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Tangani klik area konten utama agar drawer tertutup saat pengguna mengetuk area tersebut
        mainBinding.nestedScrollView.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        // Listener elemen UI lainnya (chipContainer, profileCard, iconFilter, iconSort, tvTelusuri)
        mainBinding.chipContainer.setOnClickListener {
            Toast.makeText(this, "Chip Container Diklik", Toast.LENGTH_SHORT).show()
        }

        mainBinding.profileCard.setOnClickListener {
            Toast.makeText(this, "Tombol Profil Diklik", Toast.LENGTH_SHORT).show()
        }

        mainBinding.iconFilter.setOnClickListener {
            showSortBottomSheet()
        }

        mainBinding.iconSort.setOnClickListener {
            Toast.makeText(this, "Mengubah Urutan Sortir", Toast.LENGTH_SHORT).show()
        }

        mainBinding.tvTelusuri.setOnClickListener {
            mainBinding.edSearchActual.visibility = View.VISIBLE
            mainBinding.edSearchEdit.requestFocus()
            Toast.makeText(this, "Input Pencarian Aktif", Toast.LENGTH_SHORT).show()
        }

        // Setup dialogs, RecyclerView, LiveData, dan semua fungsi lainnya
        setupAddTaskDialog()
        setupUpdateTaskDialog()
        setupRecyclerView()

        callGetTaskList()
        callSortByLiveData()
        statusCallback()
        setupSearch()
    }

    // Fungsi Setup Dialog Add Task
    private fun setupAddTaskDialog() {
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }

        val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEditText(addETTitle, addETTitleL)
            }
        })

        val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEditText(addETDesc, addETDescL)
            }
        })

        mainBinding.addTaskFABtn.setOnClickListener {
            clearEditText(addETTitle, addETTitleL)
            clearEditText(addETDesc, addETDescL)
            addTaskDialog.show()
        }

        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveTaskBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL) && validateEditText(addETDesc, addETDescL)) {
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    Date()
                )
                hideKeyBoard(it)
                addTaskDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }
    }

    // Fungsi Setup Dialog Update Task
    private fun setupUpdateTaskDialog() {
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        updateETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEditText(updateETTitle, updateETTitleL)
            }
        })

        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
        updateETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEditText(updateETDesc, updateETDescL)
            }
        })

        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }
    }

    // Fungsi Setup RecyclerView dan Adapter
    private fun setupRecyclerView() {
        isListMutableLiveData.observe(this) {
            mainBinding.taskRV.layoutManager = if (it) {
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            } else {
                StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }
        }

        val adapter = TaskRVVBListAdapter(isListMutableLiveData) { type, _, task ->
            when (type) {
                "delete" -> {
                    taskViewModel.deleteTaskUsingId(task.id)
                    restoreDeletedTask(task)
                }
                "update" -> {
                    val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
                    val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
                    val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
                    val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
                    val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)

                    updateETTitle.setText(task.title)
                    updateETDesc.setText(task.description)

                    updateTaskBtn.setOnClickListener {
                        if (validateEditText(updateETTitle, updateETTitleL) &&
                            validateEditText(updateETDesc, updateETDescL)
                        ) {
                            val updatedTask = Task(
                                task.id,
                                updateETTitle.text.toString().trim(),
                                updateETDesc.text.toString().trim(),
                                Date()
                            )
                            hideKeyBoard(it)
                            updateTaskDialog.dismiss()
                            taskViewModel.updateTask(updatedTask)
                        }
                    }
                    updateTaskDialog.show()
                }
            }
        }

        mainBinding.taskRV.adapter = adapter
        ViewCompat.setNestedScrollingEnabled(mainBinding.taskRV, false)

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                mainBinding.nestedScrollView.smoothScrollTo(0, positionStart)
            }
        })
    }

    private fun restoreDeletedTask(deletedTask: Task) {
        Snackbar.make(mainBinding.root, "Deleted '${deletedTask.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                taskViewModel.insertTask(deletedTask)
            }.show()
    }

    private fun setupSearch() {
        mainBinding.edSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(query: Editable?) {
                if (!query.isNullOrEmpty()) {
                    taskViewModel.searchTaskList(query.toString())
                } else {
                    callSortByLiveData()
                }
            }
        })

        mainBinding.edSearchEdit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyBoard(v)
                true
            } else false
        }
    }

    private fun callSortByLiveData() {
        taskViewModel.sortByLiveData.observe(this) {
            taskViewModel.getTaskList(it.second, it.first)
        }
    }

    private fun callGetTaskList() {
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel.taskStateFlow.collectLatest { statusResult ->  // ganti 'it' menjadi statusResult
                Log.d("status", statusResult.status.toString())
                when (statusResult.status) {
                    Status.LOADING -> loadingDialog.show()
                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        statusResult.data?.collect { taskList ->            // ganti 'it' menjadi taskList
                            (mainBinding.taskRV.adapter as? TaskRVVBListAdapter)?.submitList(taskList)
                        }
                    }
                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        statusResult.message?.let { longToastShow(it) }
                    }
                }
            }
        }
    }


    private fun statusCallback() {
        taskViewModel.statusLiveData.observe(this) { statusResponse -> // ganti 'it' dengan 'statusResponse'
            when (statusResponse.status) {
                Status.LOADING -> loadingDialog.show()
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    when (statusResponse.data as StatusResult) { // ganti 'it' menjadi 'statusResponse'
                        Added -> Log.d("StatusResult", "Added")
                        Deleted -> Log.d("StatusResult", "Deleted")
                        Updated -> Log.d("StatusResult", "Updated")
                    }
                    statusResponse.message?.let { message -> longToastShow(message) }
                }
                Status.ERROR -> {
                    loadingDialog.dismiss()
                    statusResponse.message?.let { message -> longToastShow(message) }
                }
            }
        }
    }
    private fun showSortBottomSheet() {
        try {
            val bottomSheet = SortBottomSheetFragment()
            bottomSheet.setOnSortSelectedListener { sortType ->
                applySorting(sortType)
            }
            bottomSheet.show(supportFragmentManager, SortBottomSheetFragment.TAG)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showSortBottomSheet", e)
        }
    }

    private fun applySorting(sortType: String) {
        when (sortType) {
            "custom" -> {
                taskViewModel.setSortBy(Pair("title", true))
                Toast.makeText(this, "Urutan: Kustom (Judul A-Z)", Toast.LENGTH_SHORT).show()
            }
            "created_date" -> {
                taskViewModel.setSortBy(Pair("date", true))
                Toast.makeText(this, "Urutan: Tanggal Dibuat", Toast.LENGTH_SHORT).show()
            }
            "modified_date" -> {
                taskViewModel.setSortBy(Pair("date", false))
                Toast.makeText(this, "Urutan: Tanggal Diubah", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
