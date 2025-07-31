//package com.example.exerciseapp.ui.components
//
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
//    imageWidth: Int,
//    imageHeight: Int,
//    lensFacing: Int,
//    jointAngles: Map<String, Double?> = emptyMap(), // Accept joint angles
//    modifier: Modifier = Modifier
//) {
//    val density = LocalDensity.current
//
//    Canvas(modifier = modifier) {
//        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas
//
//        val canvasWidth = size.width
//        val canvasHeight = size.height
//
//        val scaleX = canvasWidth / imageWidth.toFloat()
//        val scaleY = canvasHeight / imageHeight.toFloat()
//
//        val scaleFactor = min(scaleX, scaleY)
//
//        val scaledImageWidth = imageWidth * scaleFactor
//        val scaledImageHeight = imageHeight * scaleFactor
//
//        val offsetX = (canvasWidth - scaledImageWidth) / 2
//        val offsetY = (canvasHeight - scaledImageHeight) / 2
//
//        fun PoseLandmark.toOffset(): Offset {
//            var x = position.x
//            val y = position.y
//
//            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
//                x = imageWidth - x
//            }
//
//            return Offset(
//                x = x * scaleFactor + offsetX,
//                y = y * scaleFactor + offsetY
//            )
//        }
//
//        fun connect(start: PoseLandmark?, end: PoseLandmark?, color: Color) {
//            val startPos = start?.toOffset()
//            val endPos = end?.toOffset()
//            if (startPos != null && endPos != null) {
//                drawLine(
//                    color = color,
//                    start = startPos,
//                    end = endPos,
//                    strokeWidth = 12f
//                )
//            }
//        }
//
//        val landmarkPoints = pose?.allPoseLandmarks?.map { it.toOffset() } ?: emptyList()
//        drawPoints(
//            points = landmarkPoints,
//            pointMode = PointMode.Points,
//            color = Color.White,
//            strokeWidth = 12f
//        )
//
//        val landmarks = pose?.allPoseLandmarks?.associateBy { it.landmarkType } ?: emptyMap()
//
//        val faceColor = Color.White
//        val torsoColor = Color.White
//        val leftArmColor = Color.White
//        val rightArmColor = Color.White
//        val leftLegColor = Color.White
//        val rightLegColor = Color.White
//
//        // Face
//        connect(landmarks[PoseLandmark.LEFT_MOUTH], landmarks[PoseLandmark.RIGHT_MOUTH], faceColor)
//        connect(landmarks[PoseLandmark.LEFT_EYE_INNER], landmarks[PoseLandmark.LEFT_EYE], faceColor)
//        connect(landmarks[PoseLandmark.LEFT_EYE], landmarks[PoseLandmark.LEFT_EYE_OUTER], faceColor)
//        connect(landmarks[PoseLandmark.LEFT_EYE_OUTER], landmarks[PoseLandmark.LEFT_EAR], faceColor)
//        connect(landmarks[PoseLandmark.RIGHT_EYE_INNER], landmarks[PoseLandmark.RIGHT_EYE], faceColor)
//        connect(landmarks[PoseLandmark.RIGHT_EYE], landmarks[PoseLandmark.RIGHT_EYE_OUTER], faceColor)
//        connect(landmarks[PoseLandmark.RIGHT_EYE_OUTER], landmarks[PoseLandmark.RIGHT_EAR], faceColor)
//
//        // Torso
//        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.RIGHT_SHOULDER], torsoColor)
//        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)
//        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_HIP], torsoColor)
//        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], torsoColor) // Connect hips for lower torso
//        connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_HIP], torsoColor)
//
//        // Left Arm and Hand
//        connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW], leftArmColor)
//        connect(landmarks[PoseLandmark.LEFT_ELBOW], landmarks[PoseLandmark.LEFT_WRIST], leftArmColor)
//        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_THUMB], leftArmColor)
//        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_PINKY], leftArmColor)
//        connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor)
//        connect(landmarks[PoseLandmark.LEFT_PINKY], landmarks[PoseLandmark.LEFT_INDEX], leftArmColor)
//
//        // Right Arm and Hand
//        connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW], rightArmColor)
//        connect(landmarks[PoseLandmark.RIGHT_ELBOW], landmarks[PoseLandmark.RIGHT_WRIST], rightArmColor)
//        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_THUMB], rightArmColor)
//        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_PINKY], rightArmColor)
//        connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor)
//        connect(landmarks[PoseLandmark.RIGHT_PINKY], landmarks[PoseLandmark.RIGHT_INDEX], rightArmColor)
//
//        // Left Leg and Foot
//        connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.LEFT_KNEE], leftLegColor)
//        connect(landmarks[PoseLandmark.LEFT_KNEE], landmarks[PoseLandmark.LEFT_ANKLE], leftLegColor)
//        connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_HEEL], leftLegColor)
//        connect(landmarks[PoseLandmark.LEFT_HEEL], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor)
//        connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_FOOT_INDEX], leftLegColor)
//
//        // Right Leg and Foot
//        connect(landmarks[PoseLandmark.RIGHT_HIP], landmarks[PoseLandmark.RIGHT_KNEE], rightLegColor)
//        connect(landmarks[PoseLandmark.RIGHT_KNEE], landmarks[PoseLandmark.RIGHT_ANKLE], rightLegColor)
//        connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_HEEL], rightLegColor)
//        connect(landmarks[PoseLandmark.RIGHT_HEEL], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor)
//        connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], rightLegColor)
//
//
//        // --- Drawing Joint Angles ---
//        val textPaint = android.graphics.Paint().apply {
//            color = android.graphics.Color.GREEN
//            textSize = with(density) { 14.sp.toPx() } // Adjust text size as needed
//            textAlign = android.graphics.Paint.Align.CENTER
//        }
//
//        // Helper to draw angle text near a joint landmark
//        fun drawAngle(jointName: String, landmarkType: Int, offsetXPixel: Float = 0f, offsetYPixel: Float = 0f) {
//            val angle = jointAngles[jointName]
//            val landmark = landmarks[landmarkType]
//            if (angle != null && landmark != null) {
//                val offset = landmark.toOffset()
//                // Only show the angle value, not the joint name
//                val angleText = "%.1f°".format(angle) // Format to one decimal place and add degree symbol
//                drawIntoCanvas {
//                    it.nativeCanvas.drawText(
//                        angleText,
//                        offset.x + offsetXPixel,
//                        offset.y + offsetYPixel,
//                        textPaint
//                    )
//                }
//            }
//        }
//
//        // Call drawAngle for each joint you want to display
//        // Adjust offsets as needed for better visibility
//        drawAngle("Left Elbow", PoseLandmark.LEFT_ELBOW, offsetYPixel = -40f)
//        drawAngle("Right Elbow", PoseLandmark.RIGHT_ELBOW, offsetYPixel = -40f)
//        drawAngle("Left Shoulder", PoseLandmark.LEFT_SHOULDER, offsetYPixel = -40f, offsetXPixel = -40f)
//        drawAngle("Right Shoulder", PoseLandmark.RIGHT_SHOULDER, offsetYPixel = -40f, offsetXPixel = 40f)
//        drawAngle("Left Knee", PoseLandmark.LEFT_KNEE, offsetYPixel = -40f)
//        drawAngle("Right Knee", PoseLandmark.RIGHT_KNEE, offsetYPixel = -40f)
//        drawAngle("Left Hip", PoseLandmark.LEFT_HIP, offsetYPixel = -40f, offsetXPixel = -40f)
//        drawAngle("Right Hip", PoseLandmark.RIGHT_HIP, offsetYPixel = -40f, offsetXPixel = 40f)
//    }
//}




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
import com.example.exerciseapp.BoundaryStatus // Import the enum
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.min

