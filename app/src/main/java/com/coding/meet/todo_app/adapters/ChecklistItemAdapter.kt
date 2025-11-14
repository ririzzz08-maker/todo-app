package com.coding.meet.todo_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.R
import com.coding.meet.todo_app.models.ChecklistItem

// --- INI ADALAH DEFINISI "JEMBATAN" YANG BENAR ---
interface ChecklistItemListener {
    // 'isFromActiveList' memberi tahu fragment dari daftar mana asalnya
    fun onDeleteClicked(position: Int, isFromActiveList: Boolean)
    fun onCheckChanged(position: Int, isChecked: Boolean)
    // fun onTextChanged(position: Int, text: String) // Nanti kita tambahkan ini
}
// ------------------------------------------

class ChecklistItemAdapter(
    private val items: MutableList<ChecklistItem>,
    private val listener: ChecklistItemListener
) : RecyclerView.Adapter<ChecklistItemAdapter.ChecklistItemViewHolder>() {

    inner class ChecklistItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_item)
        val editText: EditText = itemView.findViewById(R.id.et_item_text)
        val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist, parent, false)
        return ChecklistItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistItemViewHolder, position: Int) {
        val currentItem = items[position]

        holder.editText.setText(currentItem.text)
        holder.checkBox.isChecked = currentItem.isChecked

        // Panggil "jembatan" dengan 2 argumen
        holder.deleteButton.setOnClickListener {
            listener.onDeleteClicked(holder.adapterPosition, true) // 'true' = dari daftar aktif
        }

        holder.checkBox.setOnClickListener {
            items[holder.adapterPosition].text = holder.editText.text.toString()
            // -------------------------

            // Baru panggil listener setelah data disimpan
            listener.onCheckChanged(holder.adapterPosition, holder.checkBox.isChecked)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

// --- INI ADALAH DEFINISI "MODEL DATA" YANG BENAR ---
// (Kita hanya mendefinisikannya di SATU tempat ini)
data class ChecklistItem(
    val id: String,
    var text: String,
    var isChecked: Boolean
)