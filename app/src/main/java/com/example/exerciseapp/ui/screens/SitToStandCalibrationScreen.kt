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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.exerciseapp.BoundaryStatus
import com.example.exerciseapp.PoseUiState
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ml.PoseUtils
import com.example.exerciseapp.ui.components.CalibrationIndicator
import com.example.exerciseapp.ui.components.CameraPreviewView
import com.example.exerciseapp.ui.components.CountdownTimer
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


class SitToStandCalibrationViewModel : ViewModel() {
    private val poseAnalyzer = PoseAnalyzer()
    private val TAG = "SitToStandCalibVM"

    private val _poseUiState = MutableStateFlow(PoseUiState())
    val poseUiState: StateFlow<PoseUiState> = _poseUiState.asStateFlow()

    private val _calibrationState = MutableStateFlow<CalibrationState>(CalibrationState.Idle)
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    private val _instructionMessage = MutableStateFlow("Press 'Calibrate Standing' to begin.")
    val instructionMessage: StateFlow<String> = _instructionMessage.asStateFlow()

    var standingAngle by mutableDoubleStateOf(0.0)
        private set
    var sittingAngle by mutableDoubleStateOf(0.0)
        private set

    private var hasAchievedGreenZoneForHold by mutableStateOf(false)
    private var greenZoneHoldStartTime by mutableStateOf(0L)
    private var greenZoneExitTime by mutableStateOf(0L)

    val CALIBRATION_HOLD_TIME_MS = 2000L
    private val CALIBRATION_GREEN_ZONE_GRACE_PERIOD_MS = 750L
    private val MIN_CALIBRATION_ANGLE_DIFF = 10.0

