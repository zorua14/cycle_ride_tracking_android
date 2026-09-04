package com.example.cycleridetracker.ui.utils

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.content.Context
import android.util.Log
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MarkerUtils {
    suspend fun createPhotoMarker(source: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val size = 160 // Slightly larger for better detail
        val tailHeight = 20
        val tailWidth = 24
        val borderRadius = 20f
        val borderSize = 5f
        
        // Colors from the user's mockup (approximate)
        val borderColor = 0xFF42A5F5.toInt() // Vibrant blue
        val frameBgColor = 0xFF212121.toInt() // Dark gray
        
        Log.d("MarkerUtils", "Creating photo marker. Source size: ${source.width}x${source.height}")
        
        val result = createBitmap(size, size + tailHeight, Bitmap.Config.ARGB_8888)
        result.applyCanvas {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // 1. Draw the blue border and tail as a single shape
            paint.color = borderColor
            val outerRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
            drawRoundRect(outerRect, borderRadius, borderRadius, paint)
            
            val path = Path()
            path.moveTo((size / 2f) - (tailWidth / 2f), size - borderRadius)
            path.lineTo((size / 2f) + (tailWidth / 2f), size - borderRadius)
            path.lineTo(size / 2f, (size + tailHeight).toFloat())
            path.close()
            drawPath(path, paint)

            // 2. Draw the dark background for the image
            paint.color = frameBgColor
            val innerRect = RectF(borderSize, borderSize, size - borderSize, size - borderSize)
            drawRoundRect(innerRect, borderRadius - borderSize, borderRadius - borderSize, paint)

            // 3. Draw the image with proper center-crop
            try {
                withSave {
                    val clipPath = Path()
                    val innerRadius = borderRadius - borderSize
                    clipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
                    clipPath(clipPath)
                    
                    val matrix = Matrix()
                    val targetSize = size - 2 * borderSize
                    
                    // Calculate scale to fill the square (center-crop)
                    val scale = maxOf(targetSize / source.width.toFloat(), targetSize / source.height.toFloat())
                    val dx = (targetSize - source.width * scale) / 2f
                    val dy = (targetSize - source.height * scale) / 2f
                    
                    Log.d("MarkerUtils", "Drawing bitmap: ${source.width}x${source.height}, scale=$scale, dx=$dx, dy=$dy")
                    
                    matrix.setScale(scale, scale)
                    matrix.postTranslate(dx + borderSize, dy + borderSize)
                    
                    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    imagePaint.isFilterBitmap = true // Better quality
                    drawBitmap(source, matrix, imagePaint)
                }
            } catch (e: Exception) {
                Log.e("MarkerUtils", "Error drawing image onto marker", e)
            }
        }
        
        result
    }

    fun getBikeMarkerBitmap(): Bitmap {
        val size = 100
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Background Circle (Cyan 400 from theme)
            paint.color = 0xFF81D4FA.toInt()
            drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
            
            // White border
            paint.style = Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            drawCircle(size / 2f, size / 2f, size / 2.5f, paint)
            
            // Improved Bicycle Icon (Navy 700 from theme for contrast)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = 0xFF1E293B.toInt()
            
            // Wheels
            drawCircle(size * 0.32f, size * 0.62f, size * 0.12f, paint) // Rear wheel
            drawCircle(size * 0.68f, size * 0.62f, size * 0.12f, paint) // Front wheel
            
            // Frame
            val path = Path()
            path.moveTo(size * 0.32f, size * 0.62f) // Rear hub
            path.lineTo(size * 0.45f, size * 0.48f) // Seat post bottom
            path.lineTo(size * 0.65f, size * 0.48f) // Top tube front
            path.lineTo(size * 0.5f, size * 0.62f) // Bottom bracket
            path.lineTo(size * 0.32f, size * 0.62f) // Back to rear hub
            
            path.moveTo(size * 0.45f, size * 0.48f)
            path.lineTo(size * 0.5f, size * 0.62f) // Seat tube
            
            path.moveTo(size * 0.65f, size * 0.48f)
            path.lineTo(size * 0.68f, size * 0.62f) // Fork
            
            // Handlebars
            path.moveTo(size * 0.65f, size * 0.48f)
            path.lineTo(size * 0.68f, size * 0.42f)
            path.lineTo(size * 0.62f, size * 0.42f)
            
            // Seat
            path.moveTo(size * 0.45f, size * 0.48f)
            path.lineTo(size * 0.45f, size * 0.42f)
            path.lineTo(size * 0.40f, size * 0.42f)
            path.lineTo(size * 0.50f, size * 0.42f)
            
            drawPath(path, paint)
        }
        
        return bitmap
    }

    suspend fun loadMarkerBitmap(context: Context, uri: String): Bitmap? {
        Log.d("MarkerUtils", "Loading marker bitmap for URI: $uri")
        return try {
            val imageLoader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(width = 240, height = 240) // Request larger for better quality after crop
                .allowHardware(enable = false)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                Log.d("MarkerUtils", "Successfully loaded bitmap for $uri")
                val bitmap = (result.drawable as BitmapDrawable).bitmap
                createPhotoMarker(bitmap)
            } else {
                Log.e("MarkerUtils", "Failed to load bitmap for $uri: $result")
                null
            }
        } catch (e: Exception) {
            Log.e("MarkerUtils", "Exception loading marker bitmap for $uri", e)
            null
        }
    }
}