@Composable
fun PoseOverlay(
    pose: Pose?,
    imageWidth: Int,
    imageHeight: Int,
    lensFacing: Int,
    jointAngles: Map<String, Double?> = emptyMap(),
    boundaryStatus: BoundaryStatus,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        // --- Draw Corner Indicators ---
        val cornerLength = 80f // Length of the corner lines in pixels
        val cornerStrokeWidth = 12f
        val boundaryColor = when (boundaryStatus) {
            BoundaryStatus.GREEN -> Color.Green
            BoundaryStatus.RED -> Color.Red
        }

        // Top-Left Corner
        drawLine(boundaryColor, Offset(0f, cornerLength), Offset(0f, 0f), cornerStrokeWidth)
        drawLine(boundaryColor, Offset(0f, 0f), Offset(cornerLength, 0f), cornerStrokeWidth)

        // Top-Right Corner
        drawLine(boundaryColor, Offset(size.width - cornerLength, 0f), Offset(size.width, 0f), cornerStrokeWidth)
        drawLine(boundaryColor, Offset(size.width, 0f), Offset(size.width, cornerLength), cornerStrokeWidth)

        // Bottom-Left Corner
        drawLine(boundaryColor, Offset(0f, size.height - cornerLength), Offset(0f, size.height), cornerStrokeWidth)
        drawLine(boundaryColor, Offset(0f, size.height), Offset(cornerLength, size.height), cornerStrokeWidth)

        // Bottom-Right Corner
        drawLine(boundaryColor, Offset(size.width - cornerLength, size.height), Offset(size.width, size.height), cornerStrokeWidth)
        drawLine(boundaryColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), cornerStrokeWidth)


        // --- Drawing Logic for Skeleton (only if GREEN) ---
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

        // Helper function to transform ML Kit PoseLandmark coordinates to display coordinates
        fun PoseLandmark.toDisplayOffset(): Offset {
            var x = position.x
            val y = position.y

            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                // Mirror the X coordinate for front camera to match screen display
                x = imageWidth - x
            }
            return Offset(x * scaleFactor + offsetX, y * scaleFactor + offsetY)
        }

        fun connect(start: PoseLandmark?, end: PoseLandmark?, color: Color) {
            val startPos = start?.toDisplayOffset()
            val endPos = end?.toDisplayOffset()
            if (startPos != null && endPos != null) {
                drawLine(color = color, start = startPos, end = endPos, strokeWidth = 6f)
            }
        }

        // Only draw the skeleton and angles if the boundary status is GREEN.
        if (pose != null && boundaryStatus == BoundaryStatus.GREEN) {
            // Draw Skeleton Landmarks
            val landmarkPoints = pose.allPoseLandmarks.map { it.toDisplayOffset() }
            drawPoints(points = landmarkPoints, pointMode = PointMode.Points, color = Color.Green, strokeWidth = 12f)

            val landmarks = pose.allPoseLandmarks.associateBy { it.landmarkType }
            val skeletonColor = Color.White

            // Face connections
            connect(landmarks[PoseLandmark.LEFT_MOUTH], landmarks[PoseLandmark.RIGHT_MOUTH], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE_INNER], landmarks[PoseLandmark.LEFT_EYE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE], landmarks[PoseLandmark.LEFT_EYE_OUTER], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE_OUTER], landmarks[PoseLandmark.LEFT_EAR], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE_INNER], landmarks[PoseLandmark.RIGHT_EYE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE], landmarks[PoseLandmark.RIGHT_EYE_OUTER], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE_OUTER], landmarks[PoseLandmark.RIGHT_EAR], skeletonColor)

            // Torso connections
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.RIGHT_SHOULDER], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_HIP], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_HIP], skeletonColor)

            // Left Arm and Hand connections
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_ELBOW], landmarks[PoseLandmark.LEFT_WRIST], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_THUMB], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_PINKY], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_WRIST], landmarks[PoseLandmark.LEFT_INDEX], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_PINKY], landmarks[PoseLandmark.LEFT_INDEX], skeletonColor)

            // Right Arm and Hand connections
            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_ELBOW], landmarks[PoseLandmark.RIGHT_WRIST], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_THUMB], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_PINKY], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_WRIST], landmarks[PoseLandmark.RIGHT_INDEX], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_PINKY], landmarks[PoseLandmark.RIGHT_INDEX], skeletonColor)

            // Left Leg and Foot connections
            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.LEFT_KNEE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_KNEE], landmarks[PoseLandmark.LEFT_ANKLE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_HEEL], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_HEEL], landmarks[PoseLandmark.LEFT_FOOT_INDEX], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_FOOT_INDEX], skeletonColor)

            // Right Leg and Foot connections
            connect(landmarks[PoseLandmark.RIGHT_HIP], landmarks[PoseLandmark.RIGHT_KNEE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_KNEE], landmarks[PoseLandmark.RIGHT_ANKLE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_HEEL], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_HEEL], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], skeletonColor)

            // Drawing Joint Angles
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
                textSize = with(density) { 12.sp.toPx() } // Adjust text size as needed
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // Helper to draw angle text near a joint landmark
            fun drawAngle(jointName: String, landmarkType: Int, offsetXPixel: Float = 0f, offsetYPixel: Float = 0f) {
                val angle = jointAngles[jointName]
                val landmark = landmarks[landmarkType]
                if (angle != null && landmark != null) {
                    val offset = landmark.toDisplayOffset()
                    val angleText = "%.1f°".format(angle)
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
}