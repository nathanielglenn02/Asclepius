package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private val pickImageRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set onClickListener untuk tombol galeri
        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        // Set onClickListener untuk tombol analisis
        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    private fun startGallery() {
        // Mendapatkan gambar dari Gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pickImageRequestCode)
    }

    private fun showImage() {
        // Menampilkan gambar sesuai Gallery yang dipilih
        currentImageUri?.let { uri ->
            binding.previewImageView.setImageURI(uri)
        } ?: showToast("Gambar tidak tersedia")
    }

    private fun analyzeImage() {
        // Menganalisa gambar yang berhasil ditampilkan
        currentImageUri?.let { uri ->
            // Inisialisasi ImageClassifierHelper dan lakukan prediksi
            val classifier = ImageClassifierHelper(context = this)
            val (label, confidence) = classifier.classifyStaticImage(uri) ?: return showToast("Gagal memproses gambar")

            // Menampilkan hasil prediksi
            val prediction = "$label : ${"%.2f".format(confidence)}%"
            showToast(prediction)
            classifier.close()

            moveToResult(uri, prediction) // Mengirim URI gambar dan hasil prediksi ke ResultActivity
        } ?: showToast("Pilih gambar terlebih dahulu untuk dianalisa")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                currentImageUri = uri
                showImage() // Menampilkan gambar setelah dipilih
            }
        }
    }

    private fun moveToResult(imageUri: Uri, prediction: String) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("IMAGE_URI", imageUri.toString())
        intent.putExtra("PREDICTION", prediction)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
