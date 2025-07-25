package com.example.exerciseapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.pose.Pose

@Composable
fun PoseOverlay(pose: Pose?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        pose?.allPoseLandmarks?.forEach { landmark ->
            val point = Offset(landmark.position.x, landmark.position.y)
            drawCircle(
                color = Color.Red,
                radius = 10f,
                center = point
            )
        }
    }
}
