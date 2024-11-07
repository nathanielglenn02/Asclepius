package com.dicoding.asclepius.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asclepius.data.AppDatabase
import com.dicoding.asclepius.data.History
import com.dicoding.asclepius.databinding.ActivityHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadHistory()
    }

    private fun loadHistory() {
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val historyList = database.historyDao().getAllHistories()
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter = HistoryAdapter(historyList)
            }
        }
    }
}
