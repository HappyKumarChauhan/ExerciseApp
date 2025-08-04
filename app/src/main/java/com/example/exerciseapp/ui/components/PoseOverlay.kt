package com.example.exerciseapp.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exerciseapp.BoundaryStatus
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.min
import kotlin.math.max

@Composable
fun PoseOverlay(
    pose: Pose?,
    imageWidth: Int,
    imageHeight: Int,
    lensFacing: Int,
    jointAngles: Map<String, Double?> = emptyMap(),
    boundaryStatus: BoundaryStatus,
    personBoundingBox: Rect? = null,
    previewViewSize: IntSize,
    previewScaleType: PreviewView.ScaleType,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        // --- Draw Corner Indicators ---
        val cornerLength = 80f
        val cornerStrokeWidth = 12f
        val boundaryColor = when (boundaryStatus) {
            BoundaryStatus.GREEN -> Color.Green
            BoundaryStatus.RED -> Color.Red
            BoundaryStatus.YELLOW -> Color.Yellow
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

        // --- Drawing Logic for Skeleton and Angles ---
        if (imageWidth <= 0 || imageHeight <= 0 || previewViewSize.width <= 0 || previewViewSize.height <= 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val previewWidth = previewViewSize.width.toFloat()
        val previewHeight = previewViewSize.height.toFloat()

        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float

        if (previewScaleType == PreviewView.ScaleType.FILL_CENTER) {
            scaleX = max(previewWidth / imageWidth, previewHeight / imageHeight)
            scaleY = max(previewWidth / imageWidth, previewHeight / imageHeight)
            offsetX = (canvasWidth - imageWidth * scaleX) / 2
            offsetY = (canvasHeight - imageHeight * scaleY) / 2
        } else {
            scaleX = min(previewWidth / imageWidth, previewHeight / imageHeight)
            scaleY = min(previewWidth / imageWidth, previewHeight / imageHeight)
            offsetX = (canvasWidth - imageWidth * scaleX) / 2
            offsetY = (canvasHeight - imageHeight * scaleY) / 2
        }

        fun PoseLandmark.toDisplayOffset(): Offset {
            var x = position.x
            val y = position.y

            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                x = imageWidth - x
            }

            return Offset(x * scaleX + offsetX, y * scaleY + offsetY)
        }

        fun connect(start: PoseLandmark?, end: PoseLandmark?, color: Color) {
            val startPos = start?.toDisplayOffset()
            val endPos = end?.toDisplayOffset()
            if (startPos != null && endPos != null) {
                if (start.inFrameLikelihood > 0.1f && end.inFrameLikelihood > 0.1f) {
                    drawLine(color = color, start = startPos, end = endPos, strokeWidth = 6f)
                }
            }
        }

        if (pose != null) {
            val landmarks = pose.allPoseLandmarks.associateBy { it.landmarkType }
            val skeletonColor = Color.White
            val landmarkDotColor = Color.Green

            // Draw Skeleton Landmarks (dots)
            val landmarkPoints = landmarks.values.filter { it.inFrameLikelihood > 0.1f }.map { it.toDisplayOffset() }
            drawPoints(points = landmarkPoints, pointMode = PointMode.Points, color = landmarkDotColor, strokeWidth = 12f)

            // Draw all connections
            connect(landmarks[PoseLandmark.LEFT_MOUTH], landmarks[PoseLandmark.RIGHT_MOUTH], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE_INNER], landmarks[PoseLandmark.LEFT_EYE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE], landmarks[PoseLandmark.LEFT_EYE_OUTER], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_EYE_OUTER], landmarks[PoseLandmark.LEFT_EAR], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE_INNER], landmarks[PoseLandmark.RIGHT_EYE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE], landmarks[PoseLandmark.RIGHT_EYE_OUTER], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_EYE_OUTER], landmarks[PoseLandmark.RIGHT_EAR], skeletonColor)
            connect(landmarks[PoseLandmark.NOSE], landmarks[PoseLandmark.LEFT_EYE_INNER], skeletonColor)
            connect(landmarks[PoseLandmark.NOSE], landmarks[PoseLandmark.RIGHT_EYE_INNER], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.RIGHT_SHOULDER], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_HIP], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_HIP], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_ELBOW], landmarks[PoseLandmark.LEFT_WRIST], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_ELBOW], landmarks[PoseLandmark.RIGHT_WRIST], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.LEFT_KNEE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_KNEE], landmarks[PoseLandmark.LEFT_ANKLE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_HIP], landmarks[PoseLandmark.RIGHT_KNEE], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_KNEE], landmarks[PoseLandmark.RIGHT_ANKLE], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_ANKLE], landmarks[PoseLandmark.LEFT_HEEL], skeletonColor)
            connect(landmarks[PoseLandmark.LEFT_HEEL], landmarks[PoseLandmark.LEFT_FOOT_INDEX], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_ANKLE], landmarks[PoseLandmark.RIGHT_HEEL], skeletonColor)
            connect(landmarks[PoseLandmark.RIGHT_HEEL], landmarks[PoseLandmark.RIGHT_FOOT_INDEX], skeletonColor)


            // Drawing Joint Angles
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                textSize = with(density) { 16.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            fun drawAngle(jointName: String, landmarkType: Int, offsetXPixel: Float = 0f, offsetYPixel: Float = 0f) {
                val angle = jointAngles[jointName]
                val landmark = landmarks[landmarkType]
                if (angle != null && landmark != null && landmark.inFrameLikelihood > 0.1f) {
                    val offset = landmark.toDisplayOffset()
                    val angleText = "${jointName}: %.1f°".format(angle)
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

            // Displaying the calculated arm angles
            drawAngle("Left Arm", PoseLandmark.LEFT_SHOULDER, offsetYPixel = -40f, offsetXPixel = -40f)
            drawAngle("Right Arm", PoseLandmark.RIGHT_SHOULDER, offsetYPixel = -40f, offsetXPixel = 40f)

            personBoundingBox?.let { box ->
                val displayBoxLeft = box.left
                val displayBoxTop = box.top
                val displayBoxRight = box.right
                val displayBoxBottom = box.bottom

                drawRect(
                    color = Color.Cyan.copy(alpha = 0.5f),
                    topLeft = Offset(displayBoxLeft, displayBoxTop),
                    size = androidx.compose.ui.geometry.Size(displayBoxRight - displayBoxLeft, displayBoxBottom - displayBoxTop),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }
    }

    // Overlay text for "Adjust Position" when not GREEN
    if (boundaryStatus != BoundaryStatus.GREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Adjust Position",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ensure full body/relevant joints are visible and within the frame.",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PoseOverlayPreview() {
    androidx.compose.material3.MaterialTheme {
        PoseOverlay(
            pose = null,
            imageWidth = 720,
            imageHeight = 1280,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            jointAngles = mapOf("Left Arm" to 90.0),
            boundaryStatus = BoundaryStatus.RED,
            personBoundingBox = Rect(Offset(100f, 100f), Offset(300f, 500f)),
            modifier = Modifier.fillMaxSize(),
            previewViewSize = IntSize(1080, 1920),
            previewScaleType = PreviewView.ScaleType.FILL_CENTER
        )
    }
}