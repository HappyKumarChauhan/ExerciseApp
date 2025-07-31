package com.example.exerciseapp.ui.components

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
// Ensure these imports are present
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    // The onImageAnalyzed lambda is now responsible for closing the ImageProxy
    onImageAnalyzed: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use a background executor for ImageAnalysis to prevent UI jank.
    val analysisExecutor: Executor = remember { Executors.newSingleThreadExecutor() }
    // The cameraProviderFuture.addListener still needs the main executor for UI operations
    val cameraSetupExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        // Keep FIT_CENTER as per your preference for now, since this is your working base.
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // You can keep targetResolution for the camera's internal processing (e.g., for ImageAnalysis).
            // The PreviewView's scaleType will handle how it's displayed on screen.
            val targetResolutionForPreview = Size(1280, 720) // For visual preview
            val targetResolutionForAnalysis = Size(1280, 720) // For image processing (can be different)

            val preview = Preview.Builder()
                .setTargetResolution(targetResolutionForPreview)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetResolutionForAnalysis)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    // Use the background analysisExecutor here
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        Log.d("CameraPreview", "Image Width: ${imageProxy.width}, Height: ${imageProxy.height}, Rotation: ${imageProxy.imageInfo.rotationDegrees}")

                        // Pass the ImageProxy to your skeleton tracking logic.
                        // IMPORTANT: Your `onImageAnalyzed` function MUST call `imageProxy.close()`
                        // when it is done processing the image data.
                        // If you do not close it, you will have memory leaks and performance issues.
                        onImageAnalyzed(imageProxy)

                        // If your onImageAnalyzed function is synchronous and consumes the ImageProxy fully,
                        // you can uncomment the line below.
                        // However, it's safer to let the consumer (your skeleton tracking logic) handle closing.
                        // imageProxy.close() // DO NOT UNCOMMENT THIS HERE IF ONIMAGEANALYZED IS ASYNCHRONOUS OR PASSES IT ON!
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera binding failed", e)
            }

        }, cameraSetupExecutor) // Use the main executor for camera setup
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            previewView
        }
    )
}
