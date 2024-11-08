package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val prediction = intent.getStringExtra("PREDICTION")

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            binding.resultImage.setImageURI(imageUri)
        }

        binding.resultText.text = prediction ?: "Prediksi tidak tersedia"
    }
}
