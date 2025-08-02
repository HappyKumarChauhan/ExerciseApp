// app/src/main/java/com/example/exerciseapp/ui/screens/SitToStandExerciseScreen.kt
package com.example.exerciseapp.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.min
import androidx.compose.ui.geometry.Rect


class SitToStandExerciseViewModel(
    private val standingAngle: Double,
    private val sittingAngle: Double
) : ViewModel() {
    private val poseAnalyzer = PoseAnalyzer()
    private val TAG = "SitToStandExerciseVM"

    private val _poseUiState = MutableStateFlow(PoseUiState())
    val poseUiState: StateFlow<PoseUiState> = _poseUiState.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _exerciseStatus = MutableStateFlow<ExerciseStatus>(ExerciseStatus.Standing)
    val exerciseStatus: StateFlow<ExerciseStatus> = _exerciseStatus.asStateFlow()

    private val _countdownTime = MutableStateFlow(30)
    val countdownTime: StateFlow<Int> = _countdownTime.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // New state for the preparation countdown
    private val _preparationTime = MutableStateFlow(3)
    val preparationTime: StateFlow<Int> = _preparationTime.asStateFlow()

    private val _jointAngles = MutableStateFlow<Map<String, Double>>(emptyMap())
    val jointAngles: StateFlow<Map<String, Double>> = _jointAngles.asStateFlow()

    private val CRITICAL_EXERCISE_JOINTS = listOf(
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.NOSE
    )

    private val EXERCISE_MIN_JOINT_LIKELIHOOD = 0.60f
    private val EXERCISE_IDEAL_ZONE_PADDING_X = 0.20f
    private val EXERCISE_IDEAL_ZONE_PADDING_Y = 0.15f
    private val EXERCISE_MIN_PERSON_WIDTH_RATIO = 0.08f
    private val EXERCISE_MAX_PERSON_WIDTH_RATIO = 0.98f
    private val EXERCISE_MIN_PERSON_HEIGHT_RATIO = 0.25f
    private val EXERCISE_MAX_PERSON_HEIGHT_RATIO = 0.99f

    fun startPreparationTimer() {
        _preparationTime.value = 3
        _isTimerRunning.value = false
    }

    fun startExerciseTimer() {
        _isTimerRunning.value = true
        _countdownTime.value = 30
    }

    fun decrementPreparationTime() {
        if (_preparationTime.value > 0) {
            _preparationTime.update { it - 1 }
        }
    }

    fun decrementCountdown() {
        if (_countdownTime.value > 0) {
            _countdownTime.update { it - 1 }
        } else {
            _isTimerRunning.value = false
        }
    }

    fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy, lensFacing: Int, currentCanvasWidth: Float, currentCanvasHeight: Float) {
        poseAnalyzer.detectPose(imageProxy) { pose ->
            val currentImageWidth = imageProxy.width
            val currentImageHeight = imageProxy.height
            var currentBoundaryStatus = BoundaryStatus.RED
            var personDisplayBoundingBox: Rect? = null
            val newAngles = mutableMapOf<String, Double>()

            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
                var minXRaw = Float.MAX_VALUE
                var minYRaw = Float.MAX_VALUE
                var maxXRaw = Float.MIN_VALUE
                var maxYRaw = Float.MIN_VALUE
                var anyLandmarkDetectedForBox = false
                val MIN_LIKELIHOOD_FOR_BOUNDING_BOX = 0.3f
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
                    val scaleX = currentCanvasWidth / currentImageWidth.toFloat()
                    val scaleY = currentCanvasHeight / currentImageHeight.toFloat()
                    val scaleFactor = minOf(scaleX, scaleY)
                    val scaledImageWidth = currentImageWidth * scaleFactor
                    val scaledImageHeight = currentImageHeight * scaleFactor
                    val offsetX = (currentCanvasWidth - scaledImageWidth) / 2
                    val offsetY = (currentCanvasHeight - scaledImageHeight) / 2

                    var transformedLeft = minXRaw * scaleFactor + offsetX
                    var transformedRight = maxXRaw * scaleFactor + offsetX
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        val tempLeft = transformedLeft
                        transformedLeft = (currentCanvasWidth - transformedRight)
                        transformedRight = (currentCanvasWidth - tempLeft)
                    }
                    val transformedTop = minYRaw * scaleFactor + offsetY
                    val transformedBottom = maxYRaw * scaleFactor + offsetY
                    personDisplayBoundingBox = Rect(transformedLeft, transformedTop, transformedRight, transformedBottom)
                }

                var allCriticalJointsConfident = true
                for (landmarkType in CRITICAL_EXERCISE_JOINTS) {
                    val landmark = pose.getPoseLandmark(landmarkType)
                    if (landmark == null || landmark.inFrameLikelihood < EXERCISE_MIN_JOINT_LIKELIHOOD) {
                        allCriticalJointsConfident = false
                        break
                    }
                }

                val isPersonCenteredAndSized = personDisplayBoundingBox?.let { box ->
                    box.left >= currentCanvasWidth * EXERCISE_IDEAL_ZONE_PADDING_X &&
                            box.right <= currentCanvasWidth * (1 - EXERCISE_IDEAL_ZONE_PADDING_X) &&
                            box.top >= currentCanvasHeight * EXERCISE_IDEAL_ZONE_PADDING_Y &&
                            box.bottom <= currentCanvasHeight * (1 - EXERCISE_IDEAL_ZONE_PADDING_Y) &&
                            box.width >= currentCanvasWidth * EXERCISE_MIN_PERSON_WIDTH_RATIO &&
                            box.width <= currentCanvasWidth * EXERCISE_MAX_PERSON_WIDTH_RATIO &&
                            box.height >= currentCanvasHeight * EXERCISE_MIN_PERSON_HEIGHT_RATIO &&
                            box.height <= currentCanvasHeight * EXERCISE_MAX_PERSON_HEIGHT_RATIO
                } ?: false

                if (allCriticalJointsConfident && isPersonCenteredAndSized) {
                    currentBoundaryStatus = BoundaryStatus.GREEN
                }

                val hipAngle = PoseUtils.calculateAngle(
                    pose, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER
                ) ?: PoseUtils.calculateAngle(
                    pose, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER
                )

                val kneeAngle = PoseUtils.calculateAngle(
                    pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE
                ) ?: PoseUtils.calculateAngle(
                    pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
                )

                if (hipAngle != null) {
                    newAngles["Hip"] = hipAngle
                }
                if (kneeAngle != null) {
                    newAngles["Knee"] = kneeAngle
                }

                if (currentBoundaryStatus == BoundaryStatus.GREEN && _isTimerRunning.value) {
                    if (hipAngle != null) {
                        val isStanding = hipAngle >= standingAngle
                        val isSitting = hipAngle <= sittingAngle

                        when (_exerciseStatus.value) {
                            is ExerciseStatus.Standing -> {
                                if (isSitting) {
                                    Log.d(TAG, "Repetition Logic: User is sitting. Waiting for stand up.")
                                    _exerciseStatus.value = ExerciseStatus.Sitting
                                }
                            }
                            is ExerciseStatus.Sitting -> {
                                if (isStanding) {
                                    Log.d(TAG, "Repetition Logic: User stood up. Incrementing rep.")
                                    _repCount.update { it + 1 }
                                    _exerciseStatus.value = ExerciseStatus.Standing
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Hip angle could not be calculated. Rep logic paused.")
                    }
                }
            } else {
                currentBoundaryStatus = BoundaryStatus.RED
            }

            _poseUiState.update {
                it.copy(
                    pose = pose,
                    imageWidth = currentImageWidth,
                    imageHeight = currentImageHeight,
                    jointAngles = newAngles,
                    boundaryStatus = currentBoundaryStatus,
                    personBoundingBox = personDisplayBoundingBox
                )
            }
            imageProxy.close()
        }
    }
}

