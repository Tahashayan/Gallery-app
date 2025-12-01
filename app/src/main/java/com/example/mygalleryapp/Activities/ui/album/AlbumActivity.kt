package com.example.mygalleryapp.ui.albums

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mygalleryapp.Adapters.AlbumAdapter
import com.example.mygalleryapp.databinding.ActivityAlbumBinding
import com.example.mygalleryapp.ui.gallery.GalleryActivity
import com.example.mygalleryapp.ui.photoview.PhotoViewActivity
import com.example.mygalleryapp.ui.slideshow.SlideshowActivity

class AlbumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumBinding
    private var clusters: List<GalleryActivity.FaceCluster> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get clusters passed from GalleryActivity using Parcelable
        clusters = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("clusters", GalleryActivity.FaceCluster::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("clusters") ?: emptyList()
        }

        if (clusters.isEmpty()) {
            Toast.makeText(this, "No face albums available yet", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.rvAlbums.layoutManager = LinearLayoutManager(this)

        // When album card is clicked, show dialog to choose between Gallery or Slideshow
        binding.rvAlbums.adapter = AlbumAdapter(clusters) { cluster ->
            showAlbumOptionsDialog(cluster)
        }
    }

    private fun showAlbumOptionsDialog(cluster: GalleryActivity.FaceCluster) {
        val options = arrayOf("View Photos", "Start Slideshow")

        AlertDialog.Builder(this)
            .setTitle(cluster.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openAlbumGallery(cluster)  // View Photos
                    1 -> startAlbumSlideshow(cluster)  // Start Slideshow
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun openAlbumGallery(cluster: GalleryActivity.FaceCluster) {
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("album_name", cluster.name)
        intent.putExtra("photos", cluster.faces.map { it.photoUri.toString() }.toTypedArray())
        startActivity(intent)
    }

    private fun startAlbumSlideshow(cluster: GalleryActivity.FaceCluster) {
        val photos = cluster.faces.map { it.photoUri.toString() }

        if (photos.isEmpty()) {
            Toast.makeText(this, "No photos available for slideshow", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, SlideshowActivity::class.java)
        intent.putExtra("photos", photos.toTypedArray())
        intent.putExtra("album_name", cluster.name)
        startActivity(intent)
    }
}