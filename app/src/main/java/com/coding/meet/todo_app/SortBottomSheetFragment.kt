package com.coding.meet.todo_app
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.lifecycle.ViewModelProvider // <-- 1. IMPORT TAMBAHAN
import com.coding.meet.todo_app.viewmodels.TaskViewModel // <-- 2. IMPORT TAMBAHAN
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortBottomSheetFragment : BottomSheetDialogFragment() {

    private var onSortSelected: ((String) -> Unit)? = null

    // 3. TAMBAHAN: Dapatkan referensi ke ViewModel yang sama dengan Activity
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SortBottomSheet", "========== onCreateView() dipanggil ==========")
        return try {
            val view = inflater.inflate(R.layout.bottom_sheet_sort, container, false)
            Log.d("SortBottomSheet", "Layout inflate SUCCESS")
            view
        } catch (e: Exception) {
            Log.e("SortBottomSheet", "ERROR inflate layout", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SortBottomSheet", "onViewCreated() dipanggil")

        val radioGroup = view.findViewById<RadioGroup>(R.id.sortOptionsGroup)

        if (radioGroup == null) {
            Log.e("SortBottomSheet", "ERROR: RadioGroup is NULL!")
            return
        }

        Log.d("SortBottomSheet", "RadioGroup ditemukan")

        // ==================================================
        // 4. TAMBAHAN: SET STATUS RADIO BUTTON SAAT INI
        // ==================================================
        // Dapatkan status sortir saat ini dari ViewModel
        val currentSortPair = taskViewModel.sortByLiveData.value

        if (currentSortPair != null) {
            if (currentSortPair.first == "date") {
                if (currentSortPair.second) { // true = Ascending (Tanggal Dibuat)
                    radioGroup.check(R.id.radioDateCreated)
                } else { // false = Descending (Tanggal Diubah)
                    radioGroup.check(R.id.radioDateModified)
                }
            } else if (currentSortPair.first == "title") { // <-- TAMBAHKAN ELSE IF INI
                // Jika "title", maka centang "Kustom"
                radioGroup.check(R.id.radioCustom)
            } else {
                // Default-kan ke Kustom jika "custom" atau lainnya
                radioGroup.check(R.id.radioCustom)
            }
        } else {
            // Jika ViewModel null
            radioGroup.check(R.id.radioCustom)
        }

        // --- TANDA KURUNG '}' YANG SALAH SUDAH DIHAPUS DARI SINI ---

        // Kode listener Anda (sekarang berada di dalam onViewCreated)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val sortType = when (checkedId) {
                R.id.radioCustom -> "custom"
                R.id.radioDateCreated -> "created_date"
                R.id.radioDateModified -> "modified_date"
                else -> "custom"
            }
            Log.d("SortBottomSheet", "Sort dipilih: $sortType")
            onSortSelected?.invoke(sortType)
            dismiss()
        }
    } // <-- Penutup onViewCreated yang benar

    fun setOnSortSelectedListener(listener: (String) -> Unit) {
        Log.d("SortBottomSheet", "Listener di-set")
        this.onSortSelected = listener
    }

    companion object {
        const val TAG = "SortBottomSheet"
    }
}