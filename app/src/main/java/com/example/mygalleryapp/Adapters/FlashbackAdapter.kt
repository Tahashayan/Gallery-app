package com.example.mygalleryapp.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mygalleryapp.databinding.FlashbackPhotoItemBinding

class FlashbackAdapter(
    private val photos: List<com.example.mygalleryapp.ui.gallery.GalleryActivity.FlashbackPhoto>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<FlashbackAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: FlashbackPhotoItemBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FlashbackPhotoItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = photos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = photos[position]

        holder.binding.flashbackPhoto.load(photo.uri)
        holder.binding.flashbackPhotoDate.text =
            java.text.SimpleDateFormat("dd MMM yyyy")
                .format(java.util.Date(photo.dateTaken))

        holder.binding.flashbackPhoto.setOnClickListener {
            onClick(photo.uri)
        }
    }
}
