// Ganti semua kode di MainActivity.kt dengan ini
package com.coding.meet.todo_app

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // MODIFIKASI: Import untuk mengambil warna dari colors.xml
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.coding.meet.todo_app.databinding.ActivityMainBinding
import com.coding.meet.todo_app.models.Checklist
import com.coding.meet.todo_app.models.Task
import com.coding.meet.todo_app.utils.Status
import com.coding.meet.todo_app.viewmodels.TaskViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Gunakan 'by viewModels()' untuk mendapatkan ViewModel di Activity
    private val taskViewModel: TaskViewModel by viewModels()

    private lateinit var auth: FirebaseAuth
    private var currentTasks: List<Task>? = null
    private var currentChecklists: List<Checklist>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        auth = FirebaseAuth.getInstance()

        // Panggil fungsi setup kita
        setupNavigationDrawer()
        observeTasksForNavMenu()
        observeChecklistsForNavMenu()
    }

    private fun setupNavigationDrawer() {
        // 1. Setup tombol menu untuk membuka drawer
        mainBinding.menuButton.setOnClickListener {
            mainBinding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 2. Setup listener untuk item yang diklik di drawer
        mainBinding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                else -> {
                    // Blok ini sudah benar, menangani klik pada Task DAN Checklist
                    val clickedItemId = menuItem.itemId
                    val taskList = taskViewModel.taskStateFlow.value.data
                    val checklistList = taskViewModel.allChecklists.value

                    val clickedTask = taskList?.firstOrNull { it.id.hashCode() == clickedItemId }

                    if (clickedTask != null) {
                        taskViewModel.selectTaskForEdit(clickedTask)
                    } else {
                        val clickedChecklist = checklistList?.firstOrNull { it.id.hashCode() == clickedItemId }
                        if (clickedChecklist != null) {
                            taskViewModel.selectChecklistForEdit(clickedChecklist)
                        } else {
                            Log.w("MainActivity", "Item tidak ditemukan: $clickedItemId")
                        }
                    }
                }
            }
            // Tutup drawer setelah diklik
            mainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // ================================================================
        // MODIFIKASI: Menambahkan kode untuk mengubah warna header
        // Letakkan ini DI DALAM fun setupNavigationDrawer()
        // ================================================================
        try {
            val menu = mainBinding.navView.menu
            val headerItem = menu.findItem(R.id.nav_header_dynamic) // ID dari nav_menu.xml
            val spannableString = SpannableString(headerItem.title)

            // Ganti warna di sini
            val headerColor = Color.parseColor("#000000") // Contoh: Hitam
            // Atau gunakan warna dari colors.xml (lebih disarankan):
            // val headerColor = ContextCompat.getColor(this, R.color.nama_warna_anda)

            spannableString.setSpan(
                ForegroundColorSpan(headerColor),
                0,
                spannableString.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            headerItem.title = spannableString
        } catch (e: Exception) {
            Log.e("MainActivity", "Gagal mengubah warna header nav", e)
        }
        // ================================================================

    } // <- Ini adalah kurung tutup dari fun setupNavigationDrawer()

    /**
     * Mengamati daftar task dari ViewModel
     */
    private fun observeTasksForNavMenu() {
        lifecycleScope.launch {
            taskViewModel.taskStateFlow.collectLatest { statusResult ->
                if (statusResult.status == Status.SUCCESS) {
                    currentTasks = statusResult.data // Simpan data
                    updateFullNavMenu() // Panggil fungsi gabungan
                }
            }
        }
    }

    /**
     * Mengupdate item menu di NavigationDrawer dengan judul task
     */
    // TAMBAHKAN FUNGSI BARU INI
    private fun updateFullNavMenu() {
        val navMenu = mainBinding.navView.menu

        // 1. HAPUS SEMUANYA
        navMenu.clear()

        // 2. TAMBAHKAN KEMBALI HEADER "CATATAN/CHECKLIST"
        // Kita tambahkan header secara manual
        navMenu.add(
            R.id.group_header, // ID grup dari nav_menu.xml
            R.id.nav_header_dynamic, // ID item dari nav_menu.xml
            0, // Urutan
            "CATATAN/CHECKLIST"
        ).apply {
            isEnabled = false // Buat agar tidak bisa diklik

            // Terapkan lagi pewarnaan header Anda
            try {
                val spannableString = SpannableString(this.title)
                val headerColor = Color.parseColor("#000000") // Ganti dengan warna Anda
                spannableString.setSpan(
                    ForegroundColorSpan(headerColor),
                    0,
                    spannableString.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                this.title = spannableString
            } catch (e: Exception) {
                Log.e("MainActivity", "Gagal mewarnai header", e)
            }
        }

        // 3. Tambahkan Checklists (DENGAN URUTAN YANG BENAR)
        currentChecklists?.forEachIndexed { index, checklist ->
            val uniqueId = checklist.id.hashCode()
            navMenu.add(
                R.id.group_dynamic_checklists, // ID grup checklist
                uniqueId,
                index + 1, // Urutan setelah header
                checklist.title
            ).apply {
                setIcon(R.drawable.checkbox_icon_selector) // Ikon untuk checklist
                isCheckable = true
            }
        }

        // 4. Tambahkan Tasks/Catatan (DENGAN URUTAN YANG BENAR)
        currentTasks?.forEachIndexed { index, task ->
            val uniqueId = task.id.hashCode()
            navMenu.add(
                R.id.group_dynamic_tasks, // ID grup task
                uniqueId,
                (currentChecklists?.size ?: 0) + index + 1, // Urutan setelah checklist
                task.title
            ).apply {
                // Tidak ada ikon
                isCheckable = true
            }
        }
    }

    // MODIFIKASI: Fungsi logoutUser() DIHAPUS


    // -----------------------------------------------------------------
    // Dua fungsi baru untuk Checklist (Sudah benar)
    // -----------------------------------------------------------------

    /**
     * Mengamati daftar checklist dari ViewModel
     */
    private fun observeChecklistsForNavMenu() {
        taskViewModel.allChecklists.observe(this) { checklistList ->
            currentChecklists = checklistList // Simpan data
            updateFullNavMenu()
        }
    }

    /**
     * Mengupdate item menu di NavigationDrawer dengan judul checklist
     */

}
