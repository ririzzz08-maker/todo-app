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

    // --- (Variabel Dialog tetap sama) ---
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

    // --- ViewModel & Adapters ---
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private lateinit var taskRVVBListAdapter: TaskRVVBListAdapter
    private lateinit var checklistMainAdapter: ChecklistMainAdapter
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply { postValue(true) }

    // Variabel Kamera/Gambar (harus nullable)
    private var imageUri: Uri? = null
    // Views dialog yang diakses dari launcher harus nullable
    private var ivTaskImage: ImageView? = null
    private var ivRemoveImage: ImageView? = null
    private var btnPickImage: ImageView? = null

    // Variabel Launcher (diperbaiki agar menggunakan safe call 'ivTaskImage?')
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) startCamera() else Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            imageUri?.let { uri ->
                ivTaskImage?.setImageURI(uri) // Safe call
                ivTaskImage?.visibility = View.VISIBLE
                ivRemoveImage?.visibility = View.VISIBLE
            }
        } else Toast.makeText(requireContext(), "Gagal mengambil gambar.", Toast.LENGTH_SHORT).show()
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            ivTaskImage?.setImageURI(it) // Safe call
            ivTaskImage?.visibility = View.VISIBLE
            ivRemoveImage?.visibility = View.VISIBLE
        } ?: run { Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih.", Toast.LENGTH_SHORT).show() }
    }

    // --- Variabel Views Global Fragment yang Aman ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
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

        // --- 1. Hubungkan Views Fragment ---
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

        // --- 2. Panggil Setup ---
        setupDialogs() // Views Add Dialog ditemukan di sini
        setupTaskRecyclerView()
        setupChecklistRecyclerView()
        setupNavigationDrawer() // Views Update Dialog ditemukan di sini
        setupHeaderListeners()
        setupSearch()

        // --- 3. Panggil Data ---
        callGetTaskList()
        observeChecklistData()
        callSortByLiveData()
        statusCallback()
    }

    // ==========================================================
    // CHECKLIST RECYCLERVIEW SETUP
    // ==========================================================
    private fun setupChecklistRecyclerView() {
        checklistMainAdapter = ChecklistMainAdapter(
            isList = isListMutableLiveData,
            onEditClick = { checklist ->
                val bundle = Bundle().apply {
                    putString("checklist_id", checklist.id)
                }
                findNavController().navigate(R.id.action_taskListFragment_to_editChecklistFragment, bundle)
            },
            onDeleteClick = { checklist ->
                taskViewModel.deleteChecklist(checklist)
                restoreDeletedChecklist(checklist)
            }
        )
        checklistRV.adapter = checklistMainAdapter

        // Observer untuk Layout Manager
        isListMutableLiveData.observe(viewLifecycleOwner) { isList ->
            if (isList) {
                checklistRV.layoutManager = LinearLayoutManager(requireContext())
            } else {
                checklistRV.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }
            if (::checklistMainAdapter.isInitialized) {
                checklistMainAdapter.notifyDataSetChanged()
            }
        }

        ViewCompat.setNestedScrollingEnabled(checklistRV, false)
    }

    private fun restoreDeletedChecklist(deletedChecklist: Checklist) {
        Snackbar.make(requireView(), "Deleted '${deletedChecklist.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") { taskViewModel.insertChecklist(deletedChecklist) }.show()
    }

    private fun observeChecklistData() {
        checklistObserver?.removeObservers(viewLifecycleOwner)
        checklistObserver = taskViewModel.allChecklists
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
        // Observer untuk Layout Manager
        isListMutableLiveData.observe(viewLifecycleOwner) {
            if (it) {
                taskRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            } else {
                taskRV.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }
            if (::taskRVVBListAdapter.isInitialized) {
                taskRVVBListAdapter.notifyDataSetChanged()
            }
        }

        taskRVVBListAdapter = TaskRVVBListAdapter(isList = isListMutableLiveData) { type, position, task ->
            if (type == "delete") {
                taskViewModel.deleteTaskUsingId(task.id)
                restoreDeletedTask(task)
            } else if (type == "update") {
                // Views update dialog ditemukan lokal di sini
                val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
                val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)
                val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
                val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL )

                updateETTitle.setText(task.title)
                updateETDesc.setText(task.description)
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL)
                        && validateEditText(updateETDesc, updateETDescL)
                    ) {
                        // FIX: Tambah imagePath
                        val updateTask = Task(task.id, updateETTitle.text.toString().trim(), updateETDesc.text.toString().trim(), Date(), task.imagePath)
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
        // Views untuk update dialog (ditemukan lokal di sini)
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL )
        val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }


        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START) else drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_tambah_label -> {
                    // Views add dialog ditemukan di setupDialogs, gunakan lokal
                    val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                    val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                    clearEditText(addETTitle, addTaskDialog.findViewById(R.id.edTaskTitleL))
                    clearEditText(addETDesc, addTaskDialog.findViewById(R.id.edTaskDescL ))
                    addTaskDialog.show()
                }
                R.id.nav_tugas -> {
                    val taskList = taskRVVBListAdapter.currentList
                    if (taskList.isNotEmpty()) {
                        val taskToUpdate = taskList[0]
                        updateETTitle.setText(taskToUpdate.title)
                        updateETDesc.setText(taskToUpdate.description)
                        updateTaskBtn.setOnClickListener {
                            if (validateEditText(updateETTitle, updateETDescL) && validateEditText(updateETDesc, updateETDescL)) {
                                // FIX: Tambah imagePath
                                val updatedTask = Task(taskToUpdate.id, updateETTitle.text.toString().trim(), updateETDesc.text.toString().trim(), Date(), taskToUpdate.imagePath)
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
        chipContainer.setOnClickListener { Toast.makeText(requireContext(), "Chip Container Diklik", Toast.LENGTH_SHORT).show() }
        profileCard.setOnClickListener { findNavController().navigate(R.id.profileFragment) }

        // Tombol Sort (panah)
        iconFilter.setOnClickListener {
            showSortBottomSheet()
        }

        // Tombol Grid/List
        iconSort.setOnClickListener {
            val currentIsList = isListMutableLiveData.value ?: true
            isListMutableLiveData.postValue(!currentIsList)

            if (!currentIsList) {
                // Sekarang mode List, tampilkan ikon Grid
                iconSort.setImageResource(R.drawable.grid)
            } else {
                // Sekarang mode Grid, tampilkan ikon List
                iconSort.setImageResource(R.drawable.list)
            }
        }

        tvTelusuri.setOnClickListener {
            edSearchActual.visibility = View.VISIBLE
            edSearchEdit.requestFocus()
            Toast.makeText(requireContext(), "Input Pencarian Aktif", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDialogs() {
        // Views Add Dialog diinisialisasi secara lokal di sini
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        val dialogAddETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
        val dialogAddETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val dialogAddETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
        val dialogAddETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL )

        ivRemoveImage = addTaskDialog.findViewById(R.id.ivRemoveImage)
        ivTaskImage = addTaskDialog.findViewById(R.id.ivTaskImage)
        btnPickImage = addTaskDialog.findViewById(R.id.btnPickImage)

        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }

        ivRemoveImage?.setOnClickListener {
            imageUri = null
            ivTaskImage?.setImageURI(null)
            ivTaskImage?.visibility = View.GONE
            ivRemoveImage?.visibility = View.GONE
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }

        dialogAddETTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) { validateEditText(dialogAddETTitle, dialogAddETTitleL) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        dialogAddETDesc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) { validateEditText(dialogAddETDesc, dialogAddETDescL) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        addTaskFABtn.setOnClickListener {
            val bottomSheet = AddOptionsBottomSheetFragment()
            bottomSheet.listener = this
            bottomSheet.show(parentFragmentManager, AddOptionsBottomSheetFragment.TAG)
        }

        btnPickImage?.setOnClickListener {
            val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
            imageSourceBottomSheet.listener = this
            imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
        }

        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(dialogAddETTitle, dialogAddETTitleL) && validateEditText(dialogAddETDesc, dialogAddETDescL)) {
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    dialogAddETTitle.text.toString().trim(),
                    dialogAddETDesc.text.toString().trim(),
                    Date(),
                    imageUri?.toString()
                )
                imageUri = null
                requireContext().hideKeyBoard(it)
                addTaskDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }
    }

    private fun showSortBottomSheet() {
        try {
            val bottomSheet = SortBottomSheetFragment()
            bottomSheet.setOnSortSelectedListener { sortType ->
                applySorting(sortType)
            }
            bottomSheet.show(parentFragmentManager, SortBottomSheetFragment.TAG)
        } catch (e: Exception) {
            Log.e("TaskListFragment", "Error showSortBottomSheet", e)
        }
    }

    private fun applySorting(sortType: String) {
        when (sortType) {
            "custom" -> {
                taskViewModel.setSortBy(Pair("title", true))
            }
            "created_date" -> {
                taskViewModel.setSortBy(Pair("date", true))
            }
            "modified_date" -> {
                taskViewModel.setSortBy(Pair("date", false))
            }
        }
    }

    private fun restoreDeletedTask(deletedTask: Task) {
        Snackbar.make(requireView(), "Deleted '${deletedTask.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") { taskViewModel.insertTask(deletedTask) }.show()
    }

    private fun setupSearch() {
        edSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(query: Editable) {
                val queryString = query.toString().trim()

                val sortPair = taskViewModel.sortByLiveData.value ?: Pair("title", true)
                val sortByName = sortPair.first
                val isAsc = sortPair.second

                if (queryString.isNotEmpty()) {
                    taskViewModel.searchTaskList(queryString)
                    checklistObserver?.removeObservers(viewLifecycleOwner)
                    checklistObserver = taskViewModel.searchChecklist(queryString, isAsc, sortByName)
                    observeChecklistDataAfterSearch()
                } else {
                    callSortByLiveData()
                    observeChecklistData()
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

    private fun observeChecklistDataAfterSearch() {
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

    private fun callSortByLiveData() {
        taskViewModel.sortByLiveData.observe(viewLifecycleOwner) {
            taskViewModel.getTaskList(it.second, it.first)
        }
    }

    private fun statusCallback() {
        taskViewModel.statusLiveData.observe(viewLifecycleOwner) { statusResponse ->
            when (statusResponse.status) {
                Status.LOADING -> loadingDialog.show()
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    statusResponse.message?.let { message ->
                        if (statusResponse.data == StatusResult.Added || statusResponse.data == StatusResult.Updated) {
                            Log.d("StatusResult", message)
                        } else {
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
        val photosDir = File(requireContext().externalCacheDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val newPhotoFile = File(photosDir, "${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", newPhotoFile)
        takePictureLauncher.launch(imageUri)
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "list" -> {
                findNavController().navigate(R.id.action_taskListFragment_to_editChecklistFragment)
            }
            "catatan" -> {
                // Views add dialog ditemukan di setupDialogs, gunakan lokal
                val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)

                clearEditText(addETTitle, addTaskDialog.findViewById(R.id.edTaskTitleL))
                clearEditText(addETDesc, addTaskDialog.findViewById(R.id.edTaskDescL ))

                imageUri = null
                ivTaskImage?.setImageURI(null)
                ivTaskImage?.visibility = View.GONE
                ivRemoveImage?.visibility = View.GONE

                addTaskDialog.show()
            }
            "gambar" -> {
                val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
                val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL )

                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)
                addTaskDialog.show()

                val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
                imageSourceBottomSheet.listener = this
                imageSourceBottomSheet.show(parentFragmentManager, AddOptionsBottomSheetFragment.TAG)
            }
        }
    }

    override fun onSourceSelected(source: String) {
        when (source) {
            ImageSourceBottomSheetFragment.SOURCE_CAMERA -> requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            ImageSourceBottomSheetFragment.SOURCE_GALLERY -> pickImageLauncher.launch("image/*")
        }
    }
}