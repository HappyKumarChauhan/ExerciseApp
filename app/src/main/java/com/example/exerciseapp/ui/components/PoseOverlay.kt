package com.example.exerciseapp.ui.components

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.min

@Composable
fun PoseOverlay(
    pose: Pose?,
    imageWidth: Int,
    imageHeight: Int,
    lensFacing: Int,
    jointAngles: Map<String, Double?> = emptyMap(), // Accept joint angles
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / imageWidth.toFloat()
        val scaleY = canvasHeight / imageHeight.toFloat()

        val scaleFactor = min(scaleX, scaleY)

        val scaledImageWidth = imageWidth * scaleFactor
        val scaledImageHeight = imageHeight * scaleFactor

        val offsetX = (canvasWidth - scaledImageWidth) / 2
        val offsetY = (canvasHeight - scaledImageHeight) / 2

        fun PoseLandmark.toOffset(): Offset {
            var x = position.x
            val y = position.y

            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                x = imageWidth - x
            }

            return Offset(
                x = x * scaleFactor + offsetX,
                y = y * scaleFactor + offsetY
            )
        }

        fun connect(start: PoseLandmark?, end: PoseLandmark?, color: Color) {
            val startPos = start?.toOffset()
            val endPos = end?.toOffset()
            if (startPos != null && endPos != null) {
                drawLine(
                    color = color,
                    start = startPos,
                    end = endPos,
                    strokeWidth = 8f
                )
            }
        }

        val landmarkPoints = pose?.allPoseLandmarks?.map { it.toOffset() } ?: emptyList()
        drawPoints(
            points = landmarkPoints,
            pointMode = PointMode.Points,
            color = Color.White,
            strokeWidth = 12f
        )

        val landmarks = pose?.allPoseLandmarks?.associateBy { it.landmarkType } ?: emptyMap()

        val faceColor = Color.White
        val torsoColor = Color.White
        val leftArmColor = Color.White
        val rightArmColor = Color.White
        val leftLegColor = Color.White
        val rightLegColor = Color.White

        // Face
        connect(landmarks[PoseLandmark.LEFT_MOUTH], landmarks[PoseLandmark.RIGHT_MOUTH], faceColor)
        connect(landmarks[PoseLandmark.LEFT_EYE_INNER], landmarks[PoseLandmark.LEFT_EYE], faceColor)
        connect(landmarks[PoseLandmark.LEFT_EYE], landmarks[PoseLandmark.LEFT_EYE_OUTER], faceColor)
        connect(landmarks[PoseLandmark.LEFT_EYE_OUTER], landmarks[PoseLandmark.LEFT_EAR], faceColor)
        connect(landmarks[PoseLandmark.RIGHT_EYE_INNER], landmarks[PoseLandmark.RIGHT_EYE], faceColor)
        connect(landmarks[PoseLandmark.RIGHT_EYE], landmarks[PoseLandmark.RIGHT_EYE_OUTER], faceColor)
        connect(landmarks[PoseLandmark.RIGHT_EYE_OUTER], landmarks[PoseLandmark.RIGHT_EAR], faceColor)

        // Torso
        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.RIGHT_SHOULDER], torsoColor)
        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)
        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_HIP], torsoColor)
        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], torsoColor) // Connect hips for lower torso
        connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)

        // Left Arm and Hand
        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW], leftArmColor)
        connect(landmarks[PoseLandmark.LEFT_ELBOW], landmarks[PoseLandmark.LEFT_WRIST], leftArmColor)
        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_THUMB], leftArmColor)
        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_PINKY], leftArmColor)
        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor)
        connect(landmarks[PoseLandmark.LEFT_PINKY], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor)

        // Right Arm and Hand
        connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW], rightArmColor)
        connect(landmarks[PoseLandmark.RIGHT_ELBOW], landmarks[PoseLandmark.RIGHT_WRIST], rightArmColor)
        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_THUMB], rightArmColor)
        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_PINKY], rightArmColor)
        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor)
        connect(landmarks[PoseLandmark.RIGHT_PINKY], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor)

        // Left Leg and Foot
        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.LEFT_KNEE], leftLegColor)
        connect(landmarks[PoseLandmark.LEFT_KNEE], landmarks[PoseLandmark.LEFT_ANKLE], leftLegColor)
        connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_HEEL], leftLegColor)
        connect(landmarks[PoseLandmark.LEFT_HEEL], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor)
        connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor)

        // Right Leg and Foot
        connect(landmarks[PoseLandmark.RIGHT_HIP], landmarks[PoseLandmark.RIGHT_KNEE], rightLegColor)
        connect(landmarks[PoseLandmark.RIGHT_KNEE], landmarks[PoseLandmark.RIGHT_ANKLE], rightLegColor)
        connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_HEEL], rightLegColor)
        connect(landmarks[PoseLandmark.RIGHT_HEEL], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor)
        connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor)


        // --- Drawing Joint Angles ---
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { 14.sp.toPx() } // Adjust text size as needed
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Helper to draw angle text near a joint landmark
        fun drawAngle(jointName: String, landmarkType: Int, offsetXPixel: Float = 0f, offsetYPixel: Float = 0f) {
            val angle = jointAngles[jointName]
            val landmark = landmarks[landmarkType]
            if (angle != null && landmark != null) {
                val offset = landmark.toOffset()
                // Only show the angle value, not the joint name
                val angleText = "%.1f°".format(angle) // Format to one decimal place and add degree symbol
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        angleText,
                        offset.x + offsetXPixel,
                        offset.y + offsetYPixel,
                        textPaint
                    )
                }
            }
        }

        // Call drawAngle for each joint you want to display
        // Adjust offsets as needed for better visibility
        drawAngle("Left Elbow", PoseLandmark.LEFT_ELBOW, offsetYPixel = -40f)
        drawAngle("Right Elbow", PoseLandmark.RIGHT_ELBOW, offsetYPixel = -40f)
        drawAngle("Left Shoulder", PoseLandmark.LEFT_SHOULDER, offsetYPixel = -40f, offsetXPixel = -40f)
        drawAngle("Right Shoulder", PoseLandmark.RIGHT_SHOULDER, offsetYPixel = -40f, offsetXPixel = 40f)
        drawAngle("Left Knee", PoseLandmark.LEFT_KNEE, offsetYPixel = -40f)
        drawAngle("Right Knee", PoseLandmark.RIGHT_KNEE, offsetYPixel = -40f)
        drawAngle("Left Hip", PoseLandmark.LEFT_HIP, offsetYPixel = -40f, offsetXPixel = -40f)
        drawAngle("Right Hip", PoseLandmark.RIGHT_HIP, offsetYPixel = -40f, offsetXPixel = 40f)
    }
}


