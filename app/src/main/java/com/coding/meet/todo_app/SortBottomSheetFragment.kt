package com.coding.meet.todo_app // Pastikan package Anda benar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.lifecycle.ViewModelProvider
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SortBottomSheetFragment"
    }

    private var onSortSelectedListener: ((String) -> Unit)? = null

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var radioGroupSort: RadioGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi
        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]
        radioGroupSort = view.findViewById(R.id.radioGroupSort)

        // 2. Amati data ViewModel untuk update UI
        taskViewModel.sortByLiveData.observe(viewLifecycleOwner) { sortPair ->

            // --- INI PERBAIKANNYA ---
            // A. Matikan listener SEMENTARA
            radioGroupSort.setOnCheckedChangeListener(null)

            // B. Update UI (pilih radio button yang benar)
            updateRadioSelection(sortPair)

            // C. Nyalakan listener LAGI
            setupRadioGroupListener()
            // -------------------------
        }

        // 3. Pasang listener saat pertama kali dibuka
        setupRadioGroupListener()
    }

    // Fungsi baru untuk memasang listener
    private fun setupRadioGroupListener() {
        radioGroupSort.setOnCheckedChangeListener { group, checkedId ->
            // Saat PENGGUNA (bukan kode) memilih opsi baru...
            when (checkedId) {
                R.id.radioCustom -> onSortSelectedListener?.invoke("custom")
                R.id.radioTglCreated -> onSortSelectedListener?.invoke("created_date")
                R.id.radioTglModified -> onSortSelectedListener?.invoke("modified_date")
            }
            // Tutup bottom sheet setelah memilih
            dismiss()
        }
    }

    // Fungsi untuk menandai RadioButton yang aktif
    private fun updateRadioSelection(sortPair: Pair<String, Boolean>) {
        when {
            sortPair.first == "title" -> {
                radioGroupSort.check(R.id.radioCustom)
            }
            // (Ingat, 'sortPair.second' adalah singkatan dari 'sortPair.second == true')
            sortPair.first == "date" && sortPair.second -> {
                radioGroupSort.check(R.id.radioTglCreated)
            }
            // (Dan '!sortPair.second' adalah singkatan dari 'sortPair.second == false')
            sortPair.first == "date" && !sortPair.second -> {
                radioGroupSort.check(R.id.radioTglModified)
            }
        }
    }

    fun setOnSortSelectedListener(listener: (String) -> Unit) {
        this.onSortSelectedListener = listener
    }
}