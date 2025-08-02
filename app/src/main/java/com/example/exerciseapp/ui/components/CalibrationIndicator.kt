// app/src/main/java/com/example/exerciseapp/ui/components/CalibrationIndicator.kt
package com.example.exerciseapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalibrationIndicator(message: String, isVisible: Boolean, backgroundColor: Color = Color.Black.copy(alpha = 0.6f)) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}