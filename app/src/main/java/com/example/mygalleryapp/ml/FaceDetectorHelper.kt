package com.example.mygalleryapp.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FaceDetectorHelper {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    // Async suspend function to get faces
    suspend fun detectFaces(bitmap: Bitmap): List<Rect> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces ->
                val rects = faces.map { it.boundingBox }
                cont.resume(rects)
            }
            .addOnFailureListener {
                cont.resume(emptyList())
            }
    }

    // Synchronous helper (for batch processing in background)
    fun detectFacesSync(bitmap: Bitmap): List<Rect> {
        return runCatching { runBlocking { detectFaces(bitmap) } }.getOrElse { emptyList() }
    }
}
