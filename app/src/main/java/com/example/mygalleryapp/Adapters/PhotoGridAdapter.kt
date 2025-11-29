package com.example.mygalleryapp.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mygalleryapp.databinding.ItemPhotoGridBinding

class PhotoGridAdapter(
    private val photos: List<Uri>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val binding: ItemPhotoGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun getItemCount() = photos.size

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUri = photos[position]

        holder.binding.imgPhoto.load(photoUri) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
        }

        holder.binding.root.setOnClickListener {
            onClick(photoUri)
        }
    }
}