    private val CRITICAL_INITIAL_JOINTS = listOf(
        PoseLandmark.NOSE, PoseLandmark.LEFT_EYE, PoseLandmark.RIGHT_EYE,
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

    private val CALIBRATION_MIN_JOINT_LIKELIHOOD = 0.60f
    private val CALIBRATION_IDEAL_ZONE_PADDING_X = 0.20f
    private val CALIBRATION_IDEAL_ZONE_PADDING_Y = 0.15f
    private val CALIBRATION_MIN_PERSON_WIDTH_RATIO = 0.08f
    private val CALIBRATION_MAX_PERSON_WIDTH_RATIO = 0.98f
    private val CALIBRATION_MIN_PERSON_HEIGHT_RATIO = 0.25f
    private val CALIBRATION_MAX_PERSON_HEIGHT_RATIO = 0.99f


    fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy, lensFacing: Int, currentCanvasWidth: Float, currentCanvasHeight: Float) {
        poseAnalyzer.detectPose(imageProxy) { pose ->
            val currentImageWidth = imageProxy.width
            val currentImageHeight = imageProxy.height

            var currentBoundaryStatus = BoundaryStatus.RED
            var personDisplayBoundingBox: Rect? = null

            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
                var minXRaw = Float.MAX_VALUE
                var minYRaw = Float.MAX_VALUE
                var maxXRaw = Float.MIN_VALUE
                var maxYRaw = Float.MIN_VALUE

                val MIN_LIKELIHOOD_FOR_BOUNDING_BOX = 0.3f
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
                    val scaleX = currentCanvasWidth / currentImageWidth.toFloat()
                    val scaleY = currentCanvasHeight / currentImageHeight.toFloat()
                    val scaleFactor = min(scaleX, scaleY)

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

                var allInitialJointsConfidentCalib = true
                for (landmarkType in CRITICAL_INITIAL_JOINTS) {
                    val landmark = pose.getPoseLandmark(landmarkType)
                    if (landmark == null || landmark.inFrameLikelihood < CALIBRATION_MIN_JOINT_LIKELIHOOD) {
                        allInitialJointsConfidentCalib = false
                        break
                    }
                }

                val isPersonCenteredAndSizedCalib = personDisplayBoundingBox?.let { box ->
                    box.left >= currentCanvasWidth * CALIBRATION_IDEAL_ZONE_PADDING_X &&
                            box.right <= currentCanvasWidth * (1 - CALIBRATION_IDEAL_ZONE_PADDING_X) &&
                            box.top >= currentCanvasHeight * CALIBRATION_IDEAL_ZONE_PADDING_Y &&
                            box.bottom <= currentCanvasHeight * (1 - CALIBRATION_IDEAL_ZONE_PADDING_Y) &&
                            box.width >= currentCanvasWidth * CALIBRATION_MIN_PERSON_WIDTH_RATIO &&
                            box.width <= currentCanvasWidth * CALIBRATION_MAX_PERSON_WIDTH_RATIO &&
                            box.height >= currentCanvasHeight * CALIBRATION_MIN_PERSON_HEIGHT_RATIO &&
                            box.height <= currentCanvasHeight * CALIBRATION_MAX_PERSON_HEIGHT_RATIO
                } ?: false

                val isInCalibrationGreenZone = allInitialJointsConfidentCalib && isPersonCenteredAndSizedCalib

                val currentTime = System.currentTimeMillis()

                if (isInCalibrationGreenZone) {
                    if (!hasAchievedGreenZoneForHold) {
                        hasAchievedGreenZoneForHold = true
                        greenZoneHoldStartTime = currentTime
                        greenZoneExitTime = 0L
                        Log.d(TAG, "Entered Calibration Green Zone. Starting hold timer.")
                    }
                    currentBoundaryStatus = BoundaryStatus.GREEN
                } else {
                    if (hasAchievedGreenZoneForHold) {
                        if (greenZoneExitTime == 0L) {
                            greenZoneExitTime = currentTime
                            Log.d(TAG, "Exited Calibration Green Zone. Starting grace period.")
                        }

                        if (currentTime - greenZoneExitTime > CALIBRATION_GREEN_ZONE_GRACE_PERIOD_MS) {
                            hasAchievedGreenZoneForHold = false
                            greenZoneHoldStartTime = 0L
                            greenZoneExitTime = 0L
                            Log.d(TAG, "Grace period expired. Resetting hold timer.")
                            if (_calibrationState.value is CalibrationState.CalibratingStanding || _calibrationState.value is CalibrationState.CalibratingSitting) {
                                _instructionMessage.value = "Calibration interrupted! Keep your body fully in frame and stable."
                            }
                            currentBoundaryStatus = BoundaryStatus.RED
                        } else {
                            currentBoundaryStatus = BoundaryStatus.GREEN
                        }
                    } else {
                        currentBoundaryStatus = BoundaryStatus.RED
                        greenZoneHoldStartTime = 0L
                        greenZoneExitTime = 0L
                    }
                }

                when (val state = _calibrationState.value) {
                    is CalibrationState.CalibratingStanding -> {
                        _instructionMessage.value = "STAND TALL & STILL for calibration."
                        if (hasAchievedGreenZoneForHold && (currentTime - greenZoneHoldStartTime >= CALIBRATION_HOLD_TIME_MS)) {
                            val currentHipAngle = PoseUtils.calculateAngle(
                                pose, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER
                            ) ?: PoseUtils.calculateAngle(
                                pose, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER
                            )

                            if (currentHipAngle != null) {
                                standingAngle = currentHipAngle
                                Log.d(TAG, "Standing angle calibrated: $standingAngle")
                                _calibrationState.value = CalibrationState.StandingCalibrated
                                _instructionMessage.value = "Standing Position Calibrated Successfully! Now Calibrate Sitting."
                            } else {
                                _instructionMessage.value = "Could not calculate hip angle for standing. Please adjust position."
                                _calibrationState.value = CalibrationState.Idle
                            }
                            hasAchievedGreenZoneForHold = false
                            greenZoneHoldStartTime = 0L
                            greenZoneExitTime = 0L
                        }
                    }
                    is CalibrationState.CalibratingSitting -> {
                        _instructionMessage.value = "SIT DOWN & STILL for calibration."
                        if (hasAchievedGreenZoneForHold && (currentTime - greenZoneHoldStartTime >= CALIBRATION_HOLD_TIME_MS)) {
                            val currentHipAngle = PoseUtils.calculateAngle(
                                pose, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER
                            ) ?: PoseUtils.calculateAngle(
                                pose, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER
                            )

                            if (currentHipAngle != null) {
                                sittingAngle = currentHipAngle
                                Log.d(TAG, "Sitting angle calibrated: $sittingAngle")

                                if (standingAngle != 0.0 && (standingAngle - sittingAngle) > MIN_CALIBRATION_ANGLE_DIFF) {
                                    _calibrationState.value = CalibrationState.SittingCalibrated
                                    _instructionMessage.value = "Calibration Complete! Ready for Exercise."
                                } else {
                                    _instructionMessage.value = "Sitting calibration failed. Angle difference is too small. Try again."
                                    _calibrationState.value = CalibrationState.StandingCalibrated
                                }
                            } else {
                                _instructionMessage.value = "Could not calculate hip angle for sitting. Please adjust position."
                                _calibrationState.value = CalibrationState.StandingCalibrated
                            }
                            hasAchievedGreenZoneForHold = false
                            greenZoneHoldStartTime = 0L
                            greenZoneExitTime = 0L
                        }
                    }
                    else -> { /* No specific logic for other states */ }
                }
            } else {
                currentBoundaryStatus = BoundaryStatus.RED
                hasAchievedGreenZoneForHold = false
                greenZoneHoldStartTime = 0L
                greenZoneExitTime = 0L
                if (_calibrationState.value is CalibrationState.CalibratingStanding || _calibrationState.value is CalibrationState.CalibratingSitting) {
                    _instructionMessage.value = "No person detected. Ensure good lighting and move into the camera's view."
                }
            }

            _poseUiState.update {
                it.copy(
                    pose = if (currentBoundaryStatus == BoundaryStatus.GREEN) pose else null,
                    imageWidth = currentImageWidth,
                    imageHeight = currentImageHeight,
                    jointAngles = emptyMap(),
                    boundaryStatus = currentBoundaryStatus,
                    personBoundingBox = personDisplayBoundingBox
                )
            }
            imageProxy.close()
        }
    }

    fun startCalibration(stage: CalibrationStage) {
        _calibrationState.value = when (stage) {
            CalibrationStage.STANDING -> CalibrationState.CalibratingStanding(CALIBRATION_HOLD_TIME_MS / 1000)
            CalibrationStage.SITTING -> CalibrationState.CalibratingSitting(CALIBRATION_HOLD_TIME_MS / 1000)
        }
        hasAchievedGreenZoneForHold = false
        greenZoneHoldStartTime = 0L
        greenZoneExitTime = 0L
    }

    fun resetCalibration() {
        _calibrationState.value = CalibrationState.Idle
        _instructionMessage.value = "Press 'Calibrate Standing' to begin."
        standingAngle = 0.0
        sittingAngle = 0.0
        hasAchievedGreenZoneForHold = false
        greenZoneHoldStartTime = 0L
        greenZoneExitTime = 0L
    }

    fun updateCalibrationCountdown(newCountdown: Int) {
        _calibrationState.update { currentState ->
            when (currentState) {
                is CalibrationState.CalibratingStanding -> currentState.copy(countdownSec = newCountdown.toLong())
                is CalibrationState.CalibratingSitting -> currentState.copy(countdownSec = newCountdown.toLong())
                else -> currentState
            }
        }
    }
}

