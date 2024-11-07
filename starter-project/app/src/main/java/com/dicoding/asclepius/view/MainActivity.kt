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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.data.AppDatabase
import com.dicoding.asclepius.data.History
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private val pickImageRequestCode = 1
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

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
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

    // Fungsi untuk menyimpan data prediksi ke database
    private fun savePredictionToDatabase(imageUri: String, prediction: String, confidence: Float) {
        val history = History(
            imageUri = imageUri,
            prediction = prediction,
            confidence = confidence
        )
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            database.historyDao().insertHistory(history)
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
        }

        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(224, 224)
            .start(this) // Menggunakan "start(this)" tanpa cropLauncher
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                startCrop(uri) // Memulai proses crop setelah memilih gambar dari galeri
            }
        } else if (requestCode == cropImageRequestCode && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                currentImageUri = it
                showImage() // Menampilkan gambar yang sudah dicrop ke previewImageView
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.let { showToast("Crop error: ${it.message}") }
        }
    }

    private fun showImage() {
        currentImageUri?.let { uri ->
            binding.previewImageView.setImageURI(null) // Reset terlebih dahulu untuk menghindari cache
            binding.previewImageView.setImageURI(uri) // Set gambar terbaru yang sudah dicrop
        } ?: showToast("Gambar tidak tersedia")
    }

    private fun analyzeImage() {
        currentImageUri?.let { uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            // Simpan gambar yang sudah diproses ke folder cache
            val savedUri = saveImageToCache(bitmap)
            if (savedUri == null) {
                showToast("Gagal menyimpan gambar.")
                return
            }
            val classifier = ImageClassifierHelper(context = this)
            val (label, confidence) = classifier.classifyStaticImage(uri) ?: return showToast("Gagal memproses gambar")

            val prediction = "$label : ${"%.2f".format(confidence)}%"
            showToast(prediction)
            classifier.close()

            // Simpan ke database setelah prediksi selesai
            savePredictionToDatabase(savedUri.toString(), label, confidence)

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
