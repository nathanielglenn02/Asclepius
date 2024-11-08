package com.dicoding.asclepius.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.R
import com.dicoding.asclepius.data.AppDatabase
import com.dicoding.asclepius.data.History
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val pickImageRequestCode = 1
    private val cropImageRequestCode = UCrop.REQUEST_CROP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memeriksa dan meminta izin
        checkAndRequestPermissions()

        // Tampilkan gambar jika `currentImageUri` sudah ada di ViewModel
        viewModel.currentImageUri?.let { showImage(it) }

        // Set onClickListener untuk tombol galeri
        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        // Set onClickListener untuk tombol analisis
        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.newsButton.setOnClickListener {
            val intent = Intent(this, NewsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 0)
        }
    }

    private fun startGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, pickImageRequestCode)
    }

    private fun startCrop(imageUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop & Rotate Image")
            setToolbarCancelDrawable(R.drawable.ic_transparent)
        }

        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(224, 224)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                startCrop(uri)
            }
        } else if (requestCode == cropImageRequestCode && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                viewModel.currentImageUri = it
                showImage(it)
            }
        } else if (requestCode == cropImageRequestCode && resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.let { showToast("Crop error: ${it.message}") }
        }
    }

    private fun showImage(uri: Uri) {
        binding.previewImageView.setImageURI(null)
        binding.previewImageView.setImageURI(uri)
    }

    private fun analyzeImage() {
        val uriToAnalyze = viewModel.currentImageUri

        uriToAnalyze?.let { uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val savedUri = saveImageToCache(bitmap)
            if (savedUri == null) {
                showToast("Gagal menyimpan gambar.")
                return
            }

            val classifier = ImageClassifierHelper(this)
            val (label, confidence) = classifier.classifyStaticImage(uri) ?: return showToast("Gagal memproses gambar.")

            val prediction = "$label : ${"%.2f".format(confidence)}%"
            showToast(prediction)
            classifier.close()
            savePredictionToDatabase(savedUri.toString(), label, confidence)
            moveToResult(savedUri, prediction)
        } ?: showToast("Pilih gambar terlebih dahulu untuk dianalisa.")
    }

    private fun moveToResult(imageUri: Uri, prediction: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("IMAGE_URI", imageUri.toString())
            putExtra("PREDICTION", prediction)
        }
        startActivity(intent)
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri? {
        val fileName = "predicted_image_${UUID.randomUUID()}.jpg"
        val file = File(cacheDir, fileName)
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun savePredictionToDatabase(imageUri: String, prediction: String, confidence: Float) {
        val history = History(
            imageUri = imageUri,
            prediction = prediction,
            confidence = confidence)
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            database.historyDao().insertHistory(history)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
