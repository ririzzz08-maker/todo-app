package com.coding.meet.todo_app

// --- IMPORT YANG DIPERLUKAN ---
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri // BARU: Untuk konversi String ke Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs // BARU: Untuk menerima argumen
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.adapters.CheckedItemAdapter
import com.coding.meet.todo_app.models.ChecklistItem // Pastikan ini dari 'models'
import com.coding.meet.todo_app.adapters.ChecklistItemAdapter
import com.coding.meet.todo_app.adapters.ChecklistItemListener
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.util.Log

class AddChecklistFragment : Fragment(), ChecklistItemListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    // BARU: Dapatkan instance ViewModel
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    // BARU: Menerima argumen dari Navigasi
    private val args: AddChecklistFragmentArgs by navArgs()

    // BARU: Variabel untuk menyimpan data yang sedang diedit
    private var editingChecklist: Checklist? = null

    // --- Daftar (List) Data ---
    // BARU: Ubah jadi var agar bisa di-replace saat edit
    private var activeItems = mutableListOf<ChecklistItem>()
    private var checkedItems = mutableListOf<ChecklistItem>()

    // --- Adapters ---
    private lateinit var activeAdapter: ChecklistItemAdapter
    private lateinit var checkedAdapter: CheckedItemAdapter

    // --- Views ---
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etChecklistTitle: EditText
    private lateinit var rvActiveItems: RecyclerView
    private lateinit var rvCheckedItems: RecyclerView
    private lateinit var btnAddItem: LinearLayout
    private lateinit var divider: View
    private lateinit var tvCheckedHeader: TextView
    private lateinit var btnSaveChecklist: ImageView
    private lateinit var btnPickChecklistImage: ImageView
    private lateinit var frameLayoutChecklistImageContainer: FrameLayout
    private lateinit var ivChecklistImage: ImageView
    private lateinit var ivDeleteChecklistImage: ImageView

    private var checklistImageUri: Uri? = null
    private var currentChecklistPhotoPath: String? = null

    // --- LAUNCHER GAMBAR (Tetap Sama) ---
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) startCameraForChecklist() else Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
    }
    private val takeChecklistPictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            checklistImageUri?.let { uri ->
                ivChecklistImage.setImageURI(uri)
                frameLayoutChecklistImageContainer.visibility = View.VISIBLE
                ivDeleteChecklistImage.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(requireContext(), "Gagal mengambil gambar.", Toast.LENGTH_SHORT).show()
            frameLayoutChecklistImageContainer.visibility = View.GONE
            checklistImageUri = null
        }
    }
    private val pickChecklistImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            checklistImageUri = it
            ivChecklistImage.setImageURI(it)
            frameLayoutChecklistImageContainer.visibility = View.VISIBLE
            ivDeleteChecklistImage.visibility = View.VISIBLE
        } ?: run {
            Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih.", Toast.LENGTH_SHORT).show()
            frameLayoutChecklistImageContainer.visibility = View.GONE
            checklistImageUri = null
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_checklist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Hubungkan Views ---
        toolbar = view.findViewById(R.id.checklist_toolbar)
        rvActiveItems = view.findViewById(R.id.rvChecklistItems)
        rvCheckedItems = view.findViewById(R.id.rvCheckedItems)
        btnAddItem = view.findViewById(R.id.btn_add_list_item)
        divider = view.findViewById(R.id.checklist_divider)
        tvCheckedHeader = view.findViewById(R.id.tvCheckedItemsHeader)
        etChecklistTitle = view.findViewById(R.id.etChecklistTitle)
        btnSaveChecklist = toolbar.findViewById(R.id.btnSaveChecklist)
        btnPickChecklistImage = toolbar.findViewById(R.id.btnPickChecklistImage)
        frameLayoutChecklistImageContainer = view.findViewById(R.id.frameLayoutChecklistImageContainer)
        ivChecklistImage = view.findViewById(R.id.ivChecklistImage)
        ivDeleteChecklistImage = view.findViewById(R.id.ivDeleteChecklistImage)

        // --- Setup 2 Adapter dan 2 RecyclerView ---
        // (Pindahkan setup adapter ke sini agar siap sebelum data dimuat)
        activeAdapter = ChecklistItemAdapter(activeItems, this)
        rvActiveItems.adapter = activeAdapter

        checkedAdapter = CheckedItemAdapter(checkedItems, this)
        rvCheckedItems.adapter = checkedAdapter

        // --- BARU: Logika Mode Edit vs Mode Buat Baru ---
        val checklistId = args.checklistId
        if (checklistId == null) {
            // MODE BUAT BARU
            // Tambahkan 1 item kosong sebagai permulaan
            activeItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
            activeAdapter.notifyItemInserted(0)
        } else {
            // MODE EDIT
            // Ambil data dari database
            loadChecklistData(checklistId)
        }

        // --- Setup Listeners ---
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        btnAddItem.setOnClickListener {
            addNewItem()
        }
        btnSaveChecklist.setOnClickListener {
            saveChecklist() // Fungsi ini sekarang "pintar"
        }
        btnPickChecklistImage.setOnClickListener {
            val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
            imageSourceBottomSheet.listener = this
            imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
        }
        ivDeleteChecklistImage.setOnClickListener {
            checklistImageUri = null
            ivChecklistImage.setImageURI(null)
            frameLayoutChecklistImageContainer.visibility = View.GONE
            ivDeleteChecklistImage.visibility = View.GONE
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }

        updateCheckedHeaderVisibility()
    }

    // --- BARU: FUNGSI UNTUK MEMUAT DATA SAAT MODE EDIT ---
    private fun loadChecklistData(checklistId: String) {
        taskViewModel.getChecklistById(checklistId).observe(viewLifecycleOwner) { checklist ->
            if (checklist != null) {
                // Simpan data checklist yang sedang diedit
                editingChecklist = checklist

                // Isi UI dengan data yang ada
                etChecklistTitle.setText(checklist.title)

                // Muat gambar jika ada
                checklist.imagePath?.let {
                    checklistImageUri = it.toUri() // Konversi String ke Uri
                    ivChecklistImage.setImageURI(checklistImageUri)
                    frameLayoutChecklistImageContainer.visibility = View.VISIBLE
                    ivDeleteChecklistImage.visibility = View.VISIBLE
                }

                // Pisahkan item yang aktif dan yang sudah dicentang
                val (checked, active) = checklist.items.partition { it.isChecked }

                // Perbarui daftar dan adapter
                activeItems.clear()
                activeItems.addAll(active)
                activeAdapter.notifyDataSetChanged() // Muat semua data baru

                checkedItems.clear()
                checkedItems.addAll(checked)
                checkedAdapter.notifyDataSetChanged() // Muat semua data baru

                updateCheckedHeaderVisibility()
            }
        }
    }

    // --- FUNGSI SIMPAN (VERSI BARU TANPA VALIDASI ITEM) ---
    private fun saveChecklist() {
        val title = etChecklistTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Bersihkan item yang teksnya kosong
        val finalActiveItems = activeItems.filter { it.text.isNotBlank() }
        val finalCheckedItems = checkedItems.filter { it.text.isNotBlank() }
        val allItems = finalActiveItems + finalCheckedItems

        //
        // !!!!! BLOK VALIDASI ITEM SUDAH DIHAPUS DARI SINI !!!!!
        //

        // Cek apakah kita sedang 'Edit' atau 'Buat Baru'
        if (editingChecklist == null) {
            // MODE BUAT BARU
            val newChecklist = Checklist(
                id = UUID.randomUUID().toString(),
                title = title,
                createdDate = Date(),
                imagePath = checklistImageUri?.toString(),
                items = allItems // Sekarang boleh kosong
            )
            taskViewModel.insertChecklist(newChecklist)
            Toast.makeText(requireContext(), "Checklist Disimpan!", Toast.LENGTH_SHORT).show()

        } else {
            // MODE EDIT
            val updatedChecklist = editingChecklist!!.copy(
                title = title,
                imagePath = checklistImageUri?.toString(),
                items = allItems, // Sekarang boleh kosong
                createdDate = editingChecklist!!.createdDate
            )
            taskViewModel.updateChecklist(updatedChecklist)
            Toast.makeText(requireContext(), "Checklist Diperbarui!", Toast.LENGTH_SHORT).show()
        }

        findNavController().popBackStack() // Kembali ke TaskListFragment
    }

    // --- FUNGSI LAMA (TETAP SAMA) ---
    private fun addNewItem() {
        val newItem = ChecklistItem(UUID.randomUUID().toString(), "", false)
        activeItems.add(newItem)
        activeAdapter.notifyItemInserted(activeItems.size - 1)
        // Auto-focus ke item baru (opsional tapi bagus)
        rvActiveItems.smoothScrollToPosition(activeItems.size - 1)
    }

    private fun updateCheckedHeaderVisibility() {
        if (checkedItems.isEmpty()) {
            divider.visibility = View.GONE
            tvCheckedHeader.visibility = View.GONE
        } else {
            divider.visibility = View.VISIBLE
            tvCheckedHeader.visibility = View.VISIBLE
        }
    }

    override fun onCheckChanged(position: Int, isChecked: Boolean) {
        try {
            if (isChecked) {
                val item = activeItems.removeAt(position)
                item.isChecked = true
                activeAdapter.notifyItemRemoved(position)
                checkedItems.add(item)
                checkedAdapter.notifyItemInserted(checkedItems.size - 1)
            } else {
                val item = checkedItems.removeAt(position)
                item.isChecked = false
                checkedAdapter.notifyItemRemoved(position)
                activeItems.add(item)
                activeAdapter.notifyItemInserted(activeItems.size - 1)
            }
            updateCheckedHeaderVisibility()
        } catch (e: IndexOutOfBoundsException) {
            // Terkadang terjadi jika klik terlalu cepat, abaikan saja
            Log.w("AddChecklistFragment", "onCheckChanged error: ${e.message}")
        }
    }

    override fun onDeleteClicked(position: Int, isFromActiveList: Boolean) {
        try {
            if (isFromActiveList) {
                activeItems.removeAt(position)
                activeAdapter.notifyItemRemoved(position)
            } else {
                checkedItems.removeAt(position)
                checkedAdapter.notifyItemRemoved(position)
                updateCheckedHeaderVisibility()
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.w("AddChecklistFragment", "onDeleteClicked error: ${e.message}")
        }
    }

    // --- FUNGSI KAMERA & GALERI (Tetap Sama) ---
    private fun startCameraForChecklist() {
        val photoFile: File? = try {
            createChecklistImageFile()
        } catch (ignored: IOException) {
            Toast.makeText(requireContext(), "Error saat membuat file gambar.", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            checklistImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                it
            )
            takeChecklistPictureLauncher.launch(checklistImageUri)
        }
    }

    @Throws(IOException::class)
    private fun createChecklistImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = File(requireContext().externalCacheDir, "photos").apply {
            if (!exists()) mkdirs()
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentChecklistPhotoPath = absolutePath
        }
    }

    override fun onSourceSelected(source: String) {
        when (source) {
            ImageSourceBottomSheetFragment.SOURCE_CAMERA -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            ImageSourceBottomSheetFragment.SOURCE_GALLERY -> {
                pickChecklistImageLauncher.launch("image/*")
            }
        }
    }
}