package com.example.mygalleryapp.ui.photoview

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mygalleryapp.Adapters.PhotoGridAdapter
import com.example.mygalleryapp.databinding.ActivityPhotoViewBinding

class PhotoViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if we're showing a single photo or an album
        val singlePhotoUri = intent.getStringExtra("photo_uri")
        val albumName = intent.getStringExtra("album_name")
        val photosArray = intent.getStringArrayExtra("photos")

        if (singlePhotoUri != null) {
            // Single photo view (original behavior)
            val uri = Uri.parse(singlePhotoUri)
            binding.imgPhoto.setImageURI(uri)

            // Hide RecyclerView if it exists in layout
            binding.rvPhotos?.let { it.visibility = android.view.View.GONE }
            binding.imgPhoto.visibility = android.view.View.VISIBLE
        } else if (photosArray != null && albumName != null) {
            // Album view (multiple photos in grid)
            supportActionBar?.title = albumName

            // Hide single ImageView
            binding.imgPhoto.visibility = android.view.View.GONE

            // Show RecyclerView
            binding.rvPhotos?.let { recyclerView ->
                recyclerView.visibility = android.view.View.VISIBLE
                recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns

                val photoUris = photosArray.map { Uri.parse(it) }
                recyclerView.adapter = PhotoGridAdapter(photoUris) { uri ->
                    // Handle photo click - open full screen view
                    openFullScreenPhoto(uri)
                }
            }
        }
    }

    private fun openFullScreenPhoto(uri: Uri) {
        // Open a new activity or dialog to show full screen photo
        val intent = android.content.Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("photo_uri", uri.toString())
        startActivity(intent)
    }
}