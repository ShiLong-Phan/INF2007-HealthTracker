package com.inf2007.healthtracker.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.graphics.scale
import java.io.IOException
import java.io.InputStream

class ImageUtils(private val context: Context) {

    /**
     * Convert a URI to a Bitmap
     * Handles orientation issues with gallery images
     */
    fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Get orientation information if available
        val rotatedBitmap = getRotatedBitmap(uri, originalBitmap)

        // Resize if the image is too large (to prevent OOM errors and improve performance)
        return resizeBitmapIfNeeded(rotatedBitmap)
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
     * Resizes bitmap if it's too large
     * Prevents OOM errors and improves performance with large images
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = 1024 // Maximum width or height
        val width = bitmap.width
        val height = bitmap.height

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

        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * Process image specifically for food recognition
     * - Enhances contrast and saturation
     * - Centers food in frame where possible
     */
    fun processForFoodRecognition(bitmap: Bitmap): Bitmap {
        // This would contain more advanced processing if needed
        // For now, just resize if needed to ensure it's not too large
        return resizeBitmapIfNeeded(bitmap)
    }
}