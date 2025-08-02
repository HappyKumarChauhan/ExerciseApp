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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier, // <--- Added the modifier parameter here
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    onImageAnalyzed: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use a background executor for ImageAnalysis to prevent UI jank.
    val analysisExecutor: Executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSetupExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context) }

    // Use DisposableEffect to manage the lifecycle of the analysis executor
    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraPreviewView", "Shutting down analysis executor.")
            analysisExecutor.asCloser().close()
        }
    }

    // LaunchedEffect reacts to changes in lensFacing and sets up the camera
    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

            // Set resolutions for both the visual preview and the image analysis
            val targetResolution = Size(1280, 720)

            val preview = Preview.Builder()
                .setTargetResolution(targetResolution)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        Log.d("CameraPreview", "Image Width: ${imageProxy.width}, Height: ${imageProxy.height}")
                        onImageAnalyzed(imageProxy)
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
        }, cameraSetupExecutor)
    }

    AndroidView(
        // The modifier is now passed from the parent composable
        modifier = modifier,
        factory = {
            previewView
        }
    )
}

// Extension function to help with closing the executor
private fun Executor.asCloser() = object : AutoCloseable {
    override fun close() {
        if (this@asCloser is java.util.concurrent.ExecutorService) {
            this@asCloser.shutdown()
        }
    }
}