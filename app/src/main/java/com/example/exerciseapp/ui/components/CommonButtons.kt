// app/src/main/java/com/example/exerciseapp/ui/components/CommonButtons.kt
package com.example.exerciseapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StartStopButtons(
    isTracking: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onStartClick,
            enabled = !isTracking,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Text("START", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onStopClick,
            enabled = isTracking,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        ) {
            Text("STOP", fontSize = 20.sp)
        }
    }
}