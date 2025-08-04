// app/src/main/java/com/example/exerciseapp/ui/screens/FreeMovementTrackingScreen.kt
package com.example.exerciseapp.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.exerciseapp.BoundaryStatus
import com.example.exerciseapp.PoseUiState
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ml.PoseUtils
import com.example.exerciseapp.ui.components.CameraPreviewView
import com.example.exerciseapp.ui.components.FlipCameraButton
import com.example.exerciseapp.ui.components.PoseOverlay
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.min

// (FreeMovementViewModel code remains unchanged)
class FreeMovementViewModel : androidx.lifecycle.ViewModel() {
    private val poseAnalyzer = PoseAnalyzer()
    private val smoothedAnglesMap = mutableMapOf<String, Double>()
    private val SMOOTHING_FACTOR = 0.3f
    private val TAG = "FreeMovementViewModel"

    private val CRITICAL_INITIAL_JOINTS = listOf(
        PoseLandmark.NOSE,
        PoseLandmark.LEFT_EYE, PoseLandmark.RIGHT_EYE,
        PoseLandmark.LEFT_EAR, PoseLandmark.RIGHT_EAR,
        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.LEFT_HEEL, PoseLandmark.RIGHT_HEEL,
        PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.RIGHT_FOOT_INDEX
    )

    private val CRITICAL_LOWER_JOINTS = listOf(
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
    )

    private val MIN_INITIAL_FULL_BODY_LIKELIHOOD = 0.85f
    private val MIN_ONGOING_LOWER_BODY_LIKELIHOOD = 0.2f

    var poseUiState by mutableStateOf(PoseUiState())
        private set

    var hasAchievedGreenZone by mutableStateOf(false)