sealed class ExerciseStatus {
    object Standing : ExerciseStatus()
    object Sitting : ExerciseStatus()
}

@Composable
fun SitToStandExerciseScreen(
    navController: NavController,
    standingAngle: Double,
    sittingAngle: Double,
    viewModel: SitToStandExerciseViewModel = viewModel(factory = SitToStandExerciseViewModelFactory(standingAngle, sittingAngle))
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    val poseUiState by viewModel.poseUiState.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val countdownTime by viewModel.countdownTime.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val preparationTime by viewModel.preparationTime.collectAsState()

    var isPreparationDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startPreparationTimer()
        while (viewModel.preparationTime.value > 0) {
            delay(1000L)
            viewModel.decrementPreparationTime()
        }
        isPreparationDone = true
        viewModel.startExerciseTimer()
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (viewModel.countdownTime.value > 0) {
                delay(1000L)
                viewModel.decrementCountdown()
            }
            // Exercise is complete, navigate to the results screen
            navController.navigate("exercise_completed/$repCount") {
                popUpTo("sit_to_stand_exercise/{standingAngle}/{sittingAngle}") {
                    inclusive = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val currentCanvasWidth = constraints.maxWidth.toFloat()
            val currentCanvasHeight = constraints.maxHeight.toFloat()

            CameraPreviewView(
                lensFacing = lensFacing,
                onImageAnalyzed = { imageProxy ->
                    viewModel.processImageProxy(imageProxy, lensFacing, currentCanvasWidth, currentCanvasHeight)
                },
                modifier = Modifier.fillMaxSize()
            )

            PoseOverlay(
                pose = poseUiState.pose,
                imageWidth = poseUiState.imageWidth,
                imageHeight = poseUiState.imageHeight,
                lensFacing = lensFacing,
                jointAngles = poseUiState.jointAngles,
                boundaryStatus = poseUiState.boundaryStatus,
                personBoundingBox = poseUiState.personBoundingBox,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sit to Stand",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isPreparationDone) {
                        Text(
                            text = "Reps: $repCount",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CountdownTimer(time = countdownTime)
                    } else {
                        Text(
                            text = "Get Ready",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = preparationTime.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isPreparationDone // Only allow ending the exercise when it has started
                ) {
                    Text("End Exercise", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        FlipCameraButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK
                else
                    CameraSelector.LENS_FACING_FRONT
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
        )
    }
}

@Composable
fun CountdownTimer(time: Int) {
    Text(
        text = "Time: ${"%02d".format(time)}",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

class SitToStandExerciseViewModelFactory(
    private val standingAngle: Double,
    private val sittingAngle: Double
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SitToStandExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SitToStandExerciseViewModel(standingAngle, sittingAngle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}