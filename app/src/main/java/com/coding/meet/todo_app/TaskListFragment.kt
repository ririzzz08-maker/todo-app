package com.coding.meet.todo_app

import android.app.Dialog
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.coding.meet.todo_app.adapters.TaskRVVBListAdapter
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
import java.util.Date
import java.util.UUID
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File


// --- IMPLEMENTASI KEDUA "JEMBATAN" ---
class TaskListFragment : Fragment(), AddOptionsBottomSheetFragment.AddOptionsListener, ImageSourceBottomSheetFragment.ImageSourceListener {

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
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply {
        postValue(true)
    }
    // ... (di dalam class TaskListFragment) ...

    // Variabel untuk menyimpan URI gambar yang diambil/dipilih
    private var imageUri: Uri? = null

    // Launcher untuk meminta izin kamera
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startCamera() // Jika izin diberikan, buka kamera
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk mengambil gambar dari kamera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            // Jika gambar berhasil diambil, tampilkan di ImageView
            imageUri?.let { uri ->
                ivTaskImage.setImageURI(uri)
                ivTaskImage.visibility = View.VISIBLE
                ivRemoveImage.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(requireContext(), "Gagal mengambil gambar.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Jika gambar berhasil dipilih, tampilkan di ImageView
            imageUri = it // Simpan URI ke variabel imageUri kita
            ivTaskImage.setImageURI(it)
            ivTaskImage.visibility = View.VISIBLE
            ivRemoveImage.visibility = View.VISIBLE
        } ?: run {
            Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih.", Toast.LENGTH_SHORT).show()
        }
    }

// ... (lanjutkan dengan kode yang sudah ada) ...

    // --- Variabel Global untuk Navigasi ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // --- Variabel Global untuk View di Dialog Update ---
    private lateinit var updateETTitle: TextInputEditText
    private lateinit var updateETTitleL: TextInputLayout
    private lateinit var updateETDesc: TextInputEditText
    private lateinit var updateETDescL: TextInputLayout
    private lateinit var updateTaskBtn: Button

    // --- Variabel Global untuk View di Dialog Add ---
    private lateinit var addETTitle: TextInputEditText
    private lateinit var addETTitleL: TextInputLayout
    private lateinit var addETDesc: TextInputEditText
    private lateinit var addETDescL: TextInputLayout
    private lateinit var btnPickImage: ImageView

    private lateinit var ivTaskImage: ImageView

    // --- Variabel Global untuk Views (Pengganti Binding) ---
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

        // --- 2. Panggil semua fungsi setup ---
        setupDialogs()
        setupRecyclerView()
        setupNavigationDrawer()
        setupHeaderListeners()
        setupSearch()

