// app/src/main/java/com/example/exerciseapp/ui/screens/ExerciseCompletedScreen.kt
package com.example.exerciseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.exerciseapp.ui.theme.Purple40
import com.example.exerciseapp.ui.theme.Purple80

@Composable
fun ExerciseCompletedScreen(navController: NavController, repCount: Int) {
    val performanceMessage = getPerformanceMessage(repCount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Exercise Complete!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Purple40)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Reps",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = repCount.toString(),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = performanceMessage,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = { navController.popBackStack("welcome", inclusive = false) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple80,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start New Exercise", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getPerformanceMessage(repCount: Int): String {
    return when {
        repCount <= 4 -> "Poor: You can do better next time!"
        repCount <= 8 -> "Average: Solid effort, keep it up!"
        repCount <= 12 -> "Good: Great job, you're getting stronger!"
        else -> "Excellent: Outstanding performance! You're a pro."
    }
}