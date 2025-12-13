package com.example.pixxelo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pixxelo.ui.theme.PixxeloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        val btnOpenGallery = findViewById<Button>(R.id.create_image_button)
        btnOpenGallery.setOnClickListener {
            pickImage.launch("image/*")
        }
    }
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Передаём выбранное изображение на экран редактирования
            val intent = Intent(this, CreateImageLayout::class.java).apply {
                putExtra("imageUri", it.toString())
            }
            startActivity(intent)
        }
    }
}