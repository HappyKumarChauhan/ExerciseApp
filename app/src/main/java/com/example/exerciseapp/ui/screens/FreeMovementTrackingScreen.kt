// app/src/main/java/com/example/exerciseapp/ui/screens/FreeMovementTrackingScreen.kt
package com.example.exerciseapp.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
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

// ViewModel to hold and process pose data for Free Movement Tracking
class FreeMovementViewModel : androidx.lifecycle.ViewModel() {
    private val poseAnalyzer = PoseAnalyzer()
    private val smoothedAnglesMap = mutableMapOf<String, Double>()
    private val SMOOTHING_FACTOR = 0.3f // Using float for consistency with other parts
    private val TAG = "FreeMovementViewModel"

    // Define critical joints for initial full body detection
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

    // Define critical lower body joints for ongoing tracking check
    private val CRITICAL_LOWER_JOINTS = listOf(
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
    )

    // Likelihood thresholds
    private val MIN_INITIAL_FULL_BODY_LIKELIHOOD = 0.85f // Very strict for initial full body
    private val MIN_ONGOING_LOWER_BODY_LIKELIHOOD = 0.2f // Very lenient, consider lost only if truly low

    // Reactive state for UI
    var poseUiState by mutableStateOf(PoseUiState())
        private set

    var hasAchievedGreenZone by mutableStateOf(false) // State for initial green zone achievement

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

    // Declare mutable maps at class level to avoid recreating them on every frame
    private val newRawAngles = mutableMapOf<String, Double?>()

    fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        poseAnalyzer.detectPose(imageProxy) { pose ->
            val currentImageWidth = imageProxy.width
            val currentImageHeight = imageProxy.height

            // Always clear angle map for current frame
            newRawAngles.clear()

            var currentBoundaryStatus = BoundaryStatus.RED
            var personDisplayBoundingBox: androidx.compose.ui.geometry.Rect? = null
            val currentJointAngles = mutableMapOf<String, Double?>() // Angles to pass to UI

            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {

                // --- 1. Calculate Bounding Box (always based on confidently visible parts) ---
                var minXRaw = Float.MAX_VALUE
                var minYRaw = Float.MAX_VALUE
                var maxXRaw = Float.MIN_VALUE
                var maxYRaw = Float.MIN_VALUE

                val MIN_LIKELIHOOD_FOR_BOUNDING_BOX = 0.5f // Only include landmarks with at least 50% likelihood
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

                if (anyLandmarkDetectedForBox) {
                    // Get current canvas dimensions from PoseUiState or pass them here if available
                    // For now, assuming fixed dimensions for this calculation if not passed
                    // In a real scenario, you'd calculate these based on the actual screen size
                    // where PoseOverlay is being drawn, or pass them down.
                    // For the purpose of ViewModel, we need fixed known values or pass them.
                    // Let's assume a reasonable ratio for bounding box calculation.
                    // The actual scaling for drawing happens in PoseOverlay using its canvas size.
                    // This personBoundingBox is primarily for *detection logic*, not direct drawing.
                    // PoseOverlay will re-calculate its own.
                    // However, for the initial sizing checks, we need *some* scaled coordinates.
                    // Let's use assumed screen width/height for these checks, or better, pass them from Compose side.
                    // For now, let's omit personDisplayBoundingBox calculation here, it's redundant.
                    // The PoseOverlay already draws based on transformed pose.
                    // The critical part is checking landmark likelihoods for boundaryStatus.

                    // The bounding box logic will be better placed in a utility function
                    // that can be called with screen dimensions from the composable.
                    // For now, let's keep it simple and focus on landmark detection for status.
                }


                // --- 2. Determine Boundary Status Logic ---
                if (!hasAchievedGreenZone) {
                    // Initial State: User must achieve full body in frame
                    var allInitialJointsConfident = true
                    for (landmarkType in CRITICAL_INITIAL_JOINTS) {
                        val landmark = pose.getPoseLandmark(landmarkType)
                        if (landmark == null || landmark.inFrameLikelihood < MIN_INITIAL_FULL_BODY_LIKELIHOOD) {
                            allInitialJointsConfident = false
                            break
                        }
                    }

                    // For initial check, we also need to ensure the person fills enough of the screen.
                    // This requires knowing the screen dimensions.
                    // This part of the logic is better handled in the UI layer where screen dimensions are known,
                    // or passed into the ViewModel. For simplicity of the ViewModel, we'll focus only on
                    // landmark confidence here, and let the UI guide the user on position.
                    // Or, we can use a helper function that takes canvas dimensions.
                    // For now, let's keep the initial checks simpler to only landmark confidence.
                    // If you want precise centering/sizing, that logic from previous MainActivity
                    // will need to be adapted here or moved to a more appropriate place.

                    // To keep this ViewModel purely focused on ML interpretation and not UI layout,
                    // we'll primarily use landmark likelihood for `hasAchievedGreenZone`.
                    // The visual 'ideal zone' feedback needs to be done by the UI (PoseOverlay).

                    if (allInitialJointsConfident) {
                        currentBoundaryStatus = BoundaryStatus.GREEN
                        hasAchievedGreenZone = true
                        Log.d(TAG, "ACHIEVED GREEN: All initial joints confident.")
                    } else {
                        currentBoundaryStatus = BoundaryStatus.RED
                        Log.d(TAG, "Still RED (Initial): Not all joints confident.")
                    }

                } else {
                    // Has achieved green zone, now in tracking state
                    currentBoundaryStatus = BoundaryStatus.GREEN // Assume green unless lower body is lost

                    var lowerBodyLost = false
                    for (landmarkType in CRITICAL_LOWER_JOINTS) {
                        val landmark = pose.getPoseLandmark(landmarkType)
                        if (landmark == null || landmark.inFrameLikelihood < MIN_ONGOING_LOWER_BODY_LIKELIHOOD) {
                            lowerBodyLost = true
                            break // Found one missing/low confidence lower joint, so mark as lost
                        }
                    }

                    if (lowerBodyLost) {
                        currentBoundaryStatus = BoundaryStatus.RED
                        Log.d(TAG, "LOST GREEN: Lower body joints (hip/knee/ankle) not sufficiently visible.")
                    } else {
                        // If lower body is visible, and we're in tracking state, then it's GREEN
                        currentBoundaryStatus = BoundaryStatus.GREEN
                        Log.d(TAG, "Still GREEN (Ongoing): Lower body visible.")
                    }

                    // Only calculate and smooth angles if we are in the GREEN state
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
                        // If status is RED, clear smoothed angles to stop displaying stale data
                        smoothedAnglesMap.clear()
                        currentJointAngles.clear()
                    }
                }

            } else {
                // No pose detected at all
                currentBoundaryStatus = BoundaryStatus.RED
                // Do NOT reset hasAchievedGreenZone here,
                // if it was true, it means user temporarily stepped out,
                // or pose detection failed for a moment.
                // It will require re-entry into strict initial pose if this becomes `false`
                // Currently, `hasAchievedGreenZone` only becomes false if specific lower joints are lost.
                // If a full pose is lost, we're still in the "tracking active" state, just temporarily RED.
                // So, `hasAchievedGreenZone` remains true if it was already true.
                // Log.d(TAG, "Still RED: Pose object is null (no pose detected at all).")

                // Clear angles and bounding box if no pose detected
                smoothedAnglesMap.clear()
                currentJointAngles.clear()
            }

