package com.example.exerciseapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ml.PoseUtils
import com.example.exerciseapp.ui.components.CameraPreviewView
import com.example.exerciseapp.ui.components.FlipCameraButton // Import your FlipCameraButton
import com.example.exerciseapp.ui.components.PoseOverlay
import com.example.exerciseapp.ui.theme.ExerciseAppTheme
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

data class PoseUiState(
    val pose: Pose? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val jointAngles: Map<String, Double?> = emptyMap()
)

class MainActivity : ComponentActivity() {

    private val poseAnalyzer = PoseAnalyzer()
    private val smoothedAnglesMap = mutableMapOf<String, Double>()
    private val SMOOTHING_FACTOR = 0.3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No need for enableEdgeToEdge() here if it's not being used for edge-to-edge layout
        // You can add it back if you want that specific window behavior.

        requestCameraPermission()

        setContent {
            ExerciseAppTheme {
                PoseDetectionScreen()
            }
        }
    }

    @Composable
    private fun PoseDetectionScreen() {
        var poseUiState by remember { mutableStateOf(PoseUiState()) }
        var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview and pose overlay
            CameraPreviewView(
                lensFacing = lensFacing, // This lensFacing is now properly observed by LaunchedEffect
                onImageAnalyzed = { imageProxy ->
                    poseAnalyzer.detectPose(imageProxy) { pose ->
                        val newRawAngles = mutableMapOf<String, Double?>()

                        val jointsToTrack = mapOf(
                            "Left Elbow" to Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
                            "Right Elbow" to Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
                            "Left Shoulder" to Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
                            "Right Shoulder" to Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
                            "Left Knee" to Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
                            "Right Knee" to Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
                            "Left Hip" to Triple(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER),
                            "Right Hip" to Triple(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER)
                        )

                        jointsToTrack.forEach { (jointName, landmarks) ->
                            newRawAngles[jointName] = PoseUtils.calculateAngle(
                                pose,
                                landmarks.first,
                                landmarks.second,
                                landmarks.third
                            )
                        }

                        val smoothedAngles = newRawAngles.mapValues { (jointName, rawAngle) ->
                            if (rawAngle != null) {
                                val currentSmoothed = smoothedAnglesMap[jointName]
                                if (currentSmoothed == null) {
                                    smoothedAnglesMap[jointName] = rawAngle
                                    rawAngle
                                } else {
                                    val smoothed = SMOOTHING_FACTOR * rawAngle + (1 - SMOOTHING_FACTOR) * currentSmoothed
                                    smoothedAnglesMap[jointName] = smoothed
                                    smoothed
                                }
                            } else null
                        }

                        poseUiState = PoseUiState(
                            pose = pose,
                            imageWidth = imageProxy.width,
                            imageHeight = imageProxy.height,
                            jointAngles = smoothedAngles
                        )
                    }
                }
            )

            PoseOverlay(
                pose = poseUiState.pose,
                imageWidth = poseUiState.imageWidth,
                imageHeight = poseUiState.imageHeight,
                lensFacing = lensFacing,
                jointAngles = poseUiState.jointAngles,
                modifier = Modifier.fillMaxSize()
            )

            // Use your dedicated FlipCameraButton Composable
            FlipCameraButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        CameraSelector.LENS_FACING_BACK
                    else
                        CameraSelector.LENS_FACING_FRONT
                }
            )
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }
}


