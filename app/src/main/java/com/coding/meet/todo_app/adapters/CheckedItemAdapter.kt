package com.coding.meet.todo_app.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText // <-- UBAH KE EDITTEXT
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.R

// Adapter untuk item yang SUDAH dicentang
class CheckedItemAdapter(
    private val items: MutableList<ChecklistItem>,
    private val listener: ChecklistItemListener // Kita gunakan listener yang sama
) : RecyclerView.Adapter<CheckedItemAdapter.CheckedItemViewHolder>() {

    inner class CheckedItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Hubungkan view dari item_checklist_checked.xml
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_item_checked)
        val editText: EditText = itemView.findViewById(R.id.et_item_text_checked) // <-- UBAH KE EDITTEXT
        val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete_item_checked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckedItemViewHolder {
        // Memuat layout item_checklist_checked.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist_checked, parent, false)
        return CheckedItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckedItemViewHolder, position: Int) {
        val currentItem = items[position]

        holder.editText.setText(currentItem.text) // <-- SET TEXT KE EDITTEXT
        holder.checkBox.isChecked = currentItem.isChecked

        // Buat teksnya dicoret (strikethrough)
        holder.editText.paintFlags = holder.editText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // <-- KE EDITTEXT

        // --- Tambahkan Listener ---

        // Listener untuk Hapus
        holder.deleteButton.setOnClickListener {
            listener.onDeleteClicked(holder.adapterPosition, false)
        }

        // Listener untuk UN-CHECK (mengembalikan ke daftar aktif)
        holder.checkBox.setOnClickListener {
            listener.onCheckChanged(holder.adapterPosition, false)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}