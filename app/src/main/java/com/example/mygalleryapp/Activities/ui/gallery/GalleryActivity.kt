package com.example.mygalleryapp.ui.gallery

import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mygalleryapp.Adapters.MixedAdapter
import com.example.mygalleryapp.databinding.ActivityGalleryBinding
import com.example.mygalleryapp.ml.FaceDetectorHelper
import com.example.mygalleryapp.ml.FaceEmbeddingHelper
import com.example.mygalleryapp.ui.albums.AlbumActivity
import com.example.mygalleryapp.ui.photoview.PhotoViewActivity
import kotlinx.coroutines.*
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer
import kotlin.math.sqrt

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val photoList = mutableListOf<Uri>()
    private val faceDataList = mutableListOf<FaceData>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var flashbackList = listOf<FlashbackMemory>()
    private val faceDetector = FaceDetectorHelper()
    private lateinit var faceEmbeddingHelper: FaceEmbeddingHelper
    private var clusters = listOf<FaceCluster>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val modelBytes = assets.open("facenet.tflite").use { it.readBytes() }
        val byteBuffer = ByteBuffer.allocateDirect(modelBytes.size).put(modelBytes).apply { rewind() }
        faceEmbeddingHelper = FaceEmbeddingHelper(byteBuffer)

        binding.rvClusters.layoutManager = LinearLayoutManager(this)

        loadImages()
        binding.buttonAlbum.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            // Pass the clusters data using Parcelable
            intent.putParcelableArrayListExtra("clusters", ArrayList(clusters))
            startActivity(intent)
        }
        binding.btnAllPhotos.setOnClickListener { loadImages() }
        binding.btnAlbums.setOnClickListener { if (clusters.isNotEmpty()) showClusters() }
    }

    private fun loadImages() {
        coroutineScope.launch {
            val images = withContext(Dispatchers.IO) { getCameraPhotosOnly() }

            photoList.clear()
            photoList.addAll(images)

            // -------------- NEW: Generate flashbacks -------------
            flashbackList = withContext(Dispatchers.IO) {
                generateFlashbackMemories(images)
            }

            processImagesInBatches(images, batchSize = 10)
        }
    }

    private fun processImagesInBatches(images: List<Uri>, batchSize: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            faceDataList.clear()
            for (i in images.indices step batchSize) {
                val batch = images.subList(i, minOf(i + batchSize, images.size))
                val batchResults = batch.mapNotNull { uri ->
                    try {
                        val bitmap = loadBitmapFromUri(uri, maxSize = 300)
                        val faceRects = faceDetector.detectFacesSync(bitmap)
                        if (faceRects.isNotEmpty()) {
                            faceRects.map { rect ->
                                val croppedFace = cropFaceFromBitmap(bitmap, rect)
                                val embedding = faceEmbeddingHelper.getEmbedding(croppedFace)
                                FaceData(uri, embedding)
                            }
                        } else null
                    } catch (e: Exception) { null }
                }.flatten()
                synchronized(faceDataList) { faceDataList.addAll(batchResults) }

                withContext(Dispatchers.Main) { showClusters() }
            }
        }
    }

    private fun showClusters() {
        clusters = clusterFaces(faceDataList)

        val finalList = mutableListOf<Any>()

        // Add Flashback Memories FIRST
        if (flashbackList.isNotEmpty()) {
            finalList.addAll(flashbackList)
        }

        // Add Person clusters
        finalList.addAll(clusters)

        binding.rvClusters.adapter = MixedAdapter(
            finalList,
            onClusterPhotoClick = { uri ->
                val intent = Intent(this, PhotoViewActivity::class.java)
                intent.putExtra("photo_uri", uri.toString())
                startActivity(intent)
            }
        )
    }

    private fun cropFaceFromBitmap(bitmap: Bitmap, rect: Rect) =
        Bitmap.createBitmap(
            bitmap,
            rect.left.coerceAtLeast(0),
            rect.top.coerceAtLeast(0),
            rect.width().coerceAtMost(bitmap.width - rect.left),
            rect.height().coerceAtMost(bitmap.height - rect.top)
        )

    private fun loadBitmapFromUri(uri: Uri, maxSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }

        var scale = 1
        while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) scale *= 2

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        return contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, decodeOptions) }!!
    }

    // MODIFIED: Now filters for Camera photos only
    private fun getCameraPhotosOnly(): List<Uri> {
        val list = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketColumn) ?: ""

                // Filter for Camera folder only
                if (bucketName.equals("Camera", ignoreCase = true) ||
                    bucketName.equals("DCIM", ignoreCase = true)) {
                    val id = cursor.getLong(idColumn)
                    list.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
                }
            }
        }
        return list
    }

    // ----------------- HIGH-ACCURACY CLUSTER LOGIC -----------------
    private fun clusterFaces(
        faceDataList: List<FaceData>,
        threshold: Float = 0.46f // slightly higher to handle different environments
    ): List<FaceCluster> {

        val clusters = mutableListOf<FaceCluster>()
        val assigned = BooleanArray(faceDataList.size)

        var clusterId = 0
        var personId = 1

        // -----------------------------
        // STEP 1: GROUP PHOTOS (2+ faces)
        // -----------------------------
        val groupClusterFaces = mutableListOf<FaceData>()
        val groupedByPhoto = faceDataList.groupBy { it.photoUri }

        groupedByPhoto.forEach { (_, facesInSamePhoto) ->
            if (facesInSamePhoto.size > 1) {
                facesInSamePhoto.forEach { face ->
                    val index = faceDataList.indexOf(face)
                    assigned[index] = true
                    groupClusterFaces.add(face)
                }
            }
        }

        // Add single Group Photos cluster
        if (groupClusterFaces.isNotEmpty()) {
            clusters.add(
                FaceCluster(
                    clusterId++,
                    groupClusterFaces,
                    "Group Photos"
                )
            )
        }

        // -----------------------------
        // STEP 2: SINGLE-PERSON GREEDY CLUSTERING
        // -----------------------------
        for (i in faceDataList.indices) {

            if (assigned[i]) continue

            val baseFace = faceDataList[i]
            val personCluster = mutableListOf(baseFace)
            assigned[i] = true

            for (j in i + 1 until faceDataList.size) {
                if (assigned[j]) continue

                val distance = cosineDistance(baseFace.embedding, faceDataList[j].embedding)
                if (distance < threshold) {
                    personCluster.add(faceDataList[j])
                    assigned[j] = true
                }
            }

            clusters.add(
                FaceCluster(
                    clusterId++,
                    personCluster,
                    "Person $personId"
                )
            )

            personId++
        }

        // -----------------------------
        // STEP 3: MERGE SIMILAR PERSON CLUSTERS
        // -----------------------------
        val mergedClusters = mutableListOf<FaceCluster>()
        val mergedAssigned = BooleanArray(clusters.size)

        for (i in clusters.indices) {
            if (mergedAssigned[i]) continue
            val baseCluster = clusters[i].faces.toMutableList()
            mergedAssigned[i] = true

            for (j in i + 1 until clusters.size) {
                if (mergedAssigned[j]) continue
                if (clusters[i].name == "Group Photos" || clusters[j].name == "Group Photos") continue

                // Average distance between clusters
                val avgDistance = clusters[i].faces.flatMap { f1 ->
                    clusters[j].faces.map { f2 ->
                        cosineDistance(f1.embedding, f2.embedding)
                    }
                }.average()

                if (avgDistance < threshold) {
                    baseCluster.addAll(clusters[j].faces)
                    mergedAssigned[j] = true
                }
            }

            val clusterName = if (clusters[i].name == "Group Photos") "Group Photos" else "Person ${mergedClusters.size + 1}"
            mergedClusters.add(FaceCluster(mergedClusters.size, baseCluster, clusterName))
        }

        return mergedClusters
    }

    private fun getPhotoDate(uri: Uri): Long {
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                if (cursor.moveToFirst()) {
                    return cursor.getLong(idx)
                }
            }
        } catch (_: Exception) {}

        return 0L
    }

    private fun generateFlashbackMemories(uris: List<Uri>): List<FlashbackMemory> {
        val memories = mutableListOf<FlashbackMemory>()
        val today = java.util.Calendar.getInstance()
        val currentMonth = today.get(java.util.Calendar.MONTH)
        val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)

        val photosByYear = mutableMapOf<Int, MutableList<FlashbackPhoto>>()

        uris.forEach { uri ->
            val dateTaken = getPhotoDate(uri)
            if (dateTaken == 0L) return@forEach

            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = dateTaken

            val m = cal.get(java.util.Calendar.MONTH)
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)

            if (m == currentMonth && d == currentDay) {
                val year = cal.get(java.util.Calendar.YEAR)
                photosByYear.getOrPut(year) { mutableListOf() }
                    .add(FlashbackPhoto(uri, dateTaken))
            }
        }

        // Create memory objects
        photosByYear.toSortedMap().forEach { (year, list) ->
            memories.add(
                FlashbackMemory(
                    title = "On This Day - $year",
                    photos = list
                )
            )
        }

        // DEBUG: Log to check if flashbacks were found
        android.util.Log.d("FlashbackDebug", "Total flashback memories found: ${memories.size}")
        memories.forEach { memory ->
            android.util.Log.d("FlashbackDebug", "${memory.title}: ${memory.photos.size} photos")
        }

        return memories
    }

    private fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return 1 - (dot / (sqrt(normA) * sqrt(normB)))
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    // Made data classes Parcelable using @Parcelize
    @Parcelize
    data class FaceData(val photoUri: Uri, val embedding: FloatArray) : Parcelable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FaceData
            if (photoUri != other.photoUri) return false
            if (!embedding.contentEquals(other.embedding)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = photoUri.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    @Parcelize
    data class FaceCluster(val id: Int, val faces: List<FaceData>, val name: String) : Parcelable

    data class FlashbackMemory(val title: String, val photos: List<FlashbackPhoto>)
    data class FlashbackPhoto(val uri: Uri, val dateTaken: Long)
}