//package com.example.exerciseapp
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.camera.core.CameraSelector
//import androidx.compose.foundation.layout.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.exerciseapp.ml.PoseAnalyzer
//import com.example.exerciseapp.ml.PoseUtils
//import com.example.exerciseapp.ui.components.CameraPreviewView
//import com.example.exerciseapp.ui.components.FlipCameraButton
//import com.example.exerciseapp.ui.components.PoseOverlay
//import com.example.exerciseapp.ui.theme.ExerciseAppTheme
//import com.google.mlkit.vision.pose.Pose
//import com.google.mlkit.vision.pose.PoseLandmark
//import android.util.Log // Import for logging
//
//// Data class to hold all UI state related to pose detection
//data class PoseUiState(
//    val pose: Pose? = null,
//    val imageWidth: Int = 0,
//    val imageHeight: Int = 0,
//    val imageRotation: Int = 0, // Rotation of the image from the camera
//    val jointAngles: Map<String, Double?> = emptyMap()
//)
//
//class MainActivity : ComponentActivity() {
//
//    private val poseAnalyzer = PoseAnalyzer()
//    private val smoothedAnglesMap = mutableMapOf<String, Double>()
//    private val SMOOTHING_FACTOR = 0.3 // For angle smoothing
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        requestCameraPermission() // Request camera permission on startup
//
//        setContent {
//            ExerciseAppTheme {
//                PoseDetectionScreen()
//            }
//        }
//    }
//
//    @Composable
//    private fun PoseDetectionScreen() {
//        // State to hold pose detection results and camera info
//        var poseUiState by remember { mutableStateOf(PoseUiState()) }
//        // State to control front/back camera selection
//        var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Camera preview with image analysis for pose detection
//            CameraPreviewView(
//                lensFacing = lensFacing,
//                onImageAnalyzed = { imageProxy ->
//                    // Get the rotation of the image frame from CameraX
//                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//                    Log.d("MainActivity", "ImageProxy received: ${imageProxy.width}x${imageProxy.height}, Rotation: $rotationDegrees")
//
//                    // Perform pose detection using the PoseAnalyzer
//                    poseAnalyzer.detectPose(imageProxy) { pose ->
//                        val newRawAngles = mutableMapOf<String, Double?>()
//
//                        // Define the joints for which to calculate angles
//                        val jointsToTrack = mapOf(
//                            "Left Elbow" to Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
//                            "Right Elbow" to Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
//                            "Left Shoulder" to Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
//                            "Right Shoulder" to Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
//                            "Left Knee" to Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
//                            "Right Knee" to Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
//                            "Left Hip" to Triple(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER),
//                            "Right Hip" to Triple(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER)
//                        )
//
//                        // Calculate angles for defined joints
//                        jointsToTrack.forEach { (jointName, landmarks) ->
//                            newRawAngles[jointName] = PoseUtils.calculateAngle(
//                                pose,
//                                landmarks.first,
//                                landmarks.second,
//                                landmarks.third
//                            )
//                        }
//
//                        // Apply smoothing to the calculated angles
//                        val smoothedAngles = newRawAngles.mapValues { (jointName, rawAngle) ->
//                            if (rawAngle != null) {
//                                val currentSmoothed = smoothedAnglesMap[jointName]
//                                if (currentSmoothed == null) {
//                                    smoothedAnglesMap[jointName] = rawAngle
//                                    rawAngle
//                                } else {
//                                    val smoothed = SMOOTHING_FACTOR * rawAngle + (1 - SMOOTHING_FACTOR) * currentSmoothed
//                                    smoothedAnglesMap[jointName] = smoothed
//                                    smoothed
//                                }
//                            } else null
//                        }
//
//                        // Update the UI state with new pose data, image dimensions, rotation, and angles
//                        poseUiState = PoseUiState(
//                            pose = pose,
//                            imageWidth = imageProxy.width,
//                            imageHeight = imageProxy.height,
//                            imageRotation = rotationDegrees, // Pass the rotation to the overlay
//                            jointAngles = smoothedAngles
//                        )
//                    }
//                    imageProxy.close() // IMPORTANT: Close the ImageProxy when done processing to release the buffer
//                }
//            )
//
//            // PoseOverlay to draw the skeleton and joint angles on top of the camera preview
//            PoseOverlay(
//                pose = poseUiState.pose,
//                imageWidth = poseUiState.imageWidth,
//                imageHeight = poseUiState.imageHeight,
//                imageRotation = poseUiState.imageRotation, // Pass the image rotation
//                lensFacing = lensFacing, // Pass the current camera lens facing
//                jointAngles = poseUiState.jointAngles,
//                modifier = Modifier.fillMaxSize()
//            )
//
//            // Button to flip between front and back cameras
//            FlipCameraButton(
//                onClick = {
//                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
//                        CameraSelector.LENS_FACING_BACK
//                    else
//                        CameraSelector.LENS_FACING_FRONT
//                },
//                modifier = Modifier
//                    .align(Alignment.BottomEnd) // Position at the bottom-right
//                    .padding(16.dp) // Add padding for aesthetics
//            )
//        }
//    }
//
//    // Function to request camera permission
//    private fun requestCameraPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.CAMERA),
//                100 // Request code
//            )
//        }
//    }
//}