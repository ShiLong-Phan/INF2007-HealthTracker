package com.inf2007.healthtracker.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.core.graphics.scale
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

class ImageUtils(private val context: Context) {

    /**
     * Convert a URI to a Bitmap optimized for vision AI
     * Handles orientation issues with gallery images and optimizes quality
     */
    fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)

        // Use BitmapFactory options to get image size first without loading full bitmap
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // Calculate appropriate sample size to avoid OOM
        val sampleSize = calculateSampleSize(options, 1024, 1024)

        // Load bitmap with sample size
        val secondStream = context.contentResolver.openInputStream(uri)
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888 // Higher quality
        }
        val originalBitmap = BitmapFactory.decodeStream(secondStream, null, loadOptions)
            ?: throw IOException("Failed to decode bitmap")
        secondStream?.close()

        // Get orientation information if available
        val rotatedBitmap = getRotatedBitmap(uri, originalBitmap)

        // Optimize bitmap for vision models
        return optimizeForVisionAI(rotatedBitmap)
    }

    /**
     * Calculate appropriate sample size for loading large images
     */
    private fun calculateSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Fixes image rotation issues from gallery images
     */
    private fun getRotatedBitmap(uri: Uri, bitmap: Bitmap): Bitmap {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val stream = context.contentResolver.openInputStream(uri)
                    stream?.let { ExifInterface(it) }
                } else {
                    val path = uri.path
                    if (path != null) {
                        try {
                            ExifInterface(path)
                        } catch (e: IOException) {
                            null
                        }
                    } else null
                }

                exif?.let {
                    val orientation = it.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )

                    return when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                        else -> bitmap
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("ImageUtils", "Error getting image orientation", e)
        }

        return bitmap
    }

    /**
     * Rotates an image by the specified degrees
     */
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Optimize bitmap specifically for vision AI models like Gemini
     * - Proper resizing for model input
     * - Enhanced contrast and color for better recognition
     */
    fun optimizeForVisionAI(bitmap: Bitmap): Bitmap {
        // First resize if needed (target 1024px max dimension while maintaining aspect ratio)
        val resizedBitmap = resizeBitmapForVisionAI(bitmap)

        // Create a new bitmap to apply enhancements
        val enhancedBitmap = Bitmap.createBitmap(resizedBitmap.width, resizedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)

        // Apply slight enhancement to contrast and saturation for food image quality
        val paint = Paint()
        val colorMatrix = ColorMatrix()

        // Increase contrast slightly (1.1f)
        colorMatrix.set(floatArrayOf(
            1.1f, 0f, 0f, 0f, 0f,
            0f, 1.1f, 0f, 0f, 0f,
            0f, 0f, 1.1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        // Create a temporary saturation matrix
        val saturationMatrix = ColorMatrix()
        // Increase saturation slightly (1.2f)
        saturationMatrix.setSaturation(1.2f)

        // Apply the saturation after the contrast adjustment
        colorMatrix.postConcat(saturationMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(resizedBitmap, 0f, 0f, paint)

        // If we created a new bitmap, recycle the original to free memory
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return enhancedBitmap
    }

    /**
     * Resize bitmap to appropriate dimensions for vision AI models
     * Target resolution should be high enough for details but not excessively large
     */
    private fun resizeBitmapForVisionAI(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Best practice for Gemini Vision models: aim for 1024px on the longest side
        val maxDimension = 1024

        // Check if resize is needed
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / aspectRatio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Process image specifically for food recognition
     * - Enhanced for better food detail visibility
     */
    fun processForFoodRecognition(bitmap: Bitmap): Bitmap {
        return optimizeForVisionAI(bitmap)
    }
}