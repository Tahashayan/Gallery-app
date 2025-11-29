package com.example.mygalleryapp.ui.albums

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mygalleryapp.Adapters.AlbumAdapter
import com.example.mygalleryapp.databinding.ActivityAlbumBinding
import com.example.mygalleryapp.ui.gallery.GalleryActivity
import com.example.mygalleryapp.ui.photoview.PhotoViewActivity

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

        binding.rvAlbums.adapter = AlbumAdapter(clusters) { cluster ->
            openAlbum(cluster)
        }
    }

    private fun openAlbum(cluster: GalleryActivity.FaceCluster) {
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("album_name", cluster.name)
        intent.putExtra("photos", cluster.faces.map { it.photoUri.toString() }.toTypedArray())
        startActivity(intent)
    }
}