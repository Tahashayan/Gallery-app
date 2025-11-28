package com.example.mygalleryapp.ui.photoview

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mygalleryapp.databinding.ActivityPhotoViewBinding

class PhotoViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra("photo_uri")
        val uri = Uri.parse(uriString)
        binding.imgPhoto.setImageURI(uri)
    }
}
