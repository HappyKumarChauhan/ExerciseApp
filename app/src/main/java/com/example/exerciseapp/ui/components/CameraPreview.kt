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

@Composable
fun CameraPreviewView(
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    onImageAnalyzed: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val targetResolution = Size(1280, 720) // 16:9 HD resolution (good for pose detection)

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
                    it.setAnalyzer(executor) { imageProxy ->
                        Log.d("CameraPreview", "Image Width: ${imageProxy.width}, Height: ${imageProxy.height}, Rotation: ${imageProxy.imageInfo.rotationDegrees}")
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

        }, executor)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            previewView
        }
    )
}


//package com.example.exerciseapp.ui.components
//
//import android.util.Log
//import androidx.camera.core.AspectRatio
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import java.util.concurrent.Executor
//
//@Composable
//fun CameraPreviewView(
//    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
//    onImageAnalyzed: (ImageProxy) -> Unit
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }
//
//    val previewView = remember { PreviewView(context) }
//
//    LaunchedEffect(Unit) {
//        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
//    }
//
//    LaunchedEffect(lensFacing) {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val targetAspectRatio = AspectRatio.RATIO_16_9 // Consistent aspect ratio
//
//            val preview = Preview.Builder()
//                .setTargetAspectRatio(targetAspectRatio)
//                .build()
//                .also {
//                    it.setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//            val imageAnalysis = ImageAnalysis.Builder()
//                .setTargetAspectRatio(targetAspectRatio)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(executor) { imageProxy ->
//                        // *******************************************************************
//                        // DEBUG LOG: Add this line here
//                        Log.d("CameraPreview", "Image Width: ${imageProxy.width}, Height: ${imageProxy.height}, Rotation: ${imageProxy.imageInfo.rotationDegrees}")
//                        // *******************************************************************
//                        onImageAnalyzed(imageProxy)
//                    }
//                }
//
//            val cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(lensFacing)
//                .build()
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    lifecycleOwner,
//                    cameraSelector,
//                    preview,
//                    imageAnalysis
//                )
//            } catch (e: Exception) {
//                Log.e("CameraPreview", "Camera binding failed", e)
//            }
//
//        }, executor)
//    }
//
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = {
//            previewView
//        }
//    )
//}