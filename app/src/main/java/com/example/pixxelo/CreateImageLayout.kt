package com.example.pixxelo

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class CreateImageLayout : ComponentActivity() {

    private lateinit var original: Bitmap
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_image_screen)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val uriString = intent.getStringExtra("imageUri")
        val uri = Uri.parse(uriString)
        original = loadBitmapFromUri(uri)
        original = Bitmap.createScaledBitmap(original, 800, 800, true)
        imageView.setImageBitmap(original)
        currentBitmap = original

        val btn16 = findViewById<Button>(R.id.btn16)
        val btn32 = findViewById<Button>(R.id.btn32)
        val btn64 = findViewById<Button>(R.id.btn64)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btn16.setOnClickListener {
            lifecycleScope.launch {
                val bmp16 = withContext(Dispatchers.Default) { pixelArt(original, 64, 16) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp16)
                    currentBitmap = bmp16
                }
            }
        }
        btn32.setOnClickListener {
            lifecycleScope.launch {
                val bmp32 = withContext(Dispatchers.Default) { pixelArt(original, 64, 32) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp32)
                    currentBitmap = bmp32
                }
            }
        }

        btn64.setOnClickListener {
            lifecycleScope.launch {
                val bmp64 = withContext(Dispatchers.Default) { pixelArt(original, 64, 64) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp64)
                    currentBitmap = bmp64
                }
            }
        }
        btnSave.setOnClickListener {
            currentBitmap?.let { bmp ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        saveToGallery(bmp)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreateImageLayout, "Сохранено в галерею", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreateImageLayout, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    fun toGrayscale(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = src.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                bmpGray.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return bmpGray
    }
    fun quantizeGray(src: Bitmap, levels: Int): Bitmap {
        val width = src.width
        val height = src.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val step = 256 / levels
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = src.getPixel(x, y)
                val gray = Color.red(pixel)
                val quantized = (gray / step) * step
                result.setPixel(x, y, Color.rgb(quantized, quantized, quantized))
            }
        }
        return result
    }
    private fun saveToGallery(bitmap: Bitmap) {
        val filename = "pixxelo_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pixxelo")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Не удалось создать запись в MediaStore")

        contentResolver.openOutputStream(uri)?.use { out ->
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!ok) throw IOException("Сжатие не удалось")
        } ?: throw IOException("OutputStream = null")
    }
    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                .copy(Bitmap.Config.ARGB_8888, true)
        }
    }
    fun pixelArt(src: Bitmap, targetSize: Int, levels: Int): Bitmap {
        val small = Bitmap.createScaledBitmap(src, targetSize, targetSize, false)
        val quantized = quantizeGray(toGrayscale(small), levels)
        return Bitmap.createScaledBitmap(quantized, src.width, src.height, false)
    }
}