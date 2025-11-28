package com.example.mygalleryapp.ml

import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceEmbeddingHelper(modelBuffer: ByteBuffer) {

    private val interpreter = Interpreter(modelBuffer)

    private val embeddingSize: Int by lazy {
        interpreter.getOutputTensor(0).shape()[1]
    }

    // Memory-efficient embedding
    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val inputBitmap = Bitmap.createScaledBitmap(bitmap, 160, 160, true)
        val input = bitmapToFloatBuffer(inputBitmap)
        val output = Array(1) { FloatArray(embeddingSize) }
        interpreter.run(input, output)
        return output[0]
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(1 * 160 * 160 * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        imgData.rewind()
        val intValues = IntArray(160 * 160)
        bitmap.getPixels(intValues, 0, 160, 0, 0, 160, 160)
        for (pixel in intValues) {
            imgData.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 128f)
            imgData.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 128f)
            imgData.putFloat(((pixel and 0xFF) - 127.5f) / 128f)
        }
        imgData.rewind()
        return imgData
    }
}
