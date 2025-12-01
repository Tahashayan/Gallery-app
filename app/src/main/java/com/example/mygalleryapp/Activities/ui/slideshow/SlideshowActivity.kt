package com.example.mygalleryapp.ui.slideshow

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.mygalleryapp.R
import com.example.mygalleryapp.databinding.ActivitySlideshowBinding
import coil.load

class SlideshowActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideshowBinding
    private var photoUris: List<Uri> = emptyList()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = true
    private val slideDuration = 3000L

    private val slideRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && photoUris.isNotEmpty()) {
                showNextImage()
                handler.postDelayed(this, slideDuration)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlideshowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        val photosArray = intent.getStringArrayExtra("photos")
        val albumName = intent.getStringExtra("album_name") ?: "Slideshow"

        if (photosArray != null) {
            photoUris = photosArray.map { Uri.parse(it) }
        }

        if (photoUris.isEmpty()) {
            finish()
            return
        }

        binding.tvAlbumName.text = albumName
        binding.tvPhotoCount.text = "1 / ${photoUris.size}"

        // Show first image
        showImage(0)

        // Controls
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnPrevious.setOnClickListener { showPreviousImage() }
        binding.btnNext.setOnClickListener { showNextImage() }
        binding.btnClose.setOnClickListener { finish() }
        binding.imgSlideshow.setOnClickListener { toggleControlsVisibility() }

        startBackgroundMusic()

        // START SLIDESHOW ONLY ONE TIME
        handler.postDelayed(slideRunnable, slideDuration)
    }

    private fun showImage(index: Int) {
        currentIndex = index.coerceIn(0, photoUris.size - 1)

        // Slide animation (like video movement)
        binding.imgSlideshow.apply {
            alpha = 0f
            translationX = 200f   // slide from right

            animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(600)
                .start()
        }

        // Load image with crossfade
        binding.imgSlideshow.load(photoUris[currentIndex]) {
            crossfade(500)
            listener(onSuccess = { _, _ ->
                binding.tvPhotoCount.text = "${currentIndex + 1} / ${photoUris.size}"
            })
        }
    }


    private fun showNextImage() {
        currentIndex = (currentIndex + 1) % photoUris.size
        showImage(currentIndex)
    }

    private fun showPreviousImage() {
        currentIndex = if (currentIndex == 0) photoUris.size - 1 else currentIndex - 1
        showImage(currentIndex)
    }

    private fun togglePlayPause() {
        isPlaying = !isPlaying

        if (isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            handler.removeCallbacks(slideRunnable)
            handler.postDelayed(slideRunnable, slideDuration)
            mediaPlayer?.start()
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            handler.removeCallbacks(slideRunnable)
            mediaPlayer?.pause()
        }
    }

    private fun toggleControlsVisibility() {
        val visible = binding.controlsLayout.visibility == View.VISIBLE
        binding.controlsLayout.visibility = if (visible) View.GONE else View.VISIBLE
        binding.topBar.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun startBackgroundMusic() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.slideshow_music)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.3f, 0.3f)
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(slideRunnable)
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            handler.removeCallbacks(slideRunnable)
            handler.postDelayed(slideRunnable, slideDuration)
            mediaPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(slideRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
