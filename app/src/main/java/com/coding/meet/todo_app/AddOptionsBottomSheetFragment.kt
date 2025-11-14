package com.coding.meet.todo_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AddOptionsBottomSheetFragment : BottomSheetDialogFragment() {

    // --- "JEMBATAN" (LISTENER) DIDEFINISIKAN DI SINI ---
    interface AddOptionsListener {
        fun onOptionSelected(option: String)
    }
    // ----------------------------------------------------

    var listener: AddOptionsListener? = null

    companion object {
        const val TAG = "AddOptionsBottomSheet"
    }

    // Menerapkan Style Transparan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_options_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hubungkan ke komponen baru
        val optionList: ExtendedFloatingActionButton = view.findViewById(R.id.option_list)
        val optionCatatan: ExtendedFloatingActionButton = view.findViewById(R.id.option_catatan)
        val optionClose: FloatingActionButton = view.findViewById(R.id.option_close)

        // Pasang listener
        optionList.setOnClickListener {
            listener?.onOptionSelected("list")
            dismiss()
        }
        optionCatatan.setOnClickListener {
            listener?.onOptionSelected("catatan")
            dismiss()
        }


        optionClose.setOnClickListener {
            dismiss() // Hanya tutup
        }
    }
}