        // --- 3. Panggil fungsi data ---
        callGetTaskList()
        callSortByLiveData()
        statusCallback()
    }

    // =================================================================
    // SEMUA FUNGSI PRIVATE
    // =================================================================

    private fun setupNavigationDrawer() {
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
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
        chipContainer.setOnClickListener {
            Toast.makeText(requireContext(), "Chip Container Diklik", Toast.LENGTH_SHORT).show()
        }
        profileCard.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        iconFilter.setOnClickListener {
            showSortBottomSheet()
        }
        iconSort.setOnClickListener {
            Toast.makeText(requireContext(), "Mengubah Urutan Sortir", Toast.LENGTH_SHORT).show()
        }
        tvTelusuri.setOnClickListener {
            edSearchActual.visibility = View.VISIBLE
            edSearchEdit.requestFocus()
            Toast.makeText(requireContext(), "Input Pencarian Aktif", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDialogs() {
        // --- Add task dialog setup ---
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }
        addETTitle = addTaskDialog.findViewById(R.id.addETTitle)
        addETTitleL = addTaskDialog.findViewById(R.id.edTaskTitleL)
        ivRemoveImage = addTaskDialog.findViewById(R.id.ivRemoveImage)

        ivRemoveImage.setOnClickListener {
            imageUri = null // Hapus URI gambar
            ivTaskImage.setImageURI(null) // Hapus gambar dari ImageView
            ivTaskImage.visibility = View.GONE // Sembunyikan ImageView gambar
            ivRemoveImage.visibility = View.GONE // Sembunyikan tombol hapus itu sendiri
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }

        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) { validateEditText(addETTitle, addETTitleL) }
        })
        addETDesc = addTaskDialog.findViewById(R.id.addETDesc)
        addETDescL = addTaskDialog.findViewById(R.id.edTaskDescL)
        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) { validateEditText(addETDesc, addETDescL) }
        })

        // Listener untuk tombol 'Tambah' FAB
        addTaskFABtn.setOnClickListener {
            val bottomSheet = AddOptionsBottomSheetFragment()
            bottomSheet.listener = this
            bottomSheet.show(parentFragmentManager, AddOptionsBottomSheetFragment.TAG)
        }

        // Listener untuk tombol 'Pilih Gambar' di dalam dialog
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
                val newTask = Task(UUID.randomUUID().toString(), addETTitle.text.toString().trim(), addETDesc.text.toString().trim(), Date())
                requireContext().hideKeyBoard(it)
                addTaskDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }

        // --- Update Task dialog setup ---
        updateETTitle = updateTaskDialog.findViewById(R.id.edTaskTitle)
        updateETTitleL = updateTaskDialog.findViewById(R.id.edTaskTitleL)
        updateETDesc = updateTaskDialog.findViewById(R.id.edTaskDesc)
        updateETDescL = updateTaskDialog.findViewById(R.id.edTaskDescL)
        updateTaskBtn = updateTaskDialog.findViewById(R.id.updateTaskBtn)
        // ... (Tambahkan TextChangedListeners untuk dialog update jika perlu) ...
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }
    }

    private fun setupRecyclerView() {
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
                nestedScrollView.smoothScrollTo(0, positionStart)
            }
        })
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
                Toast.makeText(requireContext(), "Urutan: Kustom (Judul A-Z)", Toast.LENGTH_SHORT).show()
            }
            "created_date" -> {
                taskViewModel.setSortBy(Pair("date", true))
                Toast.makeText(requireContext(), "Urutan: Tanggal Dibuat", Toast.LENGTH_SHORT).show()
            }
            "modified_date" -> {
                taskViewModel.setSortBy(Pair("date", false))
                Toast.makeText(requireContext(), "Urutan: Tanggal Diubah", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreDeletedTask(deletedTask: Task) {
        Snackbar.make(requireView(), "Deleted '${deletedTask.title}'", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                taskViewModel.insertTask(deletedTask)
            }.show()
    }

    private fun setupSearch() {
        edSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(query: Editable) {
                if (query.toString().isNotEmpty()) {
                    taskViewModel.searchTaskList(query.toString())
                } else {
                    callSortByLiveData()
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
                    when (statusResponse.data as StatusResult) {
                        StatusResult.Added -> Log.d("StatusResult", "Added")
                        StatusResult.Deleted -> Log.d("StatusResult", "Deleted")
                        StatusResult.Updated -> Log.d("StatusResult", "Updated")
                    }
                    statusResponse.message?.let { message -> requireContext().longToastShow(message) }
                }
                Status.ERROR -> {
                    loadingDialog.dismiss()
                    statusResponse.message?.let { message -> requireContext().longToastShow(message) }
                }
            }
        }
    }

    private fun callGetTaskList() {
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel.taskStateFlow.collectLatest { statusResult ->
                Log.d("status", statusResult.status.toString())
                when (statusResult.status) {
                    Status.LOADING -> loadingDialog.show()
                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        statusResult.data?.collect { taskList ->
                            taskRVVBListAdapter.submitList(taskList)
                        }
                    }
                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        statusResult.message?.let { it1 -> requireContext().longToastShow(it1) }
                    }
                }
            }
        }
    }
    private fun startCamera() {
        // Buat direktori untuk menyimpan foto di cache aplikasi
        val photosDir = File(requireContext().externalCacheDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs() // Buat jika belum ada

        // Buat file unik untuk foto yang akan diambil
        val newPhotoFile = File(photosDir, "${System.currentTimeMillis()}.jpg")

        // Dapatkan URI untuk file menggunakan FileProvider
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider", // AUTHORITIES
            newPhotoFile
        )

        // Luncurkan aplikasi kamera
        takePictureLauncher.launch(imageUri)
    }
    // --- FUNGSI DARI JEMBATAN (LISTENER) ---

    // Dari AddOptionsBottomSheetFragment
    override fun onOptionSelected(option: String) {
        when (option) {
            "list" -> {
                findNavController().navigate(R.id.addChecklistFragment)
            }
            "catatan" -> {
                addETTitleL.hint = "Judul Catatan"
                addETDescL.hint = "Masukkan isi catatan"
                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)
                addTaskDialog.show()
            }
            "gambar" -> {
                // Saat "Gambar" dari menu utama diklik, kita lakukan hal yang sama
                // seperti "Catatan", tapi kita langsung buka pilihan gambar
                addETTitleL.hint = "Judul Catatan"
                addETDescL.hint = "Masukkan isi catatan"
                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)
                addTaskDialog.show()

                // Langsung panggil bottom sheet gambar
                val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
                imageSourceBottomSheet.listener = this
                imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
            }
        }
    }

    // Dari ImageSourceBottomSheetFragment
    override fun onSourceSelected(source: String) {
        when (source) {
            ImageSourceBottomSheetFragment.SOURCE_CAMERA -> {
                // Minta izin kamera sebelum membuka kamera
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            ImageSourceBottomSheetFragment.SOURCE_GALLERY -> {
                // Langsung buka galeri untuk memilih gambar
                pickImageLauncher.launch("image/*") // "image/*" artinya ingin semua jenis gambar
            }
        }
    }

}