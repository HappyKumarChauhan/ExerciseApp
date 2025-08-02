// app/src/main/java/com/example/exerciseapp/ui/components/RepCounter.kt
package com.example.exerciseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RepCounter(reps: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Reps: $reps",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}