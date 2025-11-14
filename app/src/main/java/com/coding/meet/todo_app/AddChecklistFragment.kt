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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.adapters.CheckedItemAdapter
import com.coding.meet.todo_app.adapters.ChecklistItem
import com.coding.meet.todo_app.adapters.ChecklistItemAdapter
import com.coding.meet.todo_app.adapters.ChecklistItemListener
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- IMPLEMENTASI KEDUA "JEMBATAN" ---
class AddChecklistFragment : Fragment(), ChecklistItemListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    // --- Daftar (List) Data ---
    private val activeItems = mutableListOf<ChecklistItem>()
    private val checkedItems = mutableListOf<ChecklistItem>()

    // --- Adapters ---
    private lateinit var activeAdapter: ChecklistItemAdapter
    private lateinit var checkedAdapter: CheckedItemAdapter

    // --- Views (Dari kode Anda) ---
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etChecklistTitle: EditText
    private lateinit var rvActiveItems: RecyclerView
    private lateinit var rvCheckedItems: RecyclerView
    private lateinit var btnAddItem: LinearLayout
    private lateinit var divider: View
    private lateinit var tvCheckedHeader: TextView

    // --- VIEWS & VARIABEL BARU UNTUK GAMBAR ---
    private lateinit var btnSaveChecklist: ImageView
    private lateinit var btnPickChecklistImage: ImageView
    private lateinit var frameLayoutChecklistImageContainer: FrameLayout
    private lateinit var ivChecklistImage: ImageView
    private lateinit var ivDeleteChecklistImage: ImageView

    private var checklistImageUri: Uri? = null
    private var currentChecklistPhotoPath: String? = null

    // --- LAUNCHER BARU UNTUK GAMBAR ---
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startCameraForChecklist()
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
        }
    }

    private val takeChecklistPictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            checklistImageUri?.let { uri ->
                ivChecklistImage.setImageURI(uri)
                frameLayoutChecklistImageContainer.visibility = View.VISIBLE
                ivDeleteChecklistImage.visibility = View.VISIBLE // Tampilkan tombol hapus
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
            ivDeleteChecklistImage.visibility = View.VISIBLE // Tampilkan tombol hapus
        } ?: run {
            Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih.", Toast.LENGTH_SHORT).show()
            frameLayoutChecklistImageContainer.visibility = View.GONE
            checklistImageUri = null
        }
    }
    // --- AKHIR BLOK BARU ---


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_checklist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Hubungkan Views (LAMA) ---
        toolbar = view.findViewById(R.id.checklist_toolbar)
        rvActiveItems = view.findViewById(R.id.rvChecklistItems)
        rvCheckedItems = view.findViewById(R.id.rvCheckedItems)
        btnAddItem = view.findViewById(R.id.btn_add_list_item)
        divider = view.findViewById(R.id.checklist_divider)
        tvCheckedHeader = view.findViewById(R.id.tvCheckedItemsHeader)
        etChecklistTitle = view.findViewById(R.id.etChecklistTitle)

        // --- HUBUNGKAN VIEWS (BARU) ---
        // Tombol-tombol ini ada di dalam toolbar
        btnSaveChecklist = toolbar.findViewById(R.id.btnSaveChecklist)
        btnPickChecklistImage = toolbar.findViewById(R.id.btnPickChecklistImage)
        // View ini ada di layout utama
        frameLayoutChecklistImageContainer = view.findViewById(R.id.frameLayoutChecklistImageContainer)
        ivChecklistImage = view.findViewById(R.id.ivChecklistImage)
        ivDeleteChecklistImage = view.findViewById(R.id.ivDeleteChecklistImage)
        // --- AKHIR HUBUNGKAN VIEWS BARU ---


        // --- Inisialisasi Data Awal (LAMA) ---
        if (activeItems.isEmpty() && checkedItems.isEmpty()) {
            activeItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
        }

        // --- Setup 2 Adapter dan 2 RecyclerView (LAMA) ---
        activeAdapter = ChecklistItemAdapter(activeItems, this)
        rvActiveItems.adapter = activeAdapter

        checkedAdapter = CheckedItemAdapter(checkedItems, this)
        rvCheckedItems.adapter = checkedAdapter

        // --- Setup Listeners (LAMA + BARU) ---
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        btnAddItem.setOnClickListener {
            addNewItem()
        }

        // --- LISTENER BARU ---
        btnSaveChecklist.setOnClickListener {
            saveChecklist()
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
            ivDeleteChecklistImage.visibility = View.GONE // Sembunyikan tombol hapus juga
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }
        // --- AKHIR LISTENER BARU ---

        updateCheckedHeaderVisibility()
    }

    // --- FUNGSI SIMPAN BARU ---
    private fun saveChecklist() {
        val title = etChecklistTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Di sini Anda akan mengumpulkan semua data
        // val imageToSave = checklistImageUri?.toString()
        // val activeItemsToSave = activeItems
        // val checkedItemsToSave = checkedItems

        // Simpan ke database/ViewModel...

        Toast.makeText(requireContext(), "Checklist Disimpan!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    // --- FUNGSI LAMA (TETAP SAMA) ---
    private fun addNewItem() {
        val newItem = ChecklistItem(UUID.randomUUID().toString(), "", false)
        activeItems.add(newItem)
        activeAdapter.notifyItemInserted(activeItems.size - 1)
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

    // --- FUNGSI LAMA DARI INTERFACE (TETAP SAMA) ---
    override fun onCheckChanged(position: Int, isChecked: Boolean) {
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
    }

    override fun onDeleteClicked(position: Int, isFromActiveList: Boolean) {
        if (isFromActiveList) {
            activeItems.removeAt(position)
            activeAdapter.notifyItemRemoved(position)
        } else {
            checkedItems.removeAt(position)
            checkedAdapter.notifyItemRemoved(position)
            updateCheckedHeaderVisibility()
        }
    }

    // --- FUNGSI BARU UNTUK KAMERA & GALERI ---
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
                "${requireContext().packageName}.provider", // Pastikan ini sama dengan di AndroidManifest
                it
            )
            takeChecklistPictureLauncher.launch(checklistImageUri)
        }
    }

    @Throws(IOException::class)
    private fun createChecklistImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Buat subdirektori 'photos' di dalam externalCacheDir
        val storageDir: File? = File(requireContext().externalCacheDir, "photos").apply {
            if (!exists()) {
                mkdirs() // Pastikan direktori ini ada
            }
        }

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentChecklistPhotoPath = absolutePath
        }
    }

    // --- FUNGSI BARU DARI INTERFACE ImageSourceListener ---
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