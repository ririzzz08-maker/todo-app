package com.coding.meet.todo_app

import android.app.Dialog
import android.content.Intent
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull // Import ini tidak diperlukan dan bisa dihapus
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
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
import coil.load
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
// --- IMPORT FIREBASE ---
import com.google.firebase.auth.FirebaseAuth
// --- IMPORT CLOUDINARY ---
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
// --- ----------------- ---
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import android.content.Context
import android.view.inputmethod.InputMethodManager

class TaskListFragment : Fragment(), AddOptionsBottomSheetFragment.AddOptionsListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    private lateinit var auth: FirebaseAuth
    private var currentUid: String? = null
    private var taskToUpdate: Task? = null

    private val addTaskDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.add_task_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.CENTER)
        }
    }
    private val updateTaskDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.update_task_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.CENTER)
        }
    }
    private val loadingDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.custom_loading_dialog)
        }
    }

    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private lateinit var taskRVVBListAdapter: TaskRVVBListAdapter
    private lateinit var checklistMainAdapter: ChecklistMainAdapter
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply { postValue(true) }

    private var imageUri: Uri? = null
    private var ivTaskImage: ImageView? = null
    private var ivRemoveImage: ImageView? = null
    private var btnPickImage: ImageView? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) startCamera() else Toast.makeText(
                requireContext(),
                "Izin kamera ditolak.",
                Toast.LENGTH_SHORT
            ).show()
        }
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                imageUri?.let { uri ->
                    ivTaskImage?.setImageURI(uri)
                    ivTaskImage?.visibility = View.VISIBLE
                    ivRemoveImage?.visibility = View.VISIBLE
                }
            } else Toast.makeText(requireContext(), "Gagal mengambil gambar.", Toast.LENGTH_SHORT)
                .show()
        }
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                ivTaskImage?.setImageURI(it)
                ivTaskImage?.visibility = View.VISIBLE
                ivRemoveImage?.visibility = View.VISIBLE
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    "Tidak ada gambar yang dipilih.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
    private lateinit var addTaskFABtn: android.widget.ImageButton
    // Menghapus nestedScrollView karena tidak digunakan di sini dan menyebabkan error
    // private lateinit var nestedScrollView: NestedScrollView

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

        setupFirebaseAuth()

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
        // Menghapus inisialisasi nestedScrollView
        // nestedScrollView = view.findViewById(R.id.nestedScrollView)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        navView = requireActivity().findViewById(R.id.nav_view)
        checklistRV = view.findViewById(R.id.checklistRV)
        tvChecklistHeader = view.findViewById(R.id.tvChecklistHeader)
        tvTaskHeader = view.findViewById(R.id.tvTaskHeader)

        setupDialogs()
        setupTaskRecyclerView()
        setupChecklistRecyclerView()
        setupNavigationDrawer()
        setupHeaderListeners()
        setupSearch()
        observeTaskToEdit()
        observeChecklistToEdit()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (edSearchActual.visibility == View.VISIBLE) {
                    // Jika search bar terbuka, tutup search bar-nya
                    closeSearch()
                } else {
                    // Jika search bar tertutup, biarkan sistem yg handle
                    isEnabled = false
                    try {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } catch (e: IllegalStateException) {
                        // Menghindari crash jika ada navigasi ganda
                    }
                }
            }
        })
    }
    private fun observeTaskToEdit() {
        taskViewModel.taskToEdit.observe(viewLifecycleOwner) { task ->
            if (task != null) {
                // Ada task yang dipilih untuk diedit!
                // Gunakan logika Anda yang sudah ada untuk menampilkan dialog

                taskToUpdate = task // Set task global

                val dialogAddETTitle =
                    addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                val dialogAddETDesc =
                    addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                val ivTaskImage = addTaskDialog.findViewById<ImageView>(R.id.ivTaskImage)
                val ivRemoveImage = addTaskDialog.findViewById<ImageView>(R.id.ivRemoveImage)

                dialogAddETTitle.setText(task.title)
                dialogAddETDesc.setText(task.description)

                imageUri = null // Reset URI

                if (task.imagePath.isNullOrEmpty()) {
                    ivTaskImage?.visibility = View.GONE
                    ivRemoveImage?.visibility = View.GONE
                } else {
                    ivTaskImage?.visibility = View.VISIBLE
                    ivRemoveImage?.visibility = View.VISIBLE
                    ivTaskImage?.load(task.imagePath) {
                        crossfade(true)
                    }
                }

                // Tampilkan dialog
                addTaskDialog.show()

                // Beri tahu ViewModel bahwa kita sudah menangani event ini
                taskViewModel.doneEditingTask()
            }
        }
    }


    // Di dalam kelas TaskListFragment.kt

    /**
     * Fungsi helper untuk menutup & mereset search bar
     */
    private fun closeSearch() {
        // Cek dulu apakah search bar memang sedang terlihat
        if (edSearchActual.visibility == View.VISIBLE) {

            // 1. Kosongkan teks pencarian
            edSearchEdit.setText("")

            // 2. Sembunyikan keyboard
            try {
                requireContext().hideKeyBoard(edSearchEdit)
            } catch (e: Exception) {
                Log.e("TaskListFragment", "Gagal menyembunyikan keyboard", e)
            }

            // 3. Tukar kembali visibilitasnya
            edSearchActual.visibility = View.GONE
            chipContainer.visibility = View.VISIBLE
        }
    }
