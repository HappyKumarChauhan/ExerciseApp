//// app/src/main/java/com/example/exerciseapp/ui/viewmodel/SitToStandExerciseViewModel.kt
//package com.example.exerciseapp.ui.viewmodel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import kotlin.math.abs
//import kotlin.math.min
//import com.google.mlkit.vision.pose.PoseLandmark
//import com.example.exerciseapp.ml.PoseAnalyzer
//import com.example.exerciseapp.ml.PoseUtils
//import com.example.exerciseapp.BoundaryStatus
//import com.example.exerciseapp.PoseUiState
//import androidx.camera.core.CameraSelector
//import androidx.compose.ui.geometry.Rect // <--- NEW IMPORT for Compose Rect
//
//// Enums for Exercise Management
//enum class ExerciseType {
//    FIVE_REPS,
//    THIRTY_SECONDS,
//    NONE // No exercise selected
//}
//
//enum class ExerciseState {
//    IDLE,       // Waiting to start, ready for type selection
//    PREPARING,  // Calibration complete, showing initial instructions/ready to start the timer
//    RUNNING,    // Exercise in progress
//    PAUSED,     // Exercise paused (optional, for later if needed)
//    COMPLETED   // Exercise finished
//}
//
//class SitToStandExerciseViewModel(
//    private val initialStandingAngle: Float,
//    private val valinitialSittingAngle: Float
//) : ViewModel() {
//
//    private val TAG = "SitToStandExerciseVM"
//
//    // --- Exercise Configuration ---
//    private val _exerciseType = MutableStateFlow(ExerciseType.NONE)
//    val exerciseType: StateFlow<ExerciseType> = _exerciseType.asStateFlow()
//
//    // --- Calibrated Angles ---
//    private val standingAngle = initialStandingAngle
//    private val sittingAngle = initialSittingAngle
//    // Threshold for determining sit/stand state from calibrated angles.
//    // You might need to adjust this value based on testing.
//    private val angleThreshold = 10f
//
//    // Derived properties for easy state checking
//    private val standZoneMin = standingAngle - angleThreshold
//    private val standZoneMax = standingAngle + angleThreshold
//    private val sitZoneMin = sittingAngle - angleThreshold
//    private val sitZoneMax = sittingAngle + angleThreshold
//
//    // --- Exercise State ---
//    private val _exerciseState = MutableStateFlow(ExerciseState.IDLE)
//    val exerciseState: StateFlow<ExerciseState> = _exerciseState.asStateFlow()
//
//    // --- Exercise Metrics ---
//    private val _repsCount = MutableStateFlow(0)
//    val repsCount: StateFlow<Int> = _repsCount.asStateFlow()
//
//    private val _timerSeconds = MutableStateFlow(0)
//    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
//
//    private val _currentHipAngle = MutableStateFlow(0f)
//    val currentHipAngle: StateFlow<Float> = _currentHipAngle.asStateFlow()
//
//    // --- Pose Detection & Boundary ---
//    private val _poseUiState = MutableStateFlow(PoseUiState())
//    val poseUiState: StateFlow<PoseUiState> = _poseUiState.asStateFlow()
//
//    // Internal state for rep counting
//    private var lastPoseState: String = "UNKNOWN" // "SITTING", "STANDING", "TRANSITIONING_FROM_SIT", "TRANSITIONING_FROM_STAND", "UNKNOWN"
//    private var timerJob: Job? = null
//
//    // Boundary detection parameters for exercise (can be stricter than calibration)
//    // These should ideally match the values in SitToStandExerciseScreen's processImageProxy for consistency.
//    private val EXERCISE_MIN_JOINT_LIKELIHOOD = 0.80f
//    private val EXERCISE_IDEAL_ZONE_PADDING_X = 0.10f
//    private val EXERCISE_IDEAL_ZONE_PADDING_Y = 0.05f
//    private val EXERCISE_MIN_PERSON_WIDTH_RATIO = 0.20f
//    private val EXERCISE_MAX_PERSON_WIDTH_RATIO = 0.80f
//    private val EXERCISE_MIN_PERSON_HEIGHT_RATIO = 0.50f
//    private val EXERCISE_MAX_PERSON_HEIGHT_RATIO = 0.95f
//
//    // Define critical joints for a full-body exercise check
//    private val CRITICAL_EXERCISE_JOINTS = listOf(
//        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
//        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
//        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER
//    )
//
//
//    init {
//        Log.d(TAG, "ViewModel initialized with Standing: $standingAngle, Sitting: $sittingAngle")
//        _exerciseState.value = ExerciseState.PREPARING // Immediately go to preparing state
//    }
//
//    // Call this from UI to set the desired exercise type
//    fun setExerciseType(type: ExerciseType) {
//        if (_exerciseState.value == ExerciseState.IDLE || _exerciseState.value == ExerciseState.PREPARING || _exerciseState.value == ExerciseState.COMPLETED) {
//            _exerciseType.value = type
//            Log.d(TAG, "Exercise type set to: $type")
//            resetExerciseData() // Ensure data is reset when type is set
//            _exerciseState.value = ExerciseState.PREPARING // Go back to preparing state after setting type
//        }
//    }
//
//    fun startExercise() {
//        if (_exerciseType.value == ExerciseType.NONE) {
//            Log.e(TAG, "Cannot start exercise: No exercise type selected.")
//            return
//        }
//        if (_exerciseState.value == ExerciseState.RUNNING) {
//            Log.w(TAG, "Exercise is already running.")
//            return
//        }
//        if (standingAngle == 0f && sittingAngle == 0f) { // Simple check if angles are default uncalibrated
//            Log.e(TAG, "Calibration angles are default. Please ensure calibration was performed.")
//            // You might want to navigate back to calibration or show an error to the user
//            return
//        }
//
//        resetExerciseData(keepExerciseType = true) // Reset metrics but keep type
//        _exerciseState.value = ExerciseState.RUNNING
//        startTimer()
//        Log.d(TAG, "Exercise started: ${_exerciseType.value}")
//    }
//
//    fun stopExercise() {
//        if (_exerciseState.value == ExerciseState.RUNNING) {
//            timerJob?.cancel()
//            _exerciseState.value = ExerciseState.COMPLETED
//            Log.d(TAG, "Exercise stopped. Final Reps: ${_repsCount.value}, Final Time: ${_timerSeconds.value}")
//        }
//    }
//
//    fun resetExercise() {
//        stopExercise() // Ensure timer is stopped if running
//        resetExerciseData(keepExerciseType = false) // Reset everything, including type
//        _exerciseState.value = ExerciseState.IDLE // Go back to IDLE to allow re-selection
//        Log.d(TAG, "Exercise reset.")
//    }
//
//    private fun resetExerciseData(keepExerciseType: Boolean = false) {
//        _repsCount.value = 0
//        _timerSeconds.value = 0
//        _currentHipAngle.value = 0f
//        lastPoseState = "UNKNOWN"
//        timerJob?.cancel() // Ensure timer job is cancelled
//        if (!keepExerciseType) {
//            _exerciseType.value = ExerciseType.NONE
//        }
//        Log.d(TAG, "Exercise data reset. Keep Type: $keepExerciseType")
//    }
//
//    private fun startTimer() {
//        timerJob?.cancel() // Cancel any existing timer
//        timerJob = viewModelScope.launch {
//            if (_exerciseType.value == ExerciseType.THIRTY_SECONDS) {
//                // Countdown for 30 seconds
//                _timerSeconds.value = 30
//                while (_timerSeconds.value > 0 && _exerciseState.value == ExerciseState.RUNNING) {
//                    delay(1000)
//                    _timerSeconds.value -= 1
//                }
//                if (_exerciseState.value == ExerciseState.RUNNING) { // Check if still running (not manually stopped)
//                    stopExercise() // Automatically stop when timer hits 0
//                    Log.d(TAG, "30-second test completed.")
//                }
//            } else if (_exerciseType.value == ExerciseType.FIVE_REPS) {
//                // Count-up for 5 Reps
//                _timerSeconds.value = 0
//                while (_repsCount.value < 5 && _exerciseState.value == ExerciseState.RUNNING) {
//                    delay(1000)
//                    _timerSeconds.value += 1
//                }
//                if (_exerciseState.value == ExerciseState.RUNNING) { // Check if still running
//                    stopExercise() // Automatically stop when 5 reps are completed
//                    Log.d(TAG, "5-rep test completed.")
//                }
//            }
//        }
//    }
//
//    // This method is called by the CameraPreviewView's onImageAnalyzed.
//    // It detects pose, updates boundary status, and processes angles for reps.
//    fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy, lensFacing: Int, currentCanvasWidth: Float, currentCanvasHeight: Float, poseAnalyzer: PoseAnalyzer) {
//        poseAnalyzer.detectPose(imageProxy) { pose ->
//            val currentImageWidth = imageProxy.width
//            val currentImageHeight = imageProxy.height
//
//            var currentBoundaryStatus = BoundaryStatus.RED // Default to RED
//            var personDisplayBoundingBox: android.graphics.RectF? = null // Using RectF for float coordinates
//            var hipAngle: Double? = null // Change to Double? as PoseUtils.calculateAngle returns Double
//
//            if (pose != null && pose.allPoseLandmarks.isNotEmpty()) {
//                var minXRaw = Float.MAX_VALUE
//                var minYRaw = Float.MAX_VALUE
//                var maxXRaw = Float.MIN_VALUE
//                var maxYRaw = Float.MIN_VALUE
//
//                val MIN_LIKELIHOOD_FOR_BOUNDING_BOX = 0.5f // Reasonable for bounding box
//                var anyLandmarkDetectedForBox = false
//
//                for (landmark in pose.allPoseLandmarks) {
//                    if (landmark.inFrameLikelihood >= MIN_LIKELIHOOD_FOR_BOUNDING_BOX) {
//                        minXRaw = minOf(minXRaw, landmark.position.x)
//                        minYRaw = minOf(minYRaw, landmark.position.y)
//                        maxXRaw = maxOf(maxXRaw, landmark.position.x)
//                        maxYRaw = maxOf(maxYRaw, landmark.position.y)
//                        anyLandmarkDetectedForBox = true
//                    }
//                }
//
//                if (anyLandmarkDetectedForBox) {
//                    val scaleX = currentCanvasWidth / currentImageWidth.toFloat()
//                    val scaleY = currentCanvasHeight / currentImageHeight.toFloat()
//                    val scaleFactor = min(scaleX, scaleY)
//
//                    val scaledImageWidth = currentImageWidth * scaleFactor
//                    val scaledImageHeight = currentImageHeight * scaleFactor
//
//                    val offsetX = (currentCanvasWidth - scaledImageWidth) / 2
//                    val offsetY = (currentCanvasHeight - scaledImageHeight) / 2
//
//                    var transformedLeft = minXRaw * scaleFactor + offsetX
//                    var transformedRight = maxXRaw * scaleFactor + offsetX
//
//                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//                        val tempLeft = transformedLeft
//                        transformedLeft = (currentCanvasWidth - transformedRight)
//                        transformedRight = (currentCanvasWidth - tempLeft)
//                    }
//                    val transformedTop = minYRaw * scaleFactor + offsetY
//                    val transformedBottom = maxYRaw * scaleFactor + offsetY
//                    personDisplayBoundingBox = android.graphics.RectF(transformedLeft, transformedTop, transformedRight, transformedBottom)
//                }
//
//                // Check for being in the exercise "green zone" (can be stricter than calibration)
//                var allCriticalJointsConfident = true
//                for (landmarkType in CRITICAL_EXERCISE_JOINTS) {
//                    val landmark = pose.getPoseLandmark(landmarkType)
//                    if (landmark == null || landmark.inFrameLikelihood < EXERCISE_MIN_JOINT_LIKELIHOOD) {
//                        allCriticalJointsConfident = false
//                        break
//                    }
//                }
//
//                val isPersonCenteredAndSized = personDisplayBoundingBox?.let { box ->
//                    box.left >= currentCanvasWidth * EXERCISE_IDEAL_ZONE_PADDING_X &&
//                            box.right <= currentCanvasWidth * (1 - EXERCISE_IDEAL_ZONE_PADDING_X) &&
//                            box.top >= currentCanvasHeight * EXERCISE_IDEAL_ZONE_PADDING_Y &&
//                            box.bottom <= currentCanvasHeight * (1 - EXERCISE_IDEAL_ZONE_PADDING_Y) &&
//                            box.width() >= currentCanvasWidth * EXERCISE_MIN_PERSON_WIDTH_RATIO &&
//                            box.width() <= currentCanvasWidth * EXERCISE_MAX_PERSON_WIDTH_RATIO &&
//                            box.height() >= currentCanvasHeight * EXERCISE_MIN_PERSON_HEIGHT_RATIO &&
//                            box.height() <= currentCanvasHeight * EXERCISE_MAX_PERSON_HEIGHT_RATIO
//                } ?: false
//
//                if (allCriticalJointsConfident && isPersonCenteredAndSized) {
//                    currentBoundaryStatus = BoundaryStatus.GREEN
//                }
//
//
//                // --- Rep Counting Logic (only if exercise is RUNNING and in Green Zone) ---
//                if (_exerciseState.value == ExerciseState.RUNNING && currentBoundaryStatus == BoundaryStatus.GREEN) {
//                    // Calculate hip angle
//                    hipAngle = PoseUtils.calculateAngle(
//                        pose, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER
//                    ) ?: PoseUtils.calculateAngle(
//                        pose, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER
//                    )
//                    // Convert Double? to Float? before assigning to _currentHipAngle.value
//                    _currentHipAngle.value = hipAngle?.toFloat() ?: 0f // <--- Corrected: Convert Double to Float
//
//                    hipAngle?.let { angle -> // 'angle' here is Double, which is fine for comparison
//                        // Determine current pose state based on calibrated angles and threshold
//                        val currentState = when {
//                            abs(angle - sittingAngle) <= angleThreshold -> "SITTING"
//                            abs(angle - standingAngle) <= angleThreshold -> "STANDING"
//                            else -> "TRANSITIONING"
//                        }
//
//                        // Rep detection: Sitting -> Standing -> Sitting = 1 rep
//                        when (lastPoseState) {
//                            "UNKNOWN" -> { // Initial state, wait for first clear position
//                                if (currentState == "SITTING" || currentState == "STANDING") {
//                                    lastPoseState = currentState
//                                }
//                            }
//                            "SITTING" -> {
//                                if (currentState == "STANDING") {
//                                    lastPoseState = "STANDING"
//                                    Log.d(TAG, "Transitioned from SITTING to STANDING")
//                                } else if (currentState == "TRANSITIONING") {
//                                    // If moving out of sitting, but not fully standing yet, mark as transitioning
//                                    lastPoseState = "TRANSITIONING_FROM_SIT"
//                                }
//                            }
//                            "TRANSITIONING_FROM_SIT" -> {
//                                if (currentState == "STANDING") {
//                                    lastPoseState = "STANDING"
//                                    Log.d(TAG, "Completed transition from SITTING to STANDING")
//                                } else if (currentState == "SITTING") {
//                                    // Fell back to sitting during transition
//                                    lastPoseState = "SITTING"
//                                }
//                            }
//                            "STANDING" -> {
//                                if (currentState == "SITTING") {
//                                    _repsCount.value += 1
//                                    lastPoseState = "SITTING"
//                                    Log.d(TAG, "Completed Rep! Total Reps: ${_repsCount.value}")
//                                } else if (currentState == "TRANSITIONING") {
//                                    // If moving out of standing, but not fully sitting yet, mark as transitioning
//                                    lastPoseState = "TRANSITIONING_FROM_STAND"
//                                }
//                            }
//                            "TRANSITIONING_FROM_STAND" -> {
//                                if (currentState == "SITTING") {
//                                    _repsCount.value += 1
//                                    lastPoseState = "SITTING"
//                                    Log.d(TAG, "Completed Rep via TRANSITIONING! Total Reps: ${_repsCount.value}")
//                                } else if (currentState == "STANDING") {
//                                    // Fell back to standing during transition
//                                    lastPoseState = "STANDING"
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    _currentHipAngle.value = 0f // Reset angle if not running or not in green zone
//                }
//            } else {
//                // No pose detected at all
//                currentBoundaryStatus = BoundaryStatus.RED
//                _currentHipAngle.value = 0f
//            }
//
//            // Update UI state
//            _poseUiState.update {
//                it.copy(
//                    pose = if (currentBoundaryStatus == BoundaryStatus.GREEN) pose else null, // Only draw skeleton if in green zone
//                    imageWidth = currentImageWidth,
//                    imageHeight = currentImageHeight,
//                    jointAngles = hipAngle?.let { angle -> mapOf("Hip" to angle.toFloat()) } ?: emptyMap(), // <--- Corrected: Convert Double to Float for the Map value
//                    boundaryStatus = currentBoundaryStatus,
//                    personBoundingBox = personDisplayBoundingBox?.let { rectF -> // <--- Corrected: Convert RectF to Compose Rect
//                        Rect(rectF.left, rectF.top, rectF.right, rectF.bottom)
//                    }
//                )
//            }
//            imageProxy.close()
//        }
//    }
//
//    // Factory to allow passing arguments to ViewModel constructor
//    companion object {
//        fun provideFactory(standingAngle: Float, sittingAngle: Float): androidx.lifecycle.ViewModelProvider.Factory =
//            object : androidx.lifecycle.ViewModelProvider.Factory {
//                @Suppress("UNCHECKED_CAST")
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    return SitToStandExerciseViewModel(standingAngle, sittingAngle) as T
//                }
//            }
//    }