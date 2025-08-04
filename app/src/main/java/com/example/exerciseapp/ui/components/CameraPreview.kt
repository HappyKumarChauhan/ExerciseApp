// File: app/src/main/java/com/example/exerciseapp/ui/components/CameraPreview.kt
package com.example.exerciseapp.ui.components

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    onImageAnalyzed: (ImageProxy) -> Unit,
    // NEW: Callback to provide the PreviewView's dimensions and scale type to the parent.
    onPreviewViewSizeChanged: (IntSize, PreviewView.ScaleType) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analysisExecutor: Executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSetupExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraPreviewView", "Shutting down analysis executor.")
            (analysisExecutor as? ExecutorService)?.shutdown()
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // CRITICAL CHANGE 1: Use FILL_CENTER for full-screen camera feed.
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

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
        // CRITICAL CHANGE 2: onSizeChanged is used to get the dimensions of the PreviewView.
        modifier = modifier.onSizeChanged { size ->
            onPreviewViewSizeChanged(size, previewView.scaleType)
        },
        factory = {
            previewView
        }
    )
}