//package com.example.exerciseapp.ui.components
//
//import android.util.Log
//import androidx.camera.core.CameraSelector
//import androidx.compose.foundation.Canvas
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.PointMode
//import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.sp
//import com.google.mlkit.vision.pose.Pose
//import com.google.mlkit.vision.pose.PoseLandmark
//import kotlin.math.min
//
//@Composable
//fun PoseOverlay(
//    pose: Pose?,
//    imageWidth: Int, // Width of the image frame processed by ML Kit
//    imageHeight: Int, // Height of the image frame processed by ML Kit
//    imageRotation: Int, // Rotation of the image frame (0, 90, 180, 270 degrees)
//    lensFacing: Int, // Camera lens facing (CameraSelector.LENS_FACING_FRONT or _BACK)
//    jointAngles: Map<String, Double?> = emptyMap(),
//    modifier: Modifier = Modifier
//) {
//    val density = LocalDensity.current
//
//    Canvas(modifier = modifier) {
//        if (imageWidth <= 0 || imageHeight <= 0) {
//            Log.w("PoseOverlay", "Image dimensions are invalid: $imageWidth x $imageHeight. Not drawing overlay.")
//            return@Canvas
//        }
//
//        val canvasWidth = size.width
//        val canvasHeight = size.height
//
//        // Determine the effective image dimensions after considering rotation.
//        // ML Kit's coordinates are based on the original (potentially rotated) image.
//        // We need to map them to the preview's (displayed) orientation.
//        val effectiveImageWidth: Int
//        val effectiveImageHeight: Int
//
//        when (imageRotation) {
//            90, 270 -> {
//                effectiveImageWidth = imageHeight // Width and height are swapped for 90/270 degrees
//                effectiveImageHeight = imageWidth
//            }
//            else -> {
//                effectiveImageWidth = imageWidth
//                effectiveImageHeight = imageHeight
//            }
//        }
//
//        // Calculate the scale factor that preserves the aspect ratio and fits the image within the canvas.
//        // This MUST match PreviewView.ScaleType.FIT_CENTER or similar behavior set in CameraPreviewView.
//        val scaleX = canvasWidth / effectiveImageWidth.toFloat()
//        val scaleY = canvasHeight / effectiveImageHeight.toFloat()
//        val scaleFactor = min(scaleX, scaleY) // Use min to fit the entire image
//
//        // Calculate the actual scaled dimensions of the image when fitted within the canvas
//        val scaledImageWidth = effectiveImageWidth * scaleFactor
//        val scaledImageHeight = effectiveImageHeight * scaleFactor
//
//        // Calculate offsets to center the scaled image (this creates letterboxing/pillarboxing)
//        val offsetX = (canvasWidth - scaledImageWidth) / 2
//        val offsetY = (canvasHeight - scaledImageHeight) / 2
//
//        Log.d("PoseOverlay", "Canvas: ${canvasWidth}x${canvasHeight}, Original ImageProxy: ${imageWidth}x${imageHeight}, Rotation: $imageRotation")
//        Log.d("PoseOverlay", "Effective Image (after rotation consideration): ${effectiveImageWidth}x${effectiveImageHeight}")
//        Log.d("PoseOverlay", "ScaleFactor: $scaleFactor, ScaledImage: ${scaledImageWidth}x${scaledImageHeight}, Offset: ${offsetX},${offsetY}")
//
//
//        // Helper function to transform a landmark's coordinates from the ML Kit image space
//        // to the Composables Canvas space.
//        fun PoseLandmark.toOffset(): Offset {
//            var transformedX = position.x
//            var transformedY = position.y
//
//            // Step 1: Adjust ML Kit coordinates based on the ImageProxy's rotation.
//            // This maps the landmark's coordinates from the original (possibly rotated)
//            // image frame to the effective (displayed) image frame.
//            when (imageRotation) {
//                0 -> {
//                    // No change needed
//                }
//                90 -> {
//                    // X becomes Y, Y becomes (original_width - X)
//                    val tempX = transformedX
//                    transformedX = transformedY
//                    transformedY = imageWidth - tempX
//                }
//                180 -> {
//                    // X becomes (original_width - X), Y becomes (original_height - Y)
//                    transformedX = imageWidth - transformedX
//                    transformedY = imageHeight - transformedY
//                }
//                270 -> {
//                    // X becomes (original_height - Y), Y becomes X
//                    val tempX = transformedX
//                    transformedX = imageHeight - transformedY
//                    transformedY = tempX
//                }
//            }
//
//            // Step 2: Apply horizontal mirroring if using the front camera.
//            // Front camera images are typically mirrored horizontally (like a mirror reflection).
//            // Rear camera images are not mirrored.
//            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//                // Mirror horizontally across the center of the *effective* image width
//                transformedX = effectiveImageWidth - transformedX
//            }
//
//            // Step 3: Apply scaling and then add the centering offset to get canvas coordinates.
//            return Offset(
//                x = transformedX * scaleFactor + offsetX,
//                y = transformedY * scaleFactor + offsetY
//            )
//        }
//
//        // Helper function to draw a line between two landmarks.
//        fun connect(start: PoseLandmark?, end: PoseLandmark?, color: Color, strokeWidth: Float = 8f) {
//            val startPos = start?.toOffset()
//            val endPos = end?.toOffset()
//            if (startPos != null && endPos != null) {
//                drawLine(
//                    color = color,
//                    start = startPos,
//                    end = endPos,
//                    strokeWidth = strokeWidth
//                )
//            }
//        }
//
//        // Check if a pose is detected before attempting to draw
//        pose?.let { currentPose ->
//            Log.d("PoseOverlay", "Pose detected with ${currentPose.allPoseLandmarks.size} landmarks. Drawing skeleton.")
//            if (currentPose.allPoseLandmarks.isEmpty()) {
//                Log.w("PoseOverlay", "Pose detected but no landmarks found. Nothing to draw.")
//            }
//
//            // Draw landmarks as points
//            val landmarkPoints = currentPose.allPoseLandmarks.map { it.toOffset() }
//            drawPoints(
//                points = landmarkPoints,
//                pointMode = PointMode.Points,
//                color = Color.White,
//                strokeWidth = 12f
//            )
//
//            val landmarks = currentPose.allPoseLandmarks.associateBy { it.landmarkType }
//
//            // Define colors for different body parts for better visualization
//            val faceColor = Color.Yellow
//            val torsoColor = Color.Green
//            val leftArmColor = Color.Red
//            val rightArmColor = Color.Blue
//            val leftLegColor = Color.Red
//            val rightLegColor = Color.Blue
//
//            // --- Draw Skeleton Connections ---
//
//            // Face connections
//            connect(landmarks[PoseLandmark.LEFT_MOUTH], landmarks[PoseLandmark.RIGHT_MOUTH], faceColor)
//            connect(landmarks[PoseLandmark.LEFT_EYE_INNER], landmarks[PoseLandmark.LEFT_EYE], faceColor)
//            connect(landmarks[PoseLandmark.LEFT_EYE], landmarks[PoseLandmark.LEFT_EYE_OUTER], faceColor)
//            connect(landmarks[PoseLandmark.LEFT_EYE_OUTER], landmarks[PoseLandmark.LEFT_EAR], faceColor)
//            connect(landmarks[PoseLandmark.RIGHT_EYE_INNER], landmarks[PoseLandmark.RIGHT_EYE], faceColor)
//            connect(landmarks[PoseLandmark.RIGHT_EYE], landmarks[PoseLandmark.RIGHT_EYE_OUTER], faceColor)
//            connect(landmarks[PoseLandmark.RIGHT_EYE_OUTER], landmarks[PoseLandmark.RIGHT_EAR], faceColor)
//            connect(landmarks[PoseLandmark.LEFT_EAR], landmarks[PoseLandmark.LEFT_SHOULDER], torsoColor) // Connect ear to shoulder
//            connect(landmarks[PoseLandmark.RIGHT_EAR], landmarks[PoseLandmark.RIGHT_SHOULDER], torsoColor) // Connect ear to shoulder
//
//
//            // Torso connections
//            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.RIGHT_SHOULDER], torsoColor)
//            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)
//            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_HIP], torsoColor)
//            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)
//
//            // Left Arm and Hand connections
//            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW], leftArmColor)
//            connect(landmarks[PoseLandmark.LEFT_ELBOW], landmarks[PoseLandmark.LEFT_WRIST], leftArmColor)
//            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_THUMB], leftArmColor, 4f)
//            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_PINKY], leftArmColor, 4f)
//            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor, 4f)
//            connect(landmarks[PoseLandmark.LEFT_PINKY], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor, 4f)
//
//            // Right Arm and Hand connections
//            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW], rightArmColor)
//            connect(landmarks[PoseLandmark.RIGHT_ELBOW], landmarks[PoseLandmark.RIGHT_WRIST], rightArmColor)
//            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_THUMB], rightArmColor, 4f)
//            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_PINKY], rightArmColor, 4f)
//            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor, 4f)
//            connect(landmarks[PoseLandmark.RIGHT_PINKY], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor, 4f)
//
//            // Left Leg and Foot connections
//            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.LEFT_KNEE], leftLegColor)
//            connect(landmarks[PoseLandmark.LEFT_KNEE], landmarks[PoseLandmark.LEFT_ANKLE], leftLegColor)
//            connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_HEEL], leftLegColor, 4f)
//            connect(landmarks[PoseLandmark.LEFT_HEEL], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor, 4f)
//            connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor, 4f)
//
//
//            // Right Leg and Foot connections
//            connect(landmarks[PoseLandmark.RIGHT_HIP], landmarks[PoseLandmark.RIGHT_KNEE], rightLegColor)
//            connect(landmarks[PoseLandmark.RIGHT_KNEE], landmarks[PoseLandmark.RIGHT_ANKLE], rightLegColor)
//            connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_HEEL], rightLegColor, 4f)
//            connect(landmarks[PoseLandmark.RIGHT_HEEL], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor, 4f)
//            connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor, 4f)
//
//
//            // --- Drawing Joint Angles ---
//            val textPaint = android.graphics.Paint().apply {
//                color = android.graphics.Color.WHITE
//                textSize = with(density) { 16.sp.toPx() } // Convert SP to pixels for Paint
//                textAlign = android.graphics.Paint.Align.CENTER
//            }
//
//            fun drawAngle(jointName: String, landmarkType: Int, offsetXPixel: Float = 0f, offsetYPixel: Float = 0f) {
//                val angle = jointAngles[jointName]
//                val landmark = landmarks[landmarkType]
//                if (angle != null && landmark != null) {
//                    val offset = landmark.toOffset() // Uses the local toOffset()
//                    val angleText = "%.1f°".format(angle)
//                    drawIntoCanvas {
//                        it.nativeCanvas.drawText(
//                            angleText,
//                            offset.x + offsetXPixel,
//                            offset.y + offsetYPixel,
//                            textPaint
//                        )
//                    }
//                }
//            }
//
//            // Draw angles near their respective joints
//            drawAngle("Left Elbow", PoseLandmark.LEFT_ELBOW, offsetYPixel = -40f)
//            drawAngle("Right Elbow", PoseLandmark.RIGHT_ELBOW, offsetYPixel = -40f)
//            drawAngle("Left Shoulder", PoseLandmark.LEFT_SHOULDER, offsetYPixel = -40f, offsetXPixel = -40f)
//            drawAngle("Right Shoulder", PoseLandmark.RIGHT_SHOULDER, offsetYPixel = -40f, offsetXPixel = 40f)
//            drawAngle("Left Knee", PoseLandmark.LEFT_KNEE, offsetYPixel = -40f)
//            drawAngle("Right Knee", PoseLandmark.RIGHT_KNEE, offsetYPixel = -40f)
//            drawAngle("Left Hip", PoseLandmark.LEFT_HIP, offsetYPixel = -40f, offsetXPixel = -40f)
//            drawAngle("Right Hip", PoseLandmark.RIGHT_HIP, offsetYPixel = -40f, offsetXPixel = 40f)
//
//            // --- DEBUGGING VISUAL CUES ---
//            // Uncomment this block to visualize the processed image boundaries and alignment.
//            // The green rectangle should align perfectly with your camera preview on screen.
//            /*
//            val debugPoints = listOf(
//                Offset(0f, 0f),                                       // Top-left of effective image
//                Offset(effectiveImageWidth.toFloat(), 0f),                     // Top-right of effective image
//                Offset(0f, effectiveImageHeight.toFloat()),                    // Bottom-left of effective image
//                Offset(effectiveImageWidth.toFloat(), effectiveImageHeight.toFloat()),  // Bottom-right of effective image
//                Offset(effectiveImageWidth / 2f, effectiveImageHeight / 2f)             // Center of effective image
//            ).map { debugPoint ->
//                // Apply the same transformation logic as toOffset() for debugging purposes
//                var debugX = debugPoint.x
//                val debugY = debugPoint.y
//
//                // Mirror debug points if front camera
//                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//                    debugX = effectiveImageWidth - debugX
//                }
//                Offset(debugX * scaleFactor + offsetX, debugY * scaleFactor + offsetY)
//            }
//
//            drawPoints(
//                points = debugPoints,
//                pointMode = PointMode.Points,
//                color = Color.Cyan, // Distinct color
//                strokeWidth = 30f // Make them large enough to see
//            )
//
//            // Draw a rectangle outlining the exact area where the image is displayed
//            drawLine(color = Color.Green, start = Offset(offsetX, offsetY), end = Offset(offsetX + scaledImageWidth, offsetY), strokeWidth = 4f)
//            drawLine(color = Color.Green, start = Offset(offsetX + scaledImageWidth, offsetY), end = Offset(offsetX + scaledImageWidth, offsetY + scaledImageHeight), strokeWidth = 4f)
//            drawLine(color = Color.Green, start = Offset(offsetX + scaledImageWidth, offsetY + scaledImageHeight), end = Offset(offsetX, offsetY + scaledImageHeight), strokeWidth = 4f)
//            drawLine(color = Color.Green, start = Offset(offsetX, offsetY + scaledImageHeight), end = Offset(offsetX, offsetY), strokeWidth = 4f)
//            */
//            // --- END DEBUGGING VISUAL CUES ---
//        }
//    }
//}