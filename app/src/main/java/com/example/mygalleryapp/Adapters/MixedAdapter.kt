package com.example.mygalleryapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygalleryapp.databinding.ClusterItemBinding
import com.example.mygalleryapp.databinding.FlashbackItemBinding
import com.example.mygalleryapp.ui.gallery.GalleryActivity

class MixedAdapter(
    private val items: List<Any>,
    private val onClusterPhotoClick: (android.net.Uri) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_FLASHBACK = 0
    private val TYPE_CLUSTER = 1

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is GalleryActivity.FlashbackMemory) TYPE_FLASHBACK else TYPE_CLUSTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FLASHBACK) {
            val binding = FlashbackItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            FlashbackViewHolder(binding)
        } else {
            val binding = ClusterItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ClusterViewHolder(binding)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if (holder is FlashbackViewHolder && item is GalleryActivity.FlashbackMemory) {
            holder.bind(item)
        }
        if (holder is ClusterViewHolder && item is GalleryActivity.FaceCluster) {
            holder.bind(item)
        }
    }

    inner class FlashbackViewHolder(val binding: FlashbackItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(memory: GalleryActivity.FlashbackMemory) {
            binding.flashbackTitle.text = memory.title

            binding.flashbackPhotosRecycler.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)

            binding.flashbackPhotosRecycler.adapter = FlashbackAdapter(memory.photos, onClusterPhotoClick)
        }
    }

    inner class ClusterViewHolder(val binding: ClusterItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(cluster: GalleryActivity.FaceCluster) {
            binding.tvClusterLabel.text = cluster.name

            binding.rvFaces.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)

            binding.rvFaces.adapter = FaceAdapter(cluster.faces.map { it.photoUri }, onClusterPhotoClick)
        }
    }
}
