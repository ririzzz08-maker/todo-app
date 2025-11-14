package com.coding.meet.todo_app.adapters

import android.view.LayoutInflater
import android.view.View // BARU: Import View
import android.view.ViewGroup
import androidx.core.net.toUri // BARU: Import toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coding.meet.todo_app.databinding.ItemChecklistMainBinding
import com.coding.meet.todo_app.models.Checklist
import coil.load // BARU: Import Coil
import java.text.SimpleDateFormat
import java.util.Locale

class ChecklistMainAdapter(
    private val onEditClick: (Checklist) -> Unit,
    private val onDeleteClick: (Checklist) -> Unit
) : ListAdapter<Checklist, ChecklistMainAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemChecklistMainBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(checklist: Checklist) {
            // 1. Set Judul (Tetap)
            binding.titleTxt.text = checklist.title

            // 2. Set Tanggal (Tetap)
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss a", Locale.getDefault())
            binding.dateTxt.text = dateFormat.format(checklist.createdDate)

            // 3. Hapus preview item (sesuai permintaan Anda sebelumnya)
            // (Kita asumsikan TextView-nya sudah dihapus atau disembunyikan di XML)

            // 4. BARU: Logika untuk menampilkan gambar
            if (checklist.imagePath.isNullOrEmpty()) {
                // Sembunyikan jika TIDAK ADA gambar
                binding.itemImagePreview.visibility = View.GONE
            } else {
                // Tampilkan jika ADA gambar
                binding.itemImagePreview.visibility = View.VISIBLE
                // Muat gambar menggunakan Coil
                binding.itemImagePreview.load(checklist.imagePath.toUri()) {
                    crossfade(true) // Efek fade-in
                    // placeholder(R.drawable.ic_placeholder) // Opsional: Ganti dengan gambar placeholder
                    // error(R.drawable.ic_error) // Opsional: Ganti dengan gambar jika error
                }
            }

            // 5. Handle Klik Hapus (Tetap)
            binding.deleteImg.setOnClickListener {
                onDeleteClick(checklist)
            }

            // 6. Handle Klik Edit (Tetap)
            binding.editImg.setOnClickListener {
                onEditClick(checklist)
            }

            binding.root.setOnClickListener {
                onEditClick(checklist)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChecklistMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
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