    private val jointsToTrack = mapOf(
        "Left Elbow" to Triple(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
        "Right Elbow" to Triple(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
        "Left Shoulder" to Triple(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
        "Right Shoulder" to Triple(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
        "Left Knee" to Triple(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
        "Right Knee" to Triple(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
        "Left Hip" to Triple(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER),
        "Right Hip" to Triple(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER)
    )

    private val newRawAngles = mutableMapOf<String, Double?>()

    fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        poseAnalyzer.detectPose(imageProxy) { pose ->
            val currentImageWidth = imageProxy.width
            val currentImageHeight = imageProxy.height

            newRawAngles.clear()

            var currentBoundaryStatus = BoundaryStatus.RED
            var personDisplayBoundingBox: androidx.compose.ui.geometry.Rect? = null
            val currentJointAngles = mutableMapOf<String, Double?>()

            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
                var minXRaw = Float.MAX_VALUE
                var minYRaw = Float.MAX_VALUE
                var maxXRaw = Float.MIN_VALUE
                var maxYRaw = Float.MIN_VALUE

                val MIN_LIKELIHOOD_FOR_BOUNDING_BOX = 0.5f
                var anyLandmarkDetectedForBox = false

                for (landmark in pose.allPoseLandmarks) {
                    if (landmark.inFrameLikelihood >= MIN_LIKELIHOOD_FOR_BOUNDING_BOX) {
                        minXRaw = minOf(minXRaw, landmark.position.x)
                        minYRaw = minOf(minYRaw, landmark.position.y)
                        maxXRaw = maxOf(maxXRaw, landmark.position.x)
                        maxYRaw = maxOf(maxYRaw, landmark.position.y)
                        anyLandmarkDetectedForBox = true
                    }
                }

                if (!hasAchievedGreenZone) {
                    var allInitialJointsConfident = true
                    for (landmarkType in CRITICAL_INITIAL_JOINTS) {
                        val landmark = pose.getPoseLandmark(landmarkType)
                        if (landmark == null || landmark.inFrameLikelihood < MIN_INITIAL_FULL_BODY_LIKELIHOOD) {
                            allInitialJointsConfident = false
                            break
                        }
                    }

                    if (allInitialJointsConfident) {
                        currentBoundaryStatus = BoundaryStatus.GREEN
                        hasAchievedGreenZone = true
                        Log.d(TAG, "ACHIEVED GREEN: All initial joints confident.")
                    } else {
                        currentBoundaryStatus = BoundaryStatus.RED
                        Log.d(TAG, "Still RED (Initial): Not all joints confident.")
                    }
                } else {
                    currentBoundaryStatus = BoundaryStatus.GREEN

                    var lowerBodyLost = false
                    for (landmarkType in CRITICAL_LOWER_JOINTS) {
                        val landmark = pose.getPoseLandmark(landmarkType)
                        if (landmark == null || landmark.inFrameLikelihood < MIN_ONGOING_LOWER_BODY_LIKELIHOOD) {
                            lowerBodyLost = true
                            break
                        }
                    }

                    if (lowerBodyLost) {
                        currentBoundaryStatus = BoundaryStatus.RED
                        Log.d(TAG, "LOST GREEN: Lower body joints (hip/knee/ankle) not sufficiently visible.")
                    } else {
                        currentBoundaryStatus = BoundaryStatus.GREEN
                        Log.d(TAG, "Still GREEN (Ongoing): Lower body visible.")
                    }

                    if (currentBoundaryStatus == BoundaryStatus.GREEN) {
                        jointsToTrack.forEach { (jointName, landmarks) ->
                            newRawAngles[jointName] = PoseUtils.calculateAngle(
                                pose,
                                landmarks.first,
                                landmarks.second,
                                landmarks.third
                            )
                        }

                        newRawAngles.mapValuesTo(currentJointAngles) { (jointName, rawAngle) ->
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
                    } else {
                        smoothedAnglesMap.clear()
                        currentJointAngles.clear()
                    }
                }
            } else {
                currentBoundaryStatus = BoundaryStatus.RED
                smoothedAnglesMap.clear()
                currentJointAngles.clear()
            }

            poseUiState = PoseUiState(
                pose = if (currentBoundaryStatus == BoundaryStatus.GREEN) pose else null,
                imageWidth = currentImageWidth,
                imageHeight = currentImageHeight,
                jointAngles = currentJointAngles,
                boundaryStatus = currentBoundaryStatus,
                personBoundingBox = personDisplayBoundingBox
            )
            imageProxy.close()
        }
    }

    fun resetTracking() {
        hasAchievedGreenZone = false
        smoothedAnglesMap.clear()
        poseUiState = PoseUiState()
    }

    override fun onCleared() {
        super.onCleared()
    }
}


@Composable
fun FreeMovementTrackingScreen(navController: NavController, viewModel: FreeMovementViewModel = viewModel()) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    // State to hold the PreviewView's dimensions and scale type
    var previewViewSize by remember { mutableStateOf(IntSize.Zero) }
    var previewScaleType by remember { mutableStateOf(PreviewView.ScaleType.FILL_CENTER) }

    LaunchedEffect(Unit) {
        viewModel.resetTracking()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Pass the new callback to CameraPreviewView
        CameraPreviewView(
            modifier = Modifier.fillMaxSize(),
            lensFacing = lensFacing,
            onImageAnalyzed = { imageProxy ->
                viewModel.processImageProxy(imageProxy)
            },
            // This is the new parameter
            onPreviewViewSizeChanged = { size, scaleType ->
                previewViewSize = size
                previewScaleType = scaleType
            }
        )

        // Pass the new state variables to PoseOverlay
        PoseOverlay(
            pose = viewModel.poseUiState.pose,
            imageWidth = viewModel.poseUiState.imageWidth,
            imageHeight = viewModel.poseUiState.imageHeight,
            lensFacing = lensFacing,
            jointAngles = viewModel.poseUiState.jointAngles,
            boundaryStatus = viewModel.poseUiState.boundaryStatus,
            personBoundingBox = viewModel.poseUiState.personBoundingBox,
            modifier = Modifier.fillMaxSize(),
            // These are the new parameters
            previewViewSize = previewViewSize,
            previewScaleType = previewScaleType
        )

        FlipCameraButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK
                else
                    CameraSelector.LENS_FACING_FRONT
                viewModel.resetTracking()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}