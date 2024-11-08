package com.dicoding.asclepius.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var currentImageUri: Uri? = null

    // variabel baru buat nyimpen data - data URI gambar
    private val imageUriHistory = mutableListOf<Uri>()


    // ini buat nambahin URI gambar baru ke variabel imageUriHistory
    fun addImageUri(newUri: Uri) {
        if (currentImageUri != null) {
            imageUriHistory.add(currentImageUri!!)
        }
        currentImageUri = newUri
    }

    // ini buat ngembaliin ke URI gambar sebelumnya
    fun revertToPreviousImageUri() {
        // kalau variabel imageUriHistory nggak kosong
        if (imageUriHistory.isNotEmpty()) {
            // Hapus URI gambar terakhir (index terakhir berarti sama dengan data terbaru) dari variabel imageUriHistory
            currentImageUri = imageUriHistory.removeAt(imageUriHistory.lastIndex)
        }
    }
}
