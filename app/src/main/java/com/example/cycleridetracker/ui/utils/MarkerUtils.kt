package com.example.cycleridetracker.ui.utils

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult

object MarkerUtils {
    fun createPhotoMarker(source: Bitmap): Bitmap {
        val size = 160 // Slightly larger for better detail
        val tailHeight = 20
        val tailWidth = 24
        val borderRadius = 20f
        val borderSize = 5f
        
        // Colors from the user's mockup (approximate)
        val borderColor = 0xFF42A5F5.toInt() // Vibrant blue
        val frameBgColor = 0xFF212121.toInt() // Dark gray
        
        Log.d("MarkerUtils", "Creating photo marker. Source size: ${source.width}x${source.height}")
        
        val result = Bitmap.createBitmap(size, size + tailHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 1. Draw the blue border and tail as a single shape
        paint.color = borderColor
        val outerRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(outerRect, borderRadius, borderRadius, paint)
        
        val path = Path()
        path.moveTo(size / 2f - tailWidth / 2f, size - borderRadius)
        path.lineTo(size / 2f + tailWidth / 2f, size - borderRadius)
        path.lineTo(size / 2f, (size + tailHeight).toFloat())
        path.close()
        canvas.drawPath(path, paint)

        // 2. Draw the dark background for the image
        paint.color = frameBgColor
        val innerRect = RectF(borderSize, borderSize, size - borderSize, size - borderSize)
        canvas.drawRoundRect(innerRect, borderRadius - borderSize, borderRadius - borderSize, paint)

        // 3. Draw the image with proper center-crop
        try {
            canvas.save()
            val clipPath = Path()
            val innerRadius = borderRadius - borderSize
            clipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            
            val matrix = Matrix()
            val targetSize = size - 2 * borderSize
            
            // Calculate scale to fill the square (center-crop)
            val scale = Math.max(targetSize / source.width.toFloat(), targetSize / source.height.toFloat())
            val dx = (targetSize - source.width * scale) / 2f
            val dy = (targetSize - source.height * scale) / 2f
            
            Log.d("MarkerUtils", "Drawing bitmap: ${source.width}x${source.height}, scale=$scale, dx=$dx, dy=$dy")
            
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx + borderSize, dy + borderSize)
            
            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            imagePaint.isFilterBitmap = true // Better quality
            canvas.drawBitmap(source, matrix, imagePaint)
            canvas.restore()
        } catch (e: Exception) {
            Log.e("MarkerUtils", "Error drawing image onto marker", e)
        }
        
        return result
    }

    suspend fun loadMarkerBitmap(context: Context, uri: String): Bitmap? {
        Log.d("MarkerUtils", "Loading marker bitmap for URI: $uri")
        return try {
            val imageLoader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(240, 240) // Request larger for better quality after crop
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                Log.d("MarkerUtils", "Successfully loaded bitmap for $uri")
                val bitmap = (result.drawable as BitmapDrawable).bitmap
                createPhotoMarker(bitmap)
            } else {
                Log.e("MarkerUtils", "Failed to load bitmap for $uri: ${result}")
                null
            }
        } catch (e: Exception) {
            Log.e("MarkerUtils", "Exception loading marker bitmap for $uri", e)
            null
        }
    }
}
