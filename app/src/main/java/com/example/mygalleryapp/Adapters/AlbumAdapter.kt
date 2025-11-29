package com.example.mygalleryapp.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mygalleryapp.databinding.AlbumCardItemBinding
import com.example.mygalleryapp.ui.gallery.GalleryActivity

class AlbumAdapter(
    private val clusters: List<GalleryActivity.FaceCluster>,
    private val onClick: (GalleryActivity.FaceCluster) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AlbumCardItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlbumCardItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = clusters.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cluster = clusters[position]

        holder.binding.tvAlbumName.text = cluster.name

        val sampleUri: Uri? = cluster.faces.firstOrNull()?.photoUri

        sampleUri?.let {
            holder.binding.imgAlbumCover.load(it)
        }

        holder.binding.root.setOnClickListener {
            onClick(cluster)
        }
    }
}
