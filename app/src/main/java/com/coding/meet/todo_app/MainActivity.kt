package com.coding.meet.todo_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coding.meet.todo_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        // KOSONG!
        // Semua logika sudah dipindahkan ke TaskListFragment.
        // NavHostFragment di activity_main.xml akan otomatis
        // menangani sisanya.
    }
}