// app/src/main/java/com/example/exerciseapp/ui/components/CountdownTimer.kt
package com.example.exerciseapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownTimer(time: Int, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = time,
        transitionSpec = {
            if (targetState > initialState) {
                // Counting up
                slideInVertically { height -> height } + fadeIn() with
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                // Counting down
                slideInVertically { height -> -height } + fadeIn() with
                        slideOutVertically { height -> height } + fadeOut()
            }.using(
                SizeTransform(clip = false)
            )
        }, label = "Countdown Timer"
    ) { targetCount ->
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = targetCount.toString(),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}