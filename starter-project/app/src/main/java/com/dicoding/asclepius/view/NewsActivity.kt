package com.dicoding.asclepius.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityNewsBinding
import com.dicoding.asclepius.model.Article
import com.dicoding.asclepius.model.NewsResponse
import com.dicoding.asclepius.network.NewsApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        newsAdapter = NewsAdapter(listOf())
        binding.recyclerView.adapter = newsAdapter
        fetchNews()
    }

    private fun fetchNews() {
        val apikey = "47031cbda2304960bb08840a96c4784d"
        val apiService = NewsApiService.create()
        val call = apiService.getNews("cancer", "health", "en", apikey)

        call.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (response.isSuccessful) {
                    val articles = response.body()?.articles ?: listOf()
                    updateAdapterData(articles)
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
            }
        })
    }

    private fun updateAdapterData(articles: List<Article>) {
        newsAdapter.updateArticles(articles)
    }
}
