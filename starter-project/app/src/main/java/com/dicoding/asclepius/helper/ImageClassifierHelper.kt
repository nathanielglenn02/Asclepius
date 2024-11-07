package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

class ImageClassifierHelper(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private val inputSize = 224 // Sesuaikan ukuran input model di sini

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        // TODO: Menyiapkan Image Classifier untuk memproses gambar.
        try {
            val model = loadModelFile()
            interpreter = Interpreter(model)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("cancer_classification.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        // Mengubah ukuran gambar ke dimensi input yang diharapkan (misal: 224x224)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        return tensorImage
    }

    fun classifyStaticImage(imageUri: Uri): Pair<String, Float>? {
        // TODO: mengklasifikasikan imageUri dari gambar statis.

        // Mengambil bitmap dari URI
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

        // Preprocess gambar
        val tensorImage = preprocessImage(bitmap)

        // Buffer output untuk hasil prediksi
        val outputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)

        // Menjalankan inferensi
        interpreter.run(tensorImage.buffer, outputTensorBuffer.buffer.rewind())

        // Mendapatkan hasil prediksi
        val confidenceScores = outputTensorBuffer.floatArray
        val maxIndex = confidenceScores.indices.maxByOrNull { confidenceScores[it] } ?: return null
        val labels = arrayOf("Non-Cancer", "Cancer")

        return labels[maxIndex] to (confidenceScores[maxIndex] * 100)
    }

    fun close() {
        interpreter.close()
    }
}
