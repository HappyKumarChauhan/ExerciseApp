// File: app/src/main/java/com/example/exerciseapp/util/CameraUtils.kt
package com.example.exerciseapp.util

import android.util.Size
import androidx.camera.view.PreviewView
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

/**
 * Transforms a coordinate from the source image's coordinate system to the preview view's coordinate system.
 * This is essential for drawing overlays like skeleton points on a camera preview, ensuring proper alignment.
 *
 * @param sourceSize The size of the source image (e.g., from ImageProxy).
 * @param destinationSize The size of the destination view (e.g., the PreviewView).
 * @param scaleType The scale type used by the PreviewView.
 * @param x The x-coordinate from the source image.
 * @param y The y-coordinate from the source image.
 * @return A Pair of transformed coordinates (x, y) suitable for drawing on the destination view.
 */
fun transformCoordinates(
    sourceSize: Size,
    destinationSize: IntSize,
    scaleType: PreviewView.ScaleType,
    x: Float,
    y: Float
): Pair<Float, Float> {
    val previewWidth = destinationSize.width.toFloat()
    val previewHeight = destinationSize.height.toFloat()
    val imageWidth = sourceSize.width.toFloat()
    val imageHeight = sourceSize.height.toFloat()

    val scaleX = previewWidth / imageWidth
    val scaleY = previewHeight / imageHeight

    val transformedX: Float
    val transformedY: Float

    when (scaleType) {
        PreviewView.ScaleType.FIT_CENTER -> {
            val scale = min(scaleX, scaleY)
            val offsetX = (previewWidth - imageWidth * scale) / 2
            val offsetY = (previewHeight - imageHeight * scale) / 2
            transformedX = x * scale + offsetX
            transformedY = y * scale + offsetY
        }
        PreviewView.ScaleType.FILL_CENTER -> {
            val scale = max(scaleX, scaleY)
            val offsetX = (previewWidth - imageWidth * scale) / 2
            val offsetY = (previewHeight - imageHeight * scale) / 2
            transformedX = x * scale + offsetX
            transformedY = y * scale + offsetY
        }
        else -> {
            // Default to a basic proportional scale for other types
            val scale = max(scaleX, scaleY)
            transformedX = x * scale
            transformedY = y * scale
        }
    }
    return Pair(transformedX, transformedY)
}