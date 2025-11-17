package com.coding.meet.todo_app

// --- IMPORT YANG DIPERLUKAN ---
import android.Manifest
import android.app.Dialog // <-- IMPORT BARU
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
// HAPUS: import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.adapters.CheckedItemAdapter
import com.coding.meet.todo_app.models.ChecklistItem
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
// --- IMPORT BARU ---
import coil.load // Untuk memuat gambar dari URL
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.coding.meet.todo_app.utils.setupDialog // Helper dialog

class AddChecklistFragment : Fragment(), ChecklistItemListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    // --- DIALOG LOADING (BARU) ---
    private val loadingDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }
    private val args: AddChecklistFragmentArgs by navArgs()
    private var editingChecklist: Checklist? = null

    private var activeItems = mutableListOf<ChecklistItem>()
    private var checkedItems = mutableListOf<ChecklistItem>()

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

    // --- Variabel Gambar ---
    // 'checklistImageUri' HANYA untuk gambar LOKAL BARU yang dipilih
    private var checklistImageUri: Uri? = null
    private var currentChecklistPhotoPath: String? = null // Untuk kamera

    // --- LAUNCHER GAMBAR (Tetap Sama) ---
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) startCameraForChecklist() else Toast.makeText(requireContext(), "Izin kamera ditolak.", Toast.LENGTH_SHORT).show()
    }
    private val takeChecklistPictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            checklistImageUri?.let { uri ->
                ivChecklistImage.setImageURI(uri) // Tampilkan gambar lokal baru
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
            checklistImageUri = it // Simpan URI lokal baru
            ivChecklistImage.setImageURI(it) // Tampilkan gambar lokal baru
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

        // --- Setup Adapter ---
        activeAdapter = ChecklistItemAdapter(activeItems, this)
        rvActiveItems.adapter = activeAdapter
        checkedAdapter = CheckedItemAdapter(checkedItems, this)
        rvCheckedItems.adapter = checkedAdapter

        // --- Logika Mode Edit vs Mode Buat Baru ---
        val checklistId = args.checklistId
        if (checklistId == null) {
            // MODE BUAT BARU
            activeItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
            activeAdapter.notifyItemInserted(0)
        } else {
            // MODE EDIT
            loadChecklistData(checklistId) // <-- Fungsi ini dimodifikasi
        }

        // --- Setup Listeners ---
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        btnAddItem.setOnClickListener {
            addNewItem()
        }
        btnSaveChecklist.setOnClickListener {
            saveChecklistClicked() // <-- Fungsi ini dimodifikasi
        }
        btnPickChecklistImage.setOnClickListener {
            val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
            imageSourceBottomSheet.listener = this
            imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
        }
        // --- LOGIKA HAPUS GAMBAR (DIMODIFIKASI) ---
        ivDeleteChecklistImage.setOnClickListener {
            checklistImageUri = null // Hapus URI lokal baru (jika ada)
            editingChecklist?.imagePath = null // Hapus URL online lama (jIKA mode edit)

            ivChecklistImage.setImageURI(null) // Bersihkan tampilan
            frameLayoutChecklistImageContainer.visibility = View.GONE
            ivDeleteChecklistImage.visibility = View.GONE
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }

        updateCheckedHeaderVisibility()
    }

    // --- FUNGSI LOAD DATA (DIMODIFIKASI) ---
    private fun loadChecklistData(checklistId: String) {
        taskViewModel.getChecklistById(checklistId).observe(viewLifecycleOwner) { checklist ->
            if (checklist != null) {
                editingChecklist = checklist
                etChecklistTitle.setText(checklist.title)

                // --- LOGIKA MEMUAT GAMBAR (DIUBAH) ---
                checklist.imagePath?.let { imageUrl ->
                    // 'imageUrl' adalah URL 'https://...'
                    // Kita TIDAK set 'checklistImageUri' di sini

                    // Gunakan Coil untuk memuat URL online ke ImageView
                    ivChecklistImage.load(imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_add_circle) // Ganti dgn placeholder Anda
                        error(R.drawable.ic_add_circle) // Ganti dgn gambar error Anda
                    }
                    frameLayoutChecklistImageContainer.visibility = View.VISIBLE
                    ivDeleteChecklistImage.visibility = View.VISIBLE
                }

                // (Sisa kode Anda sudah benar)
                val (checked, active) = checklist.items.partition { it.isChecked }
                activeItems.clear()
                activeItems.addAll(active)
                activeAdapter.notifyDataSetChanged()
                checkedItems.clear()
                checkedItems.addAll(checked)
                checkedAdapter.notifyDataSetChanged()
                updateCheckedHeaderVisibility()
            }
        }
    }

    // --- FUNGSI SIMPAN (DIMODIFIKASI TOTAL) ---
    private fun saveChecklistClicked() {
        updateActiveItemsFromViews()
        val title = etChecklistTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val finalActiveItems = activeItems.filter { it.text.isNotBlank() }
        val finalCheckedItems = checkedItems.filter { it.text.isNotBlank() }
        val allItems = finalActiveItems + finalCheckedItems

        // --- LOGIKA BARU UNTUK UPLOAD ---

        // Cek 1: Apakah user baru saja memilih gambar baru?
        if (checklistImageUri != null) {
            // Ya, upload gambar baru
            uploadImageAndSave(title, allItems)
        } else {
            // Tidak, user tidak memilih gambar baru.
            // Cek 2: Apakah kita dalam mode edit?
            val existingImageUrl = if (editingChecklist != null) {
                // Ya, ambil URL lama (bisa null jika dihapus)
                editingChecklist!!.imagePath
            } else {
                // Tidak, ini item baru tanpa gambar
                null
            }
            // Langsung simpan data dengan URL yang ada
            saveDataToFirebase(title, allItems, existingImageUrl)
        }
    }

    // --- FUNGSI BARU 1: UPLOAD KE CLOUDINARY ---
    private fun uploadImageAndSave(title: String, allItems: List<ChecklistItem>) {
        loadingDialog.show()

        // Pastikan URI tidak null (meskipun sudah dicek)
        checklistImageUri?.let { uri ->
            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        loadingDialog.dismiss()
                        val imageUrl = resultData?.get("secure_url") as? String
                        // Setelah upload, simpan ke Firebase
                        saveDataToFirebase(title, allItems, imageUrl)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Upload gambar gagal: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                })
                .dispatch()
        }
    }

    // --- FUNGSI BARU 2: SIMPAN KE FIREBASE ---
    private fun saveDataToFirebase(title: String, allItems: List<ChecklistItem>, imageUrl: String?) {
        // Cek apakah kita sedang 'Edit' atau 'Buat Baru'
        if (editingChecklist == null) {
            // MODE BUAT BARU
            val newChecklist = Checklist(
                id = UUID.randomUUID().toString(), // ID akan diganti oleh Repository
                title = title,
                createdDate = Date(),
                imagePath = imageUrl, // URL online
                items = allItems
            )
            taskViewModel.insertChecklist(newChecklist)
            Toast.makeText(requireContext(), "Checklist Disimpan!", Toast.LENGTH_SHORT).show()

        } else {
            // MODE EDIT
            val updatedChecklist = editingChecklist!!.copy(
                title = title,
                imagePath = imageUrl, // URL online (bisa null jika dihapus)
                items = allItems,
                createdDate = editingChecklist!!.createdDate // Tetap pakai tanggal lama
            )
            taskViewModel.updateChecklist(updatedChecklist)
            Toast.makeText(requireContext(), "Checklist Diperbarui!", Toast.LENGTH_SHORT).show()
        }

        findNavController().popBackStack() // Kembali ke TaskListFragment
    }

    // --- FUNGSI LAINNYA (TIDAK BERUBAH) ---
    private fun addNewItem() {
        // ... (kode Anda sudah benar)
        val newItem = ChecklistItem(UUID.randomUUID().toString(), "", false)
        activeItems.add(newItem)
        activeAdapter.notifyItemInserted(activeItems.size - 1)
        rvActiveItems.smoothScrollToPosition(activeItems.size - 1)
    }

    private fun updateCheckedHeaderVisibility() {
        // ... (kode Anda sudah benar)
        if (checkedItems.isEmpty()) {
            divider.visibility = View.GONE
            tvCheckedHeader.visibility = View.GONE
        } else {
            divider.visibility = View.VISIBLE
            tvCheckedHeader.visibility = View.VISIBLE
        }
    }

    override fun onCheckChanged(position: Int, isChecked: Boolean) {
        // ... (kode Anda sudah benar)
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
            Log.w("AddChecklistFragment", "onCheckChanged error: ${e.message}")
        }
    }

    override fun onDeleteClicked(position: Int, isFromActiveList: Boolean) {
        // ... (kode Anda sudah benar)
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
        // ... (kode Anda sudah benar)
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
        // ... (kode Anda sudah benar)
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
        // ... (kode Anda sudah benar)
        when (source) {
            ImageSourceBottomSheetFragment.SOURCE_CAMERA -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            ImageSourceBottomSheetFragment.SOURCE_GALLERY -> {
                pickChecklistImageLauncher.launch("image/*")
            }
        }
    }
    // --- TAMBAHKAN FUNGSI HELPER BARU INI ---
// Fungsi ini akan meng-scan RecyclerView dan 'memanen' teks dari EditText
    private fun updateActiveItemsFromViews() {
        for (i in 0 until activeAdapter.itemCount) {
            // Dapatkan ViewHolder untuk setiap item di adapter
            val viewHolder = rvActiveItems.findViewHolderForAdapterPosition(i) as? ChecklistItemAdapter.ChecklistItemViewHolder

            if (viewHolder != null) {
                // Jika ViewHolder terlihat di layar, ambil teksnya
                val text = viewHolder.editText.text.toString()

                // Update list 'activeItems' secara manual
                if (i < activeItems.size) { // Cek keamanan
                    activeItems[i].text = text
                }
            }
            // Jika ViewHolder tidak terlihat (di-scroll), datanya
            // seharusnya sudah tersimpan oleh TextWatcher
        }
    }
}