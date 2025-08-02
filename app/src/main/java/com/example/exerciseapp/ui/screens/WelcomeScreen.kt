// app/src/main/java/com/example/exerciseapp/ui/screens/WelcomeScreen.kt
package com.example.exerciseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.exerciseapp.R // Make sure to import R (your resources)
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(navController: NavController) {
    // Auto-swipe after 1.5 seconds
    LaunchedEffect(Unit) {
        delay(1500) // 1.5 seconds
        navController.navigate("exercise_selection") {
            // Pop up to the start destination to avoid going back to welcome screen
            popUpTo("welcome") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ripple_healthcare_logo), // Replace with your app logo
            contentDescription = "App Logo",
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to Exercise Tracker!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your personal AI fitness coach.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(navController = rememberNavController())
}