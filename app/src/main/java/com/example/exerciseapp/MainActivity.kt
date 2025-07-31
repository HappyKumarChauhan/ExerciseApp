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
//import com.example.exerciseapp.ui.components.FlipCameraButton // Import your FlipCameraButton
//import com.example.exerciseapp.ui.components.PoseOverlay
//import com.example.exerciseapp.ui.theme.ExerciseAppTheme
//import com.google.mlkit.vision.pose.Pose
//import com.google.mlkit.vision.pose.PoseLandmark
//
//data class PoseUiState(
//    val pose: Pose? = null,
//    val imageWidth: Int = 0,
//    val imageHeight: Int = 0,
//    val jointAngles: Map<String, Double?> = emptyMap()
//)
//
//class MainActivity : ComponentActivity() {
//
//    private val poseAnalyzer = PoseAnalyzer()
//    private val smoothedAnglesMap = mutableMapOf<String, Double>()
//    private val SMOOTHING_FACTOR = 0.3
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // No need for enableEdgeToEdge() here if it's not being used for edge-to-edge layout
//        // You can add it back if you want that specific window behavior.
//
//        requestCameraPermission()
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
//        var poseUiState by remember { mutableStateOf(PoseUiState()) }
//        var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Camera preview and pose overlay
//            CameraPreviewView(
//                lensFacing = lensFacing, // This lensFacing is now properly observed by LaunchedEffect
//                onImageAnalyzed = { imageProxy ->
//                    poseAnalyzer.detectPose(imageProxy) { pose ->
//                        val newRawAngles = mutableMapOf<String, Double?>()
//
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
//                        jointsToTrack.forEach { (jointName, landmarks) ->
//                            newRawAngles[jointName] = PoseUtils.calculateAngle(
//                                pose,
//                                landmarks.first,
//                                landmarks.second,
//                                landmarks.third
//                            )
//                        }
//
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
//                        poseUiState = PoseUiState(
//                            pose = pose,
//                            imageWidth = imageProxy.width,
//                            imageHeight = imageProxy.height,
//                            jointAngles = smoothedAngles
//                        )
//                    }
//                }
//            )
//
//            PoseOverlay(
//                pose = poseUiState.pose,
//                imageWidth = poseUiState.imageWidth,
//                imageHeight = poseUiState.imageHeight,
//                lensFacing = lensFacing,
//                jointAngles = poseUiState.jointAngles,
//                modifier = Modifier.fillMaxSize()
//            )
//
//            // Use your dedicated FlipCameraButton Composable
//            FlipCameraButton(
//                onClick = {
//                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
//                        CameraSelector.LENS_FACING_BACK
//                    else
//                        CameraSelector.LENS_FACING_FRONT
//                }
//            )
//        }
//    }
//
//    private fun requestCameraPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.CAMERA),
//                100
//            )
//        }
//    }
//}
//



package com.example.exerciseapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ml.PoseUtils
import com.example.exerciseapp.ui.components.CameraPreviewView
import com.example.exerciseapp.ui.components.FlipCameraButton
import com.example.exerciseapp.ui.components.PoseOverlay
import com.example.exerciseapp.ui.theme.ExerciseAppTheme
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

// REMOVED: The enum class BoundaryStatus definition was here.
// It should only exist in its own file, BoundaryStatus.kt.

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

    private val poseAnalyzer = PoseAnalyzer()
    private val smoothedAnglesMap = mutableMapOf<String, Double>()
    private val SMOOTHING_FACTOR = 0.3
    private val TAG = "PoseDetectionDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        var hasAchievedGreenZone by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                lensFacing = lensFacing,
                onImageAnalyzed = { imageProxy ->
                    poseAnalyzer.detectPose(imageProxy) { pose ->
                        val currentImageWidth = imageProxy.width
                        val currentImageHeight = imageProxy.height

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

                        var currentBoundaryStatus = BoundaryStatus.RED

                        if (pose != null) {
                            fun areAllLandmarksVisible(landmarksToCheck: List<Int>): Boolean {
                                return landmarksToCheck.all { landmarkType ->
                                    pose.getPoseLandmark(landmarkType)?.let {
                                        it.inFrameLikelihood > 0.8f
                                    } ?: false
                                }
                            }

                            val initialRequiredLandmarks = listOf(
                                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
                            )

                            val trackingRequiredLandmarks = listOf(
                                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE
                            )

                            if (hasAchievedGreenZone) {
                                if (areAllLandmarksVisible(trackingRequiredLandmarks)) {
                                    currentBoundaryStatus = BoundaryStatus.GREEN
                                    Log.d(TAG, "GREEN: Tracking maintained (hips & knees visible).")
                                } else {
                                    currentBoundaryStatus = BoundaryStatus.RED
                                    hasAchievedGreenZone = false
                                    Log.d(TAG, "LOST GREEN: Hips or knees are no longer visible.")
                                }
                            } else {
                                if (areAllLandmarksVisible(initialRequiredLandmarks)) {
                                    currentBoundaryStatus = BoundaryStatus.GREEN
                                    hasAchievedGreenZone = true
                                    Log.d(TAG, "ACHIEVED GREEN: All initial joints are visible.")
                                } else {
                                    currentBoundaryStatus = BoundaryStatus.RED
                                    Log.d(TAG, "Still RED: Waiting for user to be fully in frame.")
                                }
                            }

                        } else {
                            currentBoundaryStatus = BoundaryStatus.RED
                            hasAchievedGreenZone = false
                            Log.d(TAG, "Still RED: Pose object is null.")
                        }

                        poseUiState = PoseUiState(
                            pose = pose,
                            imageWidth = currentImageWidth,
                            imageHeight = currentImageHeight,
                            jointAngles = smoothedAngles,
                            boundaryStatus = currentBoundaryStatus,
                            personBoundingBox = null
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
                boundaryStatus = poseUiState.boundaryStatus,
                modifier = Modifier.fillMaxSize()
            )

            FlipCameraButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        CameraSelector.LENS_FACING_BACK
                    else
                        CameraSelector.LENS_FACING_FRONT
                    hasAchievedGreenZone = false
                    smoothedAnglesMap.clear()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
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