// ==========================================================
// TEMPEL FUNGSI BARU ANDA DI SINI
// ==========================================================
    /**
     * Mengamati event "edit checklist" dari ViewModel.
     * Dipicu oleh MainActivity saat item drawer diklik.
     */
    private fun observeChecklistToEdit() {
        taskViewModel.checklistToEdit.observe(viewLifecycleOwner) { checklist ->
            if (checklist != null) {

                // Buat Bundle manual
                val bundle = Bundle().apply {
                    // "checklist_obj" adalah kunci yang sama dengan di AddChecklistFragment
                    putParcelable("checklist_obj", checklist)
                }

                try {
                    // Navigasi menggunakan ID action dan Bundle
                    findNavController().navigate(
                        R.id.action_taskListFragment_to_editChecklistFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    Log.e("TaskListFragment", "Navigasi ke EditChecklistFragment gagal", e)
                    Toast.makeText(requireContext(), "Gagal membuka checklist", Toast.LENGTH_SHORT).show()
                }

                // Beri tahu ViewModel bahwa kita sudah menangani event ini
                taskViewModel.doneEditingChecklist()
            }
        }
    }
    private fun setupFirebaseAuth() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Anda harus login...", Toast.LENGTH_LONG).show()
            val intent = Intent(requireActivity(), LoginActivity2::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } else {
            currentUid = currentUser.uid
            Log.d("TaskListFragment", "User $currentUid terautentikasi. Memuat data...")
            taskViewModel.setFirebaseUser(currentUid!!)
            callGetTaskList()
            observeChecklistData()
            callSortByLiveData()
            statusCallback()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(requireActivity(), LoginActivity2::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setupChecklistRecyclerView() {
        checklistMainAdapter = ChecklistMainAdapter(
            isList = isListMutableLiveData,
            onEditClick = { checklist ->

                // ==========================================================
                // --- PERBAIKAN: Kirim seluruh objek, bukan hanya ID ---
                // ==========================================================
                val bundle = Bundle().apply {
                    // Gunakan 'putParcelable' (bukan putString)
                    // Gunakan kunci 'checklist_obj' (bukan checklist_id)
                    putParcelable("checklist_obj", checklist)
                }
                // ==========================================================

                // Aksi navigasi Anda sudah benar
                findNavController().navigate(
                    R.id.action_taskListFragment_to_editChecklistFragment,
                    bundle
                )
            },
            onDeleteClick = { checklist ->
                taskViewModel.deleteChecklist(checklist)
                restoreDeletedChecklist(checklist)
            }
        )
        checklistRV.adapter = checklistMainAdapter

        isListMutableLiveData.observe(viewLifecycleOwner) { isList ->
            if (isList) {
                checklistRV.layoutManager = LinearLayoutManager(requireContext())
            } else {
                checklistRV.layoutManager =
                    StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
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
                        val taskList = statusResult.data
                        taskRVVBListAdapter.submitList(taskList)

                        if (taskList?.isNotEmpty() == true) {
                            tvTaskHeader.visibility = View.VISIBLE
                        } else {
                            tvTaskHeader.visibility = View.GONE
                        }
                        // --- PANGGIL FUNGSI UNTUK UPDATE NAVIGATION DRAWER DI SINI ---
                        updateNavMenuWithTasks(taskList)
                        // -------------------------------------------------------------
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
        isListMutableLiveData.observe(viewLifecycleOwner) {
            if (it) {
                taskRV.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            } else {
                taskRV.layoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
            }
            if (::taskRVVBListAdapter.isInitialized) {
                taskRVVBListAdapter.notifyDataSetChanged()
            }
        }

        taskRVVBListAdapter =
            TaskRVVBListAdapter(isList = isListMutableLiveData) { type, position, task ->
                if (type == "delete") {
                    taskViewModel.deleteTaskUsingId(task.id)
                    restoreDeletedTask(task)
                } else if (type == "update") {
                    taskToUpdate = task

                    val dialogAddETTitle =
                        addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                    val dialogAddETDesc =
                        addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                    val ivTaskImage = addTaskDialog.findViewById<ImageView>(R.id.ivTaskImage)
                    val ivRemoveImage = addTaskDialog.findViewById<ImageView>(R.id.ivRemoveImage)

                    dialogAddETTitle.setText(task.title)
                    dialogAddETDesc.setText(task.description)

                    imageUri = null

                    if (task.imagePath.isNullOrEmpty()) {
                        ivTaskImage?.visibility = View.GONE
                        ivRemoveImage?.visibility = View.GONE
                    } else {
                        ivTaskImage?.visibility = View.VISIBLE
                        ivRemoveImage?.visibility = View.VISIBLE
                        ivTaskImage?.load(task.imagePath) {
                            crossfade(true)
                        }
                    }
                    addTaskDialog.show()
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
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
        val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }

        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START) else drawerLayout.openDrawer(GravityCompat.START)
        }


    }

    /**
     * FUNGSI BARU: Mengupdate item menu navigasi dengan judul-judul tugas yang ada.
     */
    private fun updateNavMenuWithTasks(taskList: List<Task>?) {
        val navMenu = navView.menu
        val dynamicTasksGroup = navMenu.findItem(R.id.group_dynamic_tasks)?.subMenu

        // Penting: Hapus semua item yang sudah ada di grup dinamis
        dynamicTasksGroup?.clear()

        // Tambahkan item menu baru untuk setiap tugas
        // Periksa apakah dynamicTasksGroup tidak null sebelum menambahkan item
        taskList?.forEach { task ->
            dynamicTasksGroup?.add(
                R.id.group_dynamic_tasks, // Grup ID
                View.generateViewId(),    // ID unik untuk MenuItem (Integer)
                0,                        // Order
                task.title                // Judul yang ditampilkan
            )?.setIcon(R.drawable.checkbox_icon_selector) // Ikon default untuk catatan
        }
    }


    private fun setupHeaderListeners() {
        // Ganti listener dari 'tvTelusuri' ke 'chipContainer'
        chipContainer.setOnClickListener {
            // Sembunyikan tombol "Telusuri"
            chipContainer.visibility = View.GONE
            // Tampilkan kotak pencarian
            edSearchActual.visibility = View.VISIBLE
            // Langsung fokus ke kotak pencarian
            edSearchEdit.requestFocus()

            // Tampilkan keyboard
            try {
                // GUNAKAN requireContext() DI SINI
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edSearchEdit, InputMethodManager.SHOW_IMPLICIT)
            } catch (e: Exception) {
                Log.e("TaskListFragment", "Gagal menampilkan keyboard", e)
            }
        }

        profileCard.setOnClickListener { findNavController().navigate(R.id.profileFragment) }

        iconFilter.setOnClickListener {
            showSortBottomSheet()
        }

        iconSort.setOnClickListener {
            val currentIsList = isListMutableLiveData.value ?: true
            isListMutableLiveData.postValue(!currentIsList)

            if (!currentIsList) {
                iconSort.setImageResource(R.drawable.list)
            } else {
                iconSort.setImageResource(R.drawable.grid)
            }
        }
    }

    private fun setupDialogs() {
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        val dialogAddETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
        val dialogAddETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val dialogAddETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
        val dialogAddETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

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
            override fun afterTextChanged(s: Editable) {
                validateEditText(dialogAddETTitle, dialogAddETTitleL)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        dialogAddETDesc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                validateEditText(dialogAddETDesc, dialogAddETDescL)
            }

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
            handleSaveTask()
        }
    }

    private fun handleSaveTask() {
        val dialogAddETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
        val dialogAddETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val dialogAddETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
        val dialogAddETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        if (!validateEditText(dialogAddETTitle, dialogAddETTitleL) || !validateEditText(
                dialogAddETDesc,
                dialogAddETDescL
            )
        ) {
            Toast.makeText(
                requireContext(),
                "Judul dan Deskripsi tidak boleh kosong",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val title = dialogAddETTitle.text.toString().trim()
        val description = dialogAddETDesc.text.toString().trim()

        if (imageUri != null) {
            loadingDialog.show()

            MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        loadingDialog.dismiss()
                        val imageUrl = resultData?.get("secure_url") as? String
                        saveTaskToDatabase(title, description, imageUrl)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "Upload gambar gagal: ${error?.description}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                })
                .dispatch()

        } else {
            saveTaskToDatabase(title, description, null)
        }
    }

    private fun saveTaskToDatabase(title: String, description: String, imageUrl: String?) {
        if (taskToUpdate == null) {
            val newTask = Task(
                UUID.randomUUID().toString(),
                title,
                description,
                Date(),
                imageUrl
            )
            taskViewModel.insertTask(newTask)
            Toast.makeText(requireContext(), "Catatan disimpan!", Toast.LENGTH_SHORT).show()

        } else {
            val updatedTask = taskToUpdate!!.copy(
                title = title,
                description = description,
                imagePath = imageUrl
            )
            taskViewModel.updateTask(updatedTask)
            Toast.makeText(requireContext(), "Catatan diperbarui!", Toast.LENGTH_SHORT).show()
        }

        imageUri = null
        taskToUpdate = null

        try {
            view?.let { requireContext().hideKeyBoard(it) }
        } catch (e: Exception) {
            Log.e("TaskListFragment", "Gagal hide keyboard", e)
        }

        addTaskDialog.dismiss()
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
                    checklistObserver =
                        taskViewModel.searchChecklist(queryString, isAsc, sortByName)
                    observeChecklistDataAfterSearch()
                } else {
                    callSortByLiveData()
                    observeChecklistData()
                }
            }
        })
        edSearchEdit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                // 1. Sembunyikan keyboard
                try {
                    requireContext().hideKeyBoard(v)
                } catch (e: Exception) {
                    Log.e("TaskListFragment", "Gagal menyembunyikan keyboard", e)
                }

                // 2. Sembunyikan kotak pencarian
                edSearchActual.visibility = View.GONE
                // 3. Tampilkan kembali tombol "Telusuri"
                chipContainer.visibility = View.VISIBLE

                true
            } else {
                false
            }
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
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            newPhotoFile
        )
        takePictureLauncher.launch(imageUri)
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "list" -> {
                // Navigasi ke EditChecklistFragment untuk membuat checklist baru
                findNavController().navigate(R.id.action_taskListFragment_to_editChecklistFragment)
            }

            "catatan" -> {
                // Logika untuk menambahkan atau mengedit catatan
                // Pastikan dialog disiapkan untuk mode "tambah baru"
                val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)

                taskToUpdate = null // Penting: reset untuk mode tambah baru

                clearEditText(addETTitle, addTaskDialog.findViewById(R.id.edTaskTitleL))
                clearEditText(addETDesc, addTaskDialog.findViewById(R.id.edTaskDescL))

                // Sembunyikan dan bersihkan gambar jika ada dari mode edit sebelumnya
                imageUri = null
                ivTaskImage?.setImageURI(null)
                ivTaskImage?.visibility = View.GONE
                ivRemoveImage?.visibility = View.GONE

                addTaskDialog.show()
            }

            "gambar" -> {
                // Logika untuk menambahkan catatan dengan gambar
                // Mirip dengan "catatan" tetapi langsung memicu pemilih gambar
                val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.addETTitle)
                val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.addETDesc)
                val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
                val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

                taskToUpdate = null // Penting: reset untuk mode tambah baru

                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)

                // Pastikan gambar direset jika ada
                imageUri = null
                ivTaskImage?.setImageURI(null)
                ivTaskImage?.visibility = View.GONE
                ivRemoveImage?.visibility = View.GONE

                // Tampilkan dialog tambah tugas
                addTaskDialog.show()

                // Langsung tampilkan bottom sheet untuk memilih sumber gambar
                val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
                imageSourceBottomSheet.listener = this
                imageSourceBottomSheet.show(
                    parentFragmentManager,
                    AddOptionsBottomSheetFragment.TAG
                )
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