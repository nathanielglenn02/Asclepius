package com.dicoding.asclepius.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private val cropImageRequestCode = UCrop.REQUEST_CROP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memeriksa dan meminta izin
        checkAndRequestPermissions()

        // Set onClickListener untuk tombol galeri
        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        // Set onClickListener untuk tombol analisis
        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    // Menggunakan ActivityResultContracts untuk izin
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.all { it.value }
        if (!granted) {
            showToast("Izin akses diperlukan untuk melanjutkan")
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        }
    }

    // Menggunakan ActivityResultContracts untuk galeri
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private fun startGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun startCrop(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))

        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop & Rotate Image")
        }

        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(224, 224)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cropImageRequestCode && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                currentImageUri = it
                showImage() // Menampilkan gambar yang baru dicrop
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.let { showToast("Crop error: ${it.message}") }
        }
    }

    private fun showImage() {
        currentImageUri?.let { uri ->
            binding.previewImageView.setImageURI(null) // Reset terlebih dahulu untuk menghindari cache
            binding.previewImageView.setImageURI(uri) // Set gambar terbaru
        } ?: showToast("Gambar tidak tersedia")
    }

    private fun analyzeImage() {
        currentImageUri?.let { uri ->
            val classifier = ImageClassifierHelper(context = this)
            val (label, confidence) = classifier.classifyStaticImage(uri) ?: return showToast("Gagal memproses gambar")

            val prediction = "$label : ${"%.2f".format(confidence)}%"
            showToast(prediction)
            classifier.close()

            moveToResult(uri, prediction)
        } ?: showToast("Pilih gambar terlebih dahulu untuk dianalisa")
    }

    private fun moveToResult(imageUri: Uri, prediction: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("IMAGE_URI", imageUri.toString())
            putExtra("PREDICTION", prediction)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
