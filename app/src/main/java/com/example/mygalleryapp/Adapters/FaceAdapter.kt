package com.example.mygalleryapp.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.example.mygalleryapp.R
import com.example.mygalleryapp.databinding.FaceItemBinding

class FaceAdapter(
    private val faces: List<Uri>,
    private val onClick: (Uri) -> Unit
) : RecyclerView.Adapter<FaceAdapter.FaceViewHolder>() {

    class FaceViewHolder(val binding: FaceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceViewHolder {
        val binding = FaceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaceViewHolder, position: Int) {
        val uri = faces[position]

        holder.binding.faceImageView.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder)
            error(R.drawable.ic_error)
            size(
                holder.binding.faceImageView.width.takeIf { it > 0 } ?: 120,
                holder.binding.faceImageView.height.takeIf { it > 0 } ?: 120
            )
            scale(Scale.FILL)
        }

        holder.binding.faceImageView.setOnClickListener { onClick(uri) }
    }

    override fun getItemCount() = faces.size
}
