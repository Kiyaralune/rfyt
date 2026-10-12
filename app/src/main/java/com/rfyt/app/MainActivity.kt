package com.rfyt.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rfyt.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: VideoAdapter
    private var currentQuery = "trending indonesia"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()

        binding.swipeRefresh.setOnRefreshListener {
            loadVideos(currentQuery)
        }

        loadVideos(currentQuery)
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter { item ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("VIDEO_ID", item.id)
                putExtra("VIDEO_TITLE", item.title)
                putExtra("VIDEO_AUTHOR", item.author)
            }
            startActivity(intent)
        }

        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = v.text.toString().trim()
                if (q.isNotEmpty()) {
                    currentQuery = q
                    loadVideos(q)
                }
                true
            } else {
                false
            }
        }

        binding.btnSearch.setOnClickListener {
            val q = binding.etSearch.text.toString().trim()
            if (q.isNotEmpty()) {
                currentQuery = q
                loadVideos(q)
            }
        }
    }

    private fun loadVideos(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false

        lifecycleScope.launch {
            try {
                val list = YtScraper.searchVideos(query)
                adapter.submitList(list)
                if (list.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Tidak ada hasil untuk '$query'", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Gagal memuat video: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
