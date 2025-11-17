package com.coding.meet.todo_app.adapters

import android.view.LayoutInflater
import android.view.View // BARU: Import View
import android.view.ViewGroup
import androidx.core.net.toUri // BARU: Import toUri
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.databinding.ViewTaskListLayoutBinding // Sesuaikan nama binding ini jika beda
import com.coding.meet.todo_app.models.Task
import coil.load // BARU: Import Coil yang benar
import java.text.SimpleDateFormat
import java.util.Locale

class TaskRVVBListAdapter(
    private val isList: MutableLiveData<Boolean>, // Ini sepertinya untuk grid/list, tidak apa-apa
    private val onItemClick: (String, Int, Task) -> Unit
) : ListAdapter<Task, TaskRVVBListAdapter.TaskViewHolder>(DiffCallback()) {

    inner class TaskViewHolder(private val binding: ViewTaskListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // 1. Set Judul
            binding.titleTxt.text = task.title

            // 2. Set Tanggal
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss a", Locale.getDefault())
            binding.dateTxt.text = dateFormat.format(task.date)

// --- PERBAIKAN DI SINI ---
            // 1. Salin 'imagePath' (var) ke 'val' lokal
            val imageUrl = task.imagePath

            // 2. Gunakan 'val' lokal untuk pengecekan dan pemuatan
            if (imageUrl.isNullOrEmpty()) {
                // Sembunyikan jika TIDAK ADA gambar
                binding.itemImagePreview.visibility = View.GONE
            } else {
                // Tampilkan jika ADA gambar
                binding.itemImagePreview.visibility = View.VISIBLE
                // Muat gambar menggunakan Coil (langsung dari URL String)
                binding.itemImagePreview.load(imageUrl) {
                    crossfade(true) // Efek fade-in
                    // placeholder(R.drawable.ic_placeholder) // Opsional
                }
            }

            // 5. Handle Klik (Logika Anda yang sudah ada)
            binding.editImg.setOnClickListener {
                onItemClick("update", adapterPosition, task)
            }
            binding.deleteImg.setOnClickListener {
                onItemClick("delete", adapterPosition, task)
            }
            binding.root.setOnClickListener {
                onItemClick("update", adapterPosition, task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ViewTaskListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}