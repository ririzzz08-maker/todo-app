package com.coding.meet.todo_app

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.coding.meet.todo_app.adapters.ChecklistMainAdapter
import com.coding.meet.todo_app.adapters.TaskRVVBListAdapter
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Status
import com.coding.meet.todo_app.utils.StatusResult
import com.coding.meet.todo_app.utils.clearEditText
import com.coding.meet.todo_app.utils.hideKeyBoard
import com.coding.meet.todo_app.utils.longToastShow
import com.coding.meet.todo_app.utils.setupDialog
import com.coding.meet.todo_app.utils.validateEditText
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID

class TaskListFragment : Fragment(), AddOptionsBottomSheetFragment.AddOptionsListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    // ... (Semua variabel dialog, viewmodel, dll tetap sama) ...
    // --- Variabel Global untuk Dialog ---
    private val addTaskDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.add_task_dialog)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setGravity(Gravity.CENTER)
        }
    }
    private val updateTaskDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.update_task_dialog)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setGravity(Gravity.CENTER)
        }
    }
    private val loadingDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    // --- Variabel Global untuk ViewModel dan Adapter ---
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private lateinit var taskRVVBListAdapter: TaskRVVBListAdapter
    private lateinit var checklistMainAdapter: ChecklistMainAdapter
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply { postValue(true) }

    // Variabel Kamera/Gambar
    private var imageUri: Uri? = null
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) startCamera() else Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            imageUri?.let { uri ->
                ivTaskImage.setImageURI(uri)
                ivTaskImage.visibility = View.VISIBLE
                ivRemoveImage.visibility = View.VISIBLE
            }
        } else Toast.makeText(requireContext(), "Gagal mengambil gambar.", Toast.LENGTH_SHORT).show()
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            ivTaskImage.setImageURI(it)
            ivTaskImage.visibility = View.VISIBLE
            ivRemoveImage.visibility = View.VISIBLE
        } ?: run { Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih.", Toast.LENGTH_SHORT).show() }
    }

    // --- Variabel Global untuk Navigasi & Views ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var updateETTitle: TextInputEditText
    private lateinit var updateETTitleL: TextInputLayout
    private lateinit var updateETDesc: TextInputEditText
    private lateinit var updateETDescL: TextInputLayout
    private lateinit var updateTaskBtn: Button
    private lateinit var addETTitle: TextInputEditText
    private lateinit var addETTitleL: TextInputLayout
    private lateinit var addETDesc: TextInputEditText
    private lateinit var addETDescL: TextInputLayout
    private lateinit var btnPickImage: ImageView
    private lateinit var ivTaskImage: ImageView
    private lateinit var menuButton: ImageButton
    private lateinit var chipContainer: ConstraintLayout
    private lateinit var profileCard: CardView
    private lateinit var iconFilter: ImageButton
    private lateinit var iconSort: ImageButton
    private lateinit var tvTelusuri: TextView
    private lateinit var edSearchActual: TextInputLayout
    private lateinit var edSearchEdit: TextInputEditText
    private lateinit var taskRV: RecyclerView
    private lateinit var addTaskFABtn: ExtendedFloatingActionButton
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var ivRemoveImage: ImageView
    private lateinit var checklistRV: RecyclerView
    private lateinit var tvChecklistHeader: TextView
    private lateinit var tvTaskHeader: TextView
    private var checklistObserver: LiveData<List<Checklist>>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Hubungkan semua View ---
        menuButton = view.findViewById(R.id.menu_button)
        chipContainer = view.findViewById(R.id.chipContainer)
        profileCard = view.findViewById(R.id.profileCard)
        iconFilter = view.findViewById(R.id.icon_filter)
        iconSort = view.findViewById(R.id.icon_sort)
        tvTelusuri = view.findViewById(R.id.tv_telusuri)
        edSearchActual = view.findViewById(R.id.edSearchActual)
        edSearchEdit = view.findViewById(R.id.edSearchEdit)
        taskRV = view.findViewById(R.id.taskRV)
        addTaskFABtn = view.findViewById(R.id.addTaskFABtn)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        navView = requireActivity().findViewById(R.id.nav_view)
        checklistRV = view.findViewById(R.id.checklistRV)
        tvChecklistHeader = view.findViewById(R.id.tvChecklistHeader)
        tvTaskHeader = view.findViewById(R.id.tvTaskHeader)

        // --- 2. Panggil semua fungsi setup ---
        setupDialogs()
        setupTaskRecyclerView()
        setupChecklistRecyclerView()
        setupNavigationDrawer()
        setupHeaderListeners()
        setupSearch()

        // --- 3. Panggil fungsi data ---
        callGetTaskList()
        observeChecklistData(taskViewModel.allChecklists)
        callSortByLiveData()
        statusCallback()
    }

    // ==========================================================
    // INI ADALAH FUNGSI YANG DIPERBARUI
    // ==========================================================
    private fun setupChecklistRecyclerView() {
        checklistMainAdapter = ChecklistMainAdapter(
            onEditClick = { checklist ->
                // Navigasi ke Edit
                val bundle = Bundle().apply {
                    putString("checklist_id", checklist.id)
                }
                findNavController().navigate(R.id.action_taskListFragment_to_editChecklistFragment, bundle)
            },
            onDeleteClick = { checklist ->
                // BARU: Panggil ViewModel untuk Hapus
                taskViewModel.deleteChecklist(checklist)

                // BARU: Tampilkan Snackbar dengan "Undo"
                restoreDeletedChecklist(checklist)
            }
        )
        checklistRV.adapter = checklistMainAdapter
        checklistRV.layoutManager = LinearLayoutManager(requireContext())
        ViewCompat.setNestedScrollingEnabled(checklistRV, false)
    }
    // ==========================================================

    // BARU: Fungsi untuk mengembalikan checklist yang dihapus
    private fun restoreDeletedChecklist(deletedChecklist: Checklist) {
        Snackbar.make(requireView(), "Deleted '${deletedChecklist.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                // Panggil insert lagi untuk mengembalikannya
                taskViewModel.insertChecklist(deletedChecklist)
            }.show()
    }

    private fun observeChecklistData(data: LiveData<List<Checklist>>) {
        // ... (Kode tetap sama) ...
        checklistObserver?.removeObservers(viewLifecycleOwner)
        checklistObserver = data
        checklistObserver?.observe(viewLifecycleOwner) { listChecklist ->
            checklistMainAdapter.submitList(listChecklist)
            if (listChecklist.isNullOrEmpty()) {
                tvChecklistHeader.visibility = View.GONE
                checklistRV.visibility = View.GONE
            } else {
                tvChecklistHeader.visibility = View.VISIBLE
                checklistRV.visibility = View.VISIBLE
            }
        }
    }

    private fun callGetTaskList() {
        // ... (Kode tetap sama) ...
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel.taskStateFlow.collectLatest { statusResult ->
                when (statusResult.status) {
                    Status.LOADING -> loadingDialog.show()
                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        statusResult.data?.collect { taskList ->
                            taskRVVBListAdapter.submitList(taskList)
                            if (taskList.isNotEmpty()) {
                                tvTaskHeader.visibility = View.VISIBLE
                            } else {
                                tvTaskHeader.visibility = View.GONE
                            }
                        }
                    }
                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        statusResult.message?.let { requireContext().longToastShow(it) }
                    }
                }
            }
        }
    }

    private fun setupTaskRecyclerView() {
        // ... (Kode tetap sama) ...
        isListMutableLiveData.observe(viewLifecycleOwner) {
            if (it) {
                taskRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            } else {
                taskRV.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }
        }
        taskRVVBListAdapter = TaskRVVBListAdapter(isListMutableLiveData) { type, position, task ->
            if (type == "delete") {
                taskViewModel.deleteTaskUsingId(task.id)
                restoreDeletedTask(task)
            } else if (type == "update") {
                updateETTitle.setText(task.title)
                updateETDesc.setText(task.description)
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL)
                        && validateEditText(updateETDesc, updateETDescL)
                    ) {
                        val updateTask = Task(task.id, updateETTitle.text.toString().trim(), updateETDesc.text.toString().trim(), Date())
                        requireContext().hideKeyBoard(it)
                        updateTaskDialog.dismiss()
                        taskViewModel.updateTask(updateTask)
                    }
                }
                updateTaskDialog.show()
            }
        }
        taskRV.adapter = taskRVVBListAdapter
        ViewCompat.setNestedScrollingEnabled(taskRV, false)
        taskRVVBListAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
            }
        })
    }

    private fun setupNavigationDrawer() {
        // ... (Kode tetap sama) ...
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START) else drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_tambah_label -> {
                    clearEditText(addETTitle, addETTitleL)
                    clearEditText(addETDesc, addETDescL)
                    addTaskDialog.show()
                }
                R.id.nav_tugas -> {
                    val taskList = taskRVVBListAdapter.currentList
                    if (taskList.isNotEmpty()) {
                        val taskToUpdate = taskList[0]
                        updateETTitle.setText(taskToUpdate.title)
                        updateETDesc.setText(taskToUpdate.description)
                        updateTaskBtn.setOnClickListener {
                            if (validateEditText(updateETTitle, updateETTitleL) && validateEditText(updateETDesc, updateETDescL)) {
                                val updatedTask = Task(taskToUpdate.id, updateETTitle.text.toString().trim(), updateETDesc.text.toString().trim(), Date())
                                requireContext().hideKeyBoard(it)
                                updateTaskDialog.dismiss()
                                taskViewModel.updateTask(updatedTask)
                            }
                        }
                        updateTaskDialog.show()
                    } else {
                        Toast.makeText(requireContext(), "Belum ada tugas untuk diedit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupHeaderListeners() {
        // ... (Kode tetap sama) ...
        chipContainer.setOnClickListener { Toast.makeText(requireContext(), "Chip Container Diklik", Toast.LENGTH_SHORT).show() }
        profileCard.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
        iconFilter.setOnClickListener { showSortBottomSheet() }
        iconSort.setOnClickListener { Toast.makeText(requireContext(), "Mengubah Urutan Sortir", Toast.LENGTH_SHORT).show() }
        tvTelusuri.setOnClickListener {
            edSearchActual.visibility = View.VISIBLE
            edSearchEdit.requestFocus()
            Toast.makeText(requireContext(), "Input Pencarian Aktif", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDialogs() {
        // ... (Kode tetap sama) ...
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }
        addETTitle = addTaskDialog.findViewById(R.id.addETTitle)
        addETTitleL = addTaskDialog.findViewById(R.id.edTaskTitleL)
        ivRemoveImage = addTaskDialog.findViewById(R.id.ivRemoveImage)
        ivRemoveImage.setOnClickListener {
            imageUri = null
            ivTaskImage.setImageURI(null)
            ivTaskImage.visibility = View.GONE
            ivRemoveImage.visibility = View.GONE
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }
        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) { validateEditText(addETTitle, addETTitleL) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        addETDesc = addTaskDialog.findViewById(R.id.addETDesc)
        addETDescL = addTaskDialog.findViewById(R.id.edTaskDescL)
        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) { validateEditText(addETDesc, addETDescL) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        addTaskFABtn.setOnClickListener {
            val bottomSheet = AddOptionsBottomSheetFragment()
            bottomSheet.listener = this
            bottomSheet.show(parentFragmentManager, AddOptionsBottomSheetFragment.TAG)
        }
        btnPickImage = addTaskDialog.findViewById(R.id.btnPickImage)
        ivTaskImage = addTaskDialog.findViewById(R.id.ivTaskImage)
        btnPickImage.setOnClickListener {
            val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
            imageSourceBottomSheet.listener = this
            imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
        }
        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL) && validateEditText(addETDesc, addETDescL)) {
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    Date(),
                    imageUri?.toString() // <-- TAMBAHKAN INI UNTUK MENYIMPAN GAMBAR
                )
// Jangan lupa bersihkan imageUri setelah menyimpan
                imageUri = null
                requireContext().hideKeyBoard(it)
                addTaskDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }
        updateETTitle = updateTaskDialog.findViewById(R.id.edTaskTitle)
        updateETTitleL = updateTaskDialog.findViewById(R.id.edTaskTitleL)
        updateETDesc = updateTaskDialog.findViewById(R.id.edTaskDesc)
        updateETDescL = updateTaskDialog.findViewById(R.id.edTaskDescL)
        updateTaskBtn = updateTaskDialog.findViewById(R.id.updateTaskBtn)
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }
    }

    private fun showSortBottomSheet() { /* ... (Kode Sort Sama) ... */ }
    private fun applySorting(sortType: String) { /* ... (Kode Sort Sama) ... */ }

    private fun restoreDeletedTask(deletedTask: Task) {
        // ... (Kode tetap sama) ...
        Snackbar.make(requireView(), "Deleted '${deletedTask.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") { taskViewModel.insertTask(deletedTask) }.show()
    }

    private fun setupSearch() {
        // ... (Kode tetap sama) ...
        edSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(query: Editable) {
                val queryString = query.toString().trim()
                if (queryString.isNotEmpty()) {
                    taskViewModel.searchTaskList(queryString)
                    observeChecklistData(taskViewModel.searchChecklist(queryString))
                } else {
                    callSortByLiveData()
                    observeChecklistData(taskViewModel.allChecklists)
                }
            }
        })
        edSearchEdit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                requireContext().hideKeyBoard(v)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun callSortByLiveData() {
        // ... (Kode tetap sama) ...
        taskViewModel.sortByLiveData.observe(viewLifecycleOwner) {
            taskViewModel.getTaskList(it.second, it.first)
        }
    }

    private fun statusCallback() {
        // ... (Kode tetap sama) ...
        taskViewModel.statusLiveData.observe(viewLifecycleOwner) { statusResponse ->
            when (statusResponse.status) {
                Status.LOADING -> loadingDialog.show()
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    statusResponse.message?.let { message ->
                        if (statusResponse.data == StatusResult.Added || statusResponse.data == StatusResult.Updated) {
                            Log.d("StatusResult", message)
                        } else {
                            // BARU: Tampilkan toast untuk 'Deleted'
                            requireContext().longToastShow(message)
                        }
                    }
                }
                Status.ERROR -> {
                    loadingDialog.dismiss()
                    statusResponse.message?.let { message -> requireContext().longToastShow(message) }
                }
            }
        }
    }

    private fun startCamera() {
        // ... (Kode tetap sama) ...
        val photosDir = File(requireContext().externalCacheDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val newPhotoFile = File(photosDir, "${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", newPhotoFile)
        takePictureLauncher.launch(imageUri)
    }

    // --- FUNGSI LISTENER (NAVIGASI) ---
    override fun onOptionSelected(option: String) {
        // ... (Kode tetap sama) ...
        when (option) {
            "list" -> {
                findNavController().navigate(R.id.action_taskListFragment_to_editChecklistFragment)
            }
            "catatan" -> {
                addETTitleL.hint = "Judul Catatan"
                addETDescL.hint = "Masukkan isi catatan"
                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)

                // BARU: Reset gambar sebelum dialog tampil
                imageUri = null
                ivTaskImage.setImageURI(null) // Hapus gambar dari ImageView
                ivTaskImage.visibility = View.GONE // Sembunyikan ImageView
                ivRemoveImage.visibility = View.GONE // Sembunyikan tombol hapus gambar

                addTaskDialog.show()
            }
            "gambar" -> {
                addETTitleL.hint = "Judul Catatan"
                addETDescL.hint = "Masukkan isi catatan"
                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)
                addTaskDialog.show()

                val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
                imageSourceBottomSheet.listener = this
                imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
            }
        }
    }

    override fun onSourceSelected(source: String) {
        // ... (Kode tetap sama) ...
        when (source) {
            ImageSourceBottomSheetFragment.SOURCE_CAMERA -> requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            ImageSourceBottomSheetFragment.SOURCE_GALLERY -> pickImageLauncher.launch("image/*")
        }
    }
}