            // Update UI state
            poseUiState = PoseUiState(
                pose = if (currentBoundaryStatus == BoundaryStatus.GREEN) pose else null, // Only pass pose if we're green (for PoseOverlay to draw skeleton)
                imageWidth = currentImageWidth,
                imageHeight = currentImageHeight,
                jointAngles = currentJointAngles,
                boundaryStatus = currentBoundaryStatus,
                personBoundingBox = personDisplayBoundingBox // Still pass for now, PoseOverlay will decide to draw based on status
            )
            imageProxy.close() // IMPORTANT: Close the image proxy after processing
        }
    }

    fun resetTracking() {
        hasAchievedGreenZone = false
        smoothedAnglesMap.clear()
        poseUiState = PoseUiState() // Reset UI state
    }

    override fun onCleared() {
        super.onCleared()
        // No explicit resources to release for PoseAnalyzer here, as it's self-managed.
        // If it had a lifecycle, you'd release it here.
    }
}


@Composable
fun FreeMovementTrackingScreen(navController: NavController, viewModel: FreeMovementViewModel = viewModel()) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    // Reset tracking state when entering this screen
    LaunchedEffect(Unit) {
        viewModel.resetTracking()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewView(
            lensFacing = lensFacing,
            onImageAnalyzed = { imageProxy ->
                viewModel.processImageProxy(imageProxy)
            }
        )

        PoseOverlay(
            pose = viewModel.poseUiState.pose,
            imageWidth = viewModel.poseUiState.imageWidth,
            imageHeight = viewModel.poseUiState.imageHeight,
            lensFacing = lensFacing,
            jointAngles = viewModel.poseUiState.jointAngles,
            boundaryStatus = viewModel.poseUiState.boundaryStatus,
            personBoundingBox = viewModel.poseUiState.personBoundingBox, // ADD THIS LINE
            modifier = Modifier.fillMaxSize()
        )

        FlipCameraButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK
                else
                    CameraSelector.LENS_FACING_FRONT
                viewModel.resetTracking() // Reset tracking on camera flip
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}