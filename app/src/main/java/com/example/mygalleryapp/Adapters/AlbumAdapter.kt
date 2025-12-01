package com.example.mygalleryapp.Adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mygalleryapp.R
import com.example.mygalleryapp.ui.gallery.GalleryActivity
import com.flaviofaria.kenburnsview.KenBurnsView

class AlbumAdapter(
    private val clusters: List<GalleryActivity.FaceCluster>,
    private val onItemClick: (GalleryActivity.FaceCluster) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private val handlers = mutableMapOf<Int, Handler>()
    private val runnables = mutableMapOf<Int, Runnable>()
    private val imageIndices = mutableMapOf<Int, Int>()

    companion object {
        private const val IMAGE_ROTATION_INTERVAL = 4000L // 4 seconds
        private const val CROSSFADE_DURATION = 1000L // 1 second crossfade
    }

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Find views manually using findViewById
        private val tvAlbumName: TextView = itemView.findViewById(R.id.tvAlbumName)
        private val imgAlbumCover: KenBurnsView = itemView.findViewById(R.id.imgAlbumCover)
        private val imgAlbumCoverOverlay: KenBurnsView = itemView.findViewById(R.id.imgAlbumCoverOverlay)

        private var isShowingPrimary = true

        fun bind(cluster: GalleryActivity.FaceCluster, position: Int) {
            tvAlbumName.text = "${cluster.name} (${cluster.faces.size})"

            // Stop any existing rotation for this position
            stopRotation(position)

            // Reset state
            isShowingPrimary = true
            imgAlbumCover.visibility = View.VISIBLE
            imgAlbumCover.alpha = 1f
            imgAlbumCoverOverlay.visibility = View.GONE
            imgAlbumCoverOverlay.alpha = 0f

            // Start Ken Burns on primary view
            imgAlbumCover.resume()

            if (cluster.faces.isNotEmpty()) {
                // Initialize with first image
                val currentIndex = imageIndices.getOrPut(position) { 0 }
                loadImageIntoKenBurns(imgAlbumCover, cluster.faces[currentIndex].photoUri.toString())

                // Start rotation if multiple images
                if (cluster.faces.size > 1) {
                    startImageRotation(cluster, position)
                }
            }

            itemView.setOnClickListener {
                onItemClick(cluster)
            }
        }

        private fun loadImageIntoKenBurns(view: KenBurnsView, uri: String) {
            Glide.with(view.context)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(view)
        }

        private fun startImageRotation(cluster: GalleryActivity.FaceCluster, position: Int) {
            val handler = Handler(Looper.getMainLooper())
            handlers[position] = handler

            val runnable = object : Runnable {
                override fun run() {
                    if (!itemView.isAttachedToWindow) return

                    // Calculate next image index
                    val currentIndex = imageIndices[position] ?: 0
                    val nextIndex = (currentIndex + 1) % cluster.faces.size
                    imageIndices[position] = nextIndex

                    val nextImageUri = cluster.faces[nextIndex].photoUri.toString()

                    // Perform crossfade between two KenBurnsViews
                    crossfadeToNextImage(nextImageUri)

                    handler.postDelayed(this, IMAGE_ROTATION_INTERVAL)
                }
            }

            runnables[position] = runnable
            handler.postDelayed(runnable, IMAGE_ROTATION_INTERVAL)
        }

        private fun crossfadeToNextImage(nextImageUri: String) {
            if (isShowingPrimary) {
                // Load next image into overlay and fade it in
                imgAlbumCoverOverlay.visibility = View.VISIBLE
                imgAlbumCoverOverlay.alpha = 0f
                loadImageIntoKenBurns(imgAlbumCoverOverlay, nextImageUri)
                imgAlbumCoverOverlay.resume() // Start Ken Burns on overlay

                // Fade in overlay
                ObjectAnimator.ofFloat(imgAlbumCoverOverlay, "alpha", 0f, 1f).apply {
                    duration = CROSSFADE_DURATION
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // After fade complete, overlay is now primary
                            imgAlbumCover.pause()
                            isShowingPrimary = false
                        }
                    })
                    start()
                }

                // Fade out primary
                ObjectAnimator.ofFloat(imgAlbumCover, "alpha", 1f, 0f).apply {
                    duration = CROSSFADE_DURATION
                    start()
                }
            } else {
                // Load next image into primary and fade it in
                imgAlbumCover.visibility = View.VISIBLE
                imgAlbumCover.alpha = 0f
                loadImageIntoKenBurns(imgAlbumCover, nextImageUri)
                imgAlbumCover.resume() // Start Ken Burns on primary

                // Fade in primary
                ObjectAnimator.ofFloat(imgAlbumCover, "alpha", 0f, 1f).apply {
                    duration = CROSSFADE_DURATION
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // After fade complete, primary is now showing
                            imgAlbumCoverOverlay.pause()
                            isShowingPrimary = true
                        }
                    })
                    start()
                }

                // Fade out overlay
                ObjectAnimator.ofFloat(imgAlbumCoverOverlay, "alpha", 1f, 0f).apply {
                    duration = CROSSFADE_DURATION
                    start()
                }
            }
        }

        fun stopRotation(position: Int) {
            handlers[position]?.let { handler ->
                runnables[position]?.let { runnable ->
                    handler.removeCallbacks(runnable)
                }
            }
            handlers.remove(position)
            runnables.remove(position)
        }

        fun pauseKenBurns() {
            imgAlbumCover.pause()
            imgAlbumCoverOverlay.pause()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        // Change R.layout.item_album to your actual XML file name
        val view = LayoutInflater.from(parent.context).inflate(R.layout.album_card_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(clusters[position], position)
    }

    override fun getItemCount(): Int = clusters.size

    override fun onViewRecycled(holder: AlbumViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            holder.stopRotation(position)
        }
        holder.pauseKenBurns()
    }

    fun stopAllRotations() {
        handlers.forEach { (position, handler) ->
            runnables[position]?.let { runnable ->
                handler.removeCallbacks(runnable)
            }
        }
        handlers.clear()
        runnables.clear()
    }
}