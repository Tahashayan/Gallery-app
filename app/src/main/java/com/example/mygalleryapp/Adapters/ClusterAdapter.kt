package com.example.mygalleryapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygalleryapp.databinding.ClusterItemBinding
import com.example.mygalleryapp.ui.gallery.GalleryActivity

class ClusterAdapter(
    private val clusters: List<GalleryActivity.FaceCluster>,
    private val onClick: (android.net.Uri) -> Unit
) : RecyclerView.Adapter<ClusterAdapter.ClusterViewHolder>() {

    class ClusterViewHolder(val binding: ClusterItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClusterViewHolder {
        val binding = ClusterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClusterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClusterViewHolder, position: Int) {
        val cluster = clusters[position]
        holder.binding.tvClusterLabel.text = cluster.name

        holder.binding.rvFaces.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.binding.rvFaces.adapter = FaceAdapter(cluster.faces.map { it.photoUri }, onClick)
    }

    override fun getItemCount() = clusters.size
}
