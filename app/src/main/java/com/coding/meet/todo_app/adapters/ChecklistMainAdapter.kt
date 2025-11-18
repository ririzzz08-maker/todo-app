package com.coding.meet.todo_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.coding.meet.todo_app.databinding.ItemChecklistGridBinding
import com.coding.meet.todo_app.databinding.ItemChecklistMainBinding
import com.coding.meet.todo_app.models.Checklist
import coil.load
import java.text.SimpleDateFormat
import java.util.Locale

class ChecklistMainAdapter(
    private val isList: MutableLiveData<Boolean>,
    private val onEditClick: (Checklist) -> Unit,
    private val onDeleteClick: (Checklist) -> Unit
) : ListAdapter<Checklist, ChecklistMainAdapter.ChecklistViewHolder>(DiffCallback()) {

    companion object {
        const val VIEW_TYPE_LIST = 1
        const val VIEW_TYPE_GRID = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (isList.value == true) {
            VIEW_TYPE_LIST
        } else {
            VIEW_TYPE_GRID
        }
    }

    abstract class ChecklistViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(
            checklist: Checklist,
            onEditClick: (Checklist) -> Unit,
            onDeleteClick: (Checklist) -> Unit
        )
    }

    inner class ChecklistListViewHolder(private val binding: ItemChecklistMainBinding) :
        ChecklistViewHolder(binding) {

        override fun bind(
            checklist: Checklist,
            onEditClick: (Checklist) -> Unit,
            onDeleteClick: (Checklist) -> Unit
        ) {
            binding.titleTxt.text = checklist.title

            val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss a", Locale.getDefault())
            binding.dateTxt.text = dateFormat.format(checklist.createdDate)

            val imageUrl = checklist.imagePath
            if (imageUrl.isNullOrEmpty()) {
                binding.itemImagePreview.visibility = View.GONE
            } else {
                binding.itemImagePreview.visibility = View.VISIBLE
                binding.itemImagePreview.load(imageUrl) {
                    crossfade(true)
                }
            }

            // --- PERBAIKAN DI SINI ---
            binding.deleteImg.setOnClickListener { onDeleteClick(checklist) }

            // HAPUS BARIS INI (karena editImg sudah tidak ada di XML)
            // binding.editImg.setOnClickListener { onEditClick(checklist) }

            // BIARKAN BARIS INI (Ini adalah klik di seluruh kartu)
            binding.root.setOnClickListener { onEditClick(checklist) }
            // --- SELESAI PERBAIKAN ---
        }
    }

    inner class ChecklistGridViewHolder(private val binding: ItemChecklistGridBinding) :
        ChecklistViewHolder(binding) {

        override fun bind(
            checklist: Checklist,
            onEditClick: (Checklist) -> Unit,
            onDeleteClick: (Checklist) -> Unit
        ) {
            binding.titleTxt.text = checklist.title

            val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss a", Locale.getDefault())
            binding.dateTxt.text = dateFormat.format(checklist.createdDate)

            val imageUrl = checklist.imagePath
            if (imageUrl.isNullOrEmpty()) {
                binding.itemImagePreview.visibility = View.GONE
            } else {
                binding.itemImagePreview.visibility = View.VISIBLE
                binding.itemImagePreview.load(imageUrl) {
                    crossfade(true)
                }
            }
            binding.deleteImg.setOnClickListener { onDeleteClick(checklist) }
            binding.root.setOnClickListener { onEditClick(checklist) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        return when (viewType) {
            VIEW_TYPE_LIST -> {
                val binding = ItemChecklistMainBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ChecklistListViewHolder(binding)
            }
            VIEW_TYPE_GRID -> {
                val binding = ItemChecklistGridBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ChecklistGridViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<Checklist>() {
        override fun areItemsTheSame(oldItem: Checklist, newItem: Checklist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Checklist, newItem: Checklist): Boolean {
            return oldItem == newItem
        }
    }
}