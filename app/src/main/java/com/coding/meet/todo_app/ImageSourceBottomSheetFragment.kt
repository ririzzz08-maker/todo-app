package com.coding.meet.todo_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImageSourceBottomSheetFragment : BottomSheetDialogFragment() {

    // --- "JEMBATAN" (LISTENER) KEDUA DIDEFINISIKAN DI SINI ---
    interface ImageSourceListener {
        fun onSourceSelected(source: String)
    }
    // --------------------------------------------------------

    var listener: ImageSourceListener? = null

    companion object {
        const val TAG = "ImageSourceBottomSheet"
        const val SOURCE_CAMERA = "camera"
        const val SOURCE_GALLERY = "gallery"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.image_source_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val optionCamera: TextView = view.findViewById(R.id.option_camera)
        val optionGallery: TextView = view.findViewById(R.id.option_gallery)

        optionCamera.setOnClickListener {
            listener?.onSourceSelected(SOURCE_CAMERA)
            dismiss()
        }

        optionGallery.setOnClickListener {
            listener?.onSourceSelected(SOURCE_GALLERY)
            dismiss()
        }
    }
}