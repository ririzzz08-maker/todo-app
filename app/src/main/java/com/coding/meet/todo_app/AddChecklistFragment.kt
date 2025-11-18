package com.coding.meet.todo_app

// --- IMPORT YANG DIPERLUKAN ---
import android.Manifest
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.coding.meet.todo_app.adapters.CheckedItemAdapter
import com.coding.meet.todo_app.adapters.ChecklistItemAdapter
import com.coding.meet.todo_app.adapters.ChecklistItemListener
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.ChecklistItem
import com.coding.meet.todo_app.utils.setupDialog
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddChecklistFragment : Fragment(), ChecklistItemListener, ImageSourceBottomSheetFragment.ImageSourceListener {

    // --- DIALOG LOADING (BARU) ---
    private val loadingDialog: Dialog by lazy {
        Dialog(requireContext(), R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.custom_loading_dialog)
        }
    }

    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    // --- MODIFIKASI: 'args' sekarang akan berisi 'checklist_obj' ---
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

        // --- Setup Adapter ---
        activeAdapter = ChecklistItemAdapter(activeItems, this)
        rvActiveItems.adapter = activeAdapter
        checkedAdapter = CheckedItemAdapter(checkedItems, this)
        rvCheckedItems.adapter = checkedAdapter

        // ==========================================================
        // --- MODIFIKASI: Logika Mode Edit vs Mode Buat Baru ---
        // ==========================================================

        // Ambil objek checklist dari argumen (nama baru: checklist_obj)
        val checklistFromArgs = arguments?.getParcelable<Checklist>("checklist_obj") // "checklist_obj" adalah kunci kita

        if (checklistFromArgs == null) {
            // MODE BUAT BARU (Tidak ada objek dikirim)
            toolbar.title = "Buat Checklist Baru"
            activeItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
            activeAdapter.notifyItemInserted(0)
        } else {
            // MODE EDIT (Objek diterima!)
            toolbar.title = "Edit Checklist"

            editingChecklist = checklistFromArgs
            etChecklistTitle.setText(checklistFromArgs.title)

            checklistFromArgs.imagePath?.let { imageUrl ->
                ivChecklistImage.load(imageUrl) { crossfade(true) }
                frameLayoutChecklistImageContainer.visibility = View.VISIBLE
                ivDeleteChecklistImage.visibility = View.VISIBLE
            }

            val (checked, active) = checklistFromArgs.items.partition { it.isChecked }
            activeItems.clear()
            activeItems.addAll(active)
            activeAdapter.notifyDataSetChanged()
            checkedItems.clear()
            checkedItems.addAll(checked)
            checkedAdapter.notifyDataSetChanged()
        }
        // ==========================================================
        // --- AKHIR DARI BLOK PENGGANTI ---
        // ==========================================================


        // --- Setup Listeners (Tetap Sama) ---
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        btnAddItem.setOnClickListener {
            addNewItem()
        }
        btnSaveChecklist.setOnClickListener {
            saveChecklistClicked()
        }
        btnPickChecklistImage.setOnClickListener {
            val imageSourceBottomSheet = ImageSourceBottomSheetFragment()
            imageSourceBottomSheet.listener = this
            imageSourceBottomSheet.show(parentFragmentManager, ImageSourceBottomSheetFragment.TAG)
        }

        ivDeleteChecklistImage.setOnClickListener {
            checklistImageUri = null
            editingChecklist?.imagePath = null

            ivChecklistImage.setImageURI(null)
            frameLayoutChecklistImageContainer.visibility = View.GONE
            ivDeleteChecklistImage.visibility = View.GONE
            Toast.makeText(requireContext(), "Gambar dihapus.", Toast.LENGTH_SHORT).show()
        }

        updateCheckedHeaderVisibility()
    }

    // --- MODIFIKASI: FUNGSI 'loadChecklistData' DIHAPUS ---
    // (Kita tidak membutuhkannya lagi)


    // --- FUNGSI SIMPAN (Logika Anda sudah benar dan tidak diubah) ---
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

        if (checklistImageUri != null) {
            uploadImageAndSave(title, allItems)
        } else {
            val existingImageUrl = if (editingChecklist != null) {
                editingChecklist!!.imagePath
            } else {
                null
            }
            saveDataToFirebase(title, allItems, existingImageUrl)
        }
    }

    // --- FUNGSI UPLOAD (Tidak diubah) ---
    private fun uploadImageAndSave(title: String, allItems: List<ChecklistItem>) {
        loadingDialog.show()
        checklistImageUri?.let { uri ->
            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        loadingDialog.dismiss()
                        val imageUrl = resultData?.get("secure_url") as? String
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

    // --- FUNGSI SIMPAN KE FIREBASE (Tidak diubah) ---
    private fun saveDataToFirebase(title: String, allItems: List<ChecklistItem>, imageUrl: String?) {
        if (editingChecklist == null) {
            // MODE BUAT BARU
            val newChecklist = Checklist(
                id = UUID.randomUUID().toString(),
                title = title,
                createdDate = Date(),
                imagePath = imageUrl,
                items = allItems
            )
            taskViewModel.insertChecklist(newChecklist)
            Toast.makeText(requireContext(), "Checklist Disimpan!", Toast.LENGTH_SHORT).show()

        } else {
            // MODE EDIT
            val updatedChecklist = editingChecklist!!.copy(
                title = title,
                imagePath = imageUrl,
                items = allItems,
                createdDate = editingChecklist!!.createdDate
            )
            taskViewModel.updateChecklist(updatedChecklist)
            Toast.makeText(requireContext(), "Checklist Diperbarui!", Toast.LENGTH_SHORT).show()
        }
        findNavController().popBackStack()
    }

    // --- FUNGSI LAINNYA (Tidak diubah) ---
    private fun addNewItem() {
        val newItem = ChecklistItem(UUID.randomUUID().toString(), "", false)
        activeItems.add(newItem)
        activeAdapter.notifyItemInserted(activeItems.size - 1)
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

    // --- FUNGSI KAMERA & GALERI (Tidak diubah) ---
    private fun startCameraForChecklist() {
        val photoFile: File? = try {
            createChecklistImageFile()
        } catch (ignored: IOException) {
            Toast.makeText(requireContext(), "Error saat memuat file gambar.", Toast.LENGTH_SHORT).show()
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

    // --- FUNGSI HELPER (Tidak diubah) ---
    private fun updateActiveItemsFromViews() {
        for (i in 0 until activeAdapter.itemCount) {
            val viewHolder = rvActiveItems.findViewHolderForAdapterPosition(i) as? ChecklistItemAdapter.ChecklistItemViewHolder

            if (viewHolder != null) {
                val text = viewHolder.editText.text.toString()
                if (i < activeItems.size) {
                    activeItems[i].text = text
                }
            }
        }
    }
}