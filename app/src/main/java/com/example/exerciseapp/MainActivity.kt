// app/src/main/java/com/example/exerciseapp/MainActivity.kt
package com.example.exerciseapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ui.navigation.AppNavGraph
import com.example.exerciseapp.ui.theme.ExerciseAppTheme
import com.google.mlkit.vision.pose.Pose

// Data class to hold all UI-related pose information
data class PoseUiState(
    val pose: Pose? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val jointAngles: Map<String, Double?> = emptyMap(),
    val boundaryStatus: BoundaryStatus = BoundaryStatus.RED,
    val personBoundingBox: Rect? = null
)

class MainActivity : ComponentActivity() {

    // PoseAnalyzer and smoothedAnglesMap should be managed by ViewModels for exercise screens.
    // For now, keep them here as a centralized resource that can be passed down.
    // Or, better, create a common ViewModel that can be used by all camera-dependent screens.
    // For this initial setup, we'll keep them here and pass them down where needed,
    // but note that a ViewModel for camera/pose data would be a cleaner architecture.
    val poseAnalyzer = PoseAnalyzer()
    val smoothedAnglesMap = mutableMapOf<String, Double>() // This needs to be managed per exercise/calibration session

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "Camera permission granted")
            // You might want to re-render the UI or navigate based on permission status
        } else {
            Log.e("Permissions", "Camera permission denied")
            // Show a rationale or disable camera-related features
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission() // Request permission on app start

        setContent {
            ExerciseAppTheme {
                // The entire app's navigation starts here
                AppNavGraph()
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}