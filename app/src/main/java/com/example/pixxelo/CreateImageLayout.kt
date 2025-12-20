package com.example.pixxelo

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
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
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, "Нет изображения для загрузки", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val uri = Uri.parse(uriString)
        original = loadBitmapFromUri(uri)
        val targetHeight = 800
        val aspectRatio = original.width.toFloat() / original.height.toFloat()
        val targetWidth = (targetHeight * aspectRatio).toInt()
        original = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        imageView.setImageBitmap(original)
        currentBitmap = original

        val btn16 = findViewById<ImageButton>(R.id.btn16)
        val btn32 = findViewById<ImageButton>(R.id.btn32)
        val btn64 = findViewById<ImageButton>(R.id.btn64)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btn16.setOnClickListener {
            lifecycleScope.launch {
                val bmp16 = withContext(Dispatchers.Default) { pixelArt(original, 64, 16) }
                val watermark = BitmapFactory.decodeResource(resources, R.drawable.icon_app_pixxelo_screen)
                val withWatermark = withContext(Dispatchers.Default) { addWatermarkImage(bmp16, watermark) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(withWatermark)
                    currentBitmap = withWatermark
                }
            }
        }
        btn32.setOnClickListener {
            lifecycleScope.launch {
                val bmp32 = withContext(Dispatchers.Default) { pixelArt(original, 64, 32) }
                val watermark = BitmapFactory.decodeResource(resources, R.drawable.icon_app_pixxelo_screen)
                val withWatermark = withContext(Dispatchers.Default) { addWatermarkImage(bmp32, watermark) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(withWatermark)
                    currentBitmap = withWatermark
                }
            }
        }
        btn64.setOnClickListener {
            lifecycleScope.launch {
                val bmp64 = withContext(Dispatchers.Default) { pixelArt(original, 64, 64) }
                val watermark = BitmapFactory.decodeResource(resources, R.drawable.icon_app_pixxelo_screen)
                val withWatermark = withContext(Dispatchers.Default) { addWatermarkImage(bmp64, watermark) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(withWatermark)
                    currentBitmap = withWatermark
                }
            }
        }
        val btnHalftoneFigure = findViewById<ImageButton>(R.id.button_dot_pic)
        btnHalftoneFigure.setOnClickListener {
            lifecycleScope.launch {
                val halftone = withContext(Dispatchers.Default) { halftoneDotArt(original, 12) }
                val watermark = BitmapFactory.decodeResource(resources, R.drawable.icon_app_pixxelo_screen)
                val withWatermark = withContext(Dispatchers.Default) { addWatermarkImage(halftone, watermark) }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(withWatermark)
                    currentBitmap = withWatermark
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
    fun halftoneDotArt(src: Bitmap, blockSize: Int): Bitmap {
            val width = src.width
            val height = src.height
            val gray = toGrayscale(src)
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
        canvas.drawColor(Color.BLACK)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
            val corners = listOf(
            gray.getPixel(0, 0),
            gray.getPixel(width - 1, 0),
            gray.getPixel(0, height - 1),
            gray.getPixel(width - 1, height - 1)
        )
        val avgR = corners.map { Color.red(it) }.average().toInt()
        val backgroundColor = avgR
        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                var sumGray = 0
                var count = 0
                var whiteCount = 0
                var hasPureWhite = false

                for (dy in 0 until blockSize) {
                    for (dx in 0 until blockSize) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            val pixelGray = gray.getPixel(px, py)
                            val brightness = Color.red(pixelGray)
                            sumGray += brightness
                            count++
                            if (brightness > 250) hasPureWhite = true
                            if (brightness > 220) whiteCount++
                        }
                    }
                }
                val avgGray = sumGray / count
                val diff = Math.abs(avgGray - backgroundColor)

                if (diff < 25) {
                    continue
                } else {
                    if (hasPureWhite || avgGray > 230 || whiteCount.toFloat() / count > 0.3f) {
                        val shade = 230
                        paint.color = Color.rgb(shade, shade, shade)
                        val radius = (blockSize / 2.5).toFloat()
                        canvas.drawCircle(
                            x + blockSize / 2f,
                            y + blockSize / 2f,
                            radius,
                            paint
                        )
                    } else {
                        val shade = avgGray
                        paint.color = Color.rgb(shade, shade, shade)
                        val radius = ((255 - avgGray) / 255.0 * blockSize / 2.8).toFloat()
                        if (radius > 1f) {
                            canvas.drawCircle(
                                x + blockSize / 2f,
                                y + blockSize / 2f,
                                radius,
                                paint
                            )
                        }
                    }
                }
            }
        }

        return result
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

    fun addWatermarkImage(src: Bitmap, watermark: Bitmap): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)

        val margin = 20
        val wmWidth = src.width / 8
        val aspectRatio = watermark.width.toFloat() / watermark.height.toFloat()
        val wmHeight = (wmWidth / aspectRatio).toInt()
        val scaledWatermark = Bitmap.createScaledBitmap(watermark, wmWidth, wmHeight, true)

        val left = src.width - wmWidth - margin
        val top = src.height - wmHeight - margin

        canvas.drawBitmap(scaledWatermark, left.toFloat(), top.toFloat(), null)
        return result
    }
}