enum class CalibrationStage {
    STANDING, SITTING
}

sealed class CalibrationState {
    object Idle : CalibrationState()
    data class CalibratingStanding(val countdownSec: Long) : CalibrationState()
    data class CalibratingSitting(val countdownSec: Long) : CalibrationState()
    object StandingCalibrated : CalibrationState()
    object SittingCalibrated : CalibrationState()
    data class Failed(val message: String) : CalibrationState()
}

@Composable
fun SitToStandCalibrationScreen(
    navController: NavController,
    viewModel: SitToStandCalibrationViewModel = viewModel()
) {
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    val poseUiState by viewModel.poseUiState.collectAsState()
    val calibrationState by viewModel.calibrationState.collectAsState()
    val instructionMessage by viewModel.instructionMessage.collectAsState()

    val countdownDisplay by remember(poseUiState.boundaryStatus, calibrationState) {
        mutableStateOf(
            if (poseUiState.boundaryStatus == BoundaryStatus.GREEN) {
                when (calibrationState) {
                    is CalibrationState.CalibratingStanding -> (calibrationState as CalibrationState.CalibratingStanding).countdownSec.toInt()
                    is CalibrationState.CalibratingSitting -> (calibrationState as CalibrationState.CalibratingSitting).countdownSec.toInt()
                    else -> 0
                }
            } else 0
        )
    }

    LaunchedEffect(poseUiState.boundaryStatus, calibrationState) {
        if (poseUiState.boundaryStatus == BoundaryStatus.GREEN &&
            (calibrationState is CalibrationState.CalibratingStanding || calibrationState is CalibrationState.CalibratingSitting)
        ) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < viewModel.CALIBRATION_HOLD_TIME_MS) {
                val remainingMillis = viewModel.CALIBRATION_HOLD_TIME_MS - (System.currentTimeMillis() - startTime)
                val newCountdownSec = if (remainingMillis > 0) (remainingMillis / 1000).toInt() + 1 else 0
                viewModel.updateCalibrationCountdown(newCountdownSec)
                delay(100L)
            }
            viewModel.updateCalibrationCountdown(0)
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
                jointAngles = emptyMap(),
                boundaryStatus = poseUiState.boundaryStatus,
                personBoundingBox = poseUiState.personBoundingBox,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 12.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sit to Stand Calibration",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = instructionMessage,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if ((calibrationState is CalibrationState.CalibratingStanding || calibrationState is CalibrationState.CalibratingSitting) &&
                    poseUiState.boundaryStatus == BoundaryStatus.GREEN && countdownDisplay > 0
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CountdownTimer(time = countdownDisplay)
                }
            }
        }

        // Buttons Bar at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { viewModel.startCalibration(CalibrationStage.STANDING) },
                enabled = calibrationState is CalibrationState.Idle || calibrationState is CalibrationState.Failed || calibrationState is CalibrationState.StandingCalibrated,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calibrationState is CalibrationState.StandingCalibrated) Color.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Calibrate Standing (${"%.1f".format(viewModel.standingAngle)}°)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.startCalibration(CalibrationStage.SITTING) },
                enabled = calibrationState is CalibrationState.StandingCalibrated,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calibrationState is CalibrationState.SittingCalibrated) Color.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Calibrate Sitting (${"%.1f".format(viewModel.sittingAngle)}°)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    navController.navigate("sit_to_stand_exercise/${viewModel.standingAngle.toFloat()}/${viewModel.sittingAngle.toFloat()}")
                },
                enabled = calibrationState is CalibrationState.SittingCalibrated,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Start Exercise", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.resetCalibration() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text("Reset Calibration", fontSize = 14.sp)
            }
        }

        FlipCameraButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK
                else
                    CameraSelector.LENS_FACING_FRONT
                viewModel.resetCalibration()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 200.dp, end = 16.dp)
        )
    }
}