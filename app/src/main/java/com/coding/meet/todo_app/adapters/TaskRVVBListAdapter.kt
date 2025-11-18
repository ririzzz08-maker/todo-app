package com.coding.meet.todo_app.adapters

import android.view.LayoutInflater
import android.view.View // BARU: Import View
import android.view.ViewGroup
import androidx.core.net.toUri // BARU: Import toUri
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// --- KITA KEMBALIKAN KE BINDING ANDA ---
import com.coding.meet.todo_app.databinding.ViewTaskListLayoutBinding
import com.coding.meet.todo_app.models.Task
import coil.load // BARU: Import Coil yang benar
import java.text.SimpleDateFormat
import java.util.Locale

class TaskRVVBListAdapter(
    private val isList: MutableLiveData<Boolean>, // Ini sepertinya untuk grid/list, tidak apa-apa
    private val onItemClick: (String, Int, Task) -> Unit
) : ListAdapter<Task, TaskRVVBListAdapter.TaskViewHolder>(DiffCallback()) {

    // --- Gunakan ViewTaskListLayoutBinding ---
    inner class TaskViewHolder(private val binding: ViewTaskListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // 1. Set Judul
            binding.titleTxt.text = task.title

            // 2. Set Tanggal
            // ... di dalam fun bind(task: Task) ...
            val dateFormat = SimpleDateFormat("dd MMM yyyy\nHH:mm a", Locale.getDefault())
            binding.dateTxt.text = dateFormat.format(task.date)

            // 3. Logika Gambar (Kode Anda sudah benar)
            val imageUrl = task.imagePath
            if (imageUrl.isNullOrEmpty()) {
                binding.itemImagePreview.visibility = View.GONE
            } else {
                binding.itemImagePreview.visibility = View.VISIBLE
                binding.itemImagePreview.load(imageUrl) {
                    crossfade(true)
                }
            }

            // --- PERUBAHAN 4: Atur Ulang Klik Listener ---

            // Hapus listener 'editImg' karena ikonnya akan kita hapus dari XML
            // binding.editImg.setOnClickListener { ... }

            // Listener untuk Hapus (Tetap)
            binding.deleteImg.setOnClickListener {
                onItemClick("delete", adapterPosition, task)
            }

            // Listener untuk Edit (Sekarang di seluruh kartu)
            binding.root.setOnClickListener {
                onItemClick("update", adapterPosition, task)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // --- Inflate layout yang benar ---
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