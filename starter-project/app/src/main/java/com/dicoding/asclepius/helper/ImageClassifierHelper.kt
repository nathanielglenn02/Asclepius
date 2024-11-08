package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
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
import kotlin.math.exp

class ImageClassifierHelper(private val context: Context) {

    private lateinit var interpreter: Interpreter
    private val inputSize = 224

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
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
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        return tensorImage
    }

    fun classifyStaticImage(imageUri: Uri): Pair<String, Float>? {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val tensorImage = preprocessImage(bitmap)
        println("Tensor size: ${tensorImage.width} x ${tensorImage.height}")
        val outputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 2), DataType.FLOAT32)
        interpreter.run(tensorImage.buffer, outputTensorBuffer.buffer.rewind())
        val confidenceScores = applySoftmax(outputTensorBuffer.floatArray)
        println("Confidence scores: ${confidenceScores.joinToString()}")
        val maxIndex = confidenceScores.indices.maxByOrNull { confidenceScores[it] } ?: return null
        val labels = arrayOf("Non-Cancer", "Cancer")
        return labels[maxIndex] to (confidenceScores[maxIndex] * 100)
    }

    private fun applySoftmax(scores: FloatArray): FloatArray {
        val maxScore = scores.maxOrNull() ?: 0f
        val expScores = scores.map { exp((it - maxScore).toDouble()).toFloat() }
        val sumExpScores = expScores.sum()
        return expScores.map { it / sumExpScores }.toFloatArray()
    }

    fun close() {
        interpreter.close()
    }
}
