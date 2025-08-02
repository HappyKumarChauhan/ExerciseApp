// app/src/main/java/com/example/exerciseapp/ui/navigation/AppNavGraph.kt
package com.example.exerciseapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.exerciseapp.ui.screens.ExerciseSelectionScreen
import com.example.exerciseapp.ui.screens.FreeMovementTrackingScreen
import com.example.exerciseapp.ui.screens.SitToStandCalibrationScreen
import com.example.exerciseapp.ui.screens.SitToStandExerciseScreen
import com.example.exerciseapp.ui.screens.WelcomeScreen
import com.example.exerciseapp.ui.screens.ExerciseCompletedScreen // Import the new screen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        composable("exercise_selection") {
            ExerciseSelectionScreen(navController = navController)
        }
        composable("sit_to_stand_calibration") {
            SitToStandCalibrationScreen(navController = navController)
        }
        // Route for Sit to Stand Exercise, accepting angles as arguments
        composable(
            route = "sit_to_stand_exercise/{standingAngle}/{sittingAngle}",
            arguments = listOf(
                navArgument("standingAngle") { type = NavType.FloatType },
                navArgument("sittingAngle") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val standingAngle = backStackEntry.arguments?.getFloat("standingAngle") ?: 0f
            val sittingAngle = backStackEntry.arguments?.getFloat("sittingAngle") ?: 0f
            SitToStandExerciseScreen(
                navController = navController,
                standingAngle = standingAngle.toDouble(),
                sittingAngle = sittingAngle.toDouble()
            )
        }
        composable("free_movement_tracking") {
            FreeMovementTrackingScreen(navController = navController)
        }
        // New composable for the Exercise Completed screen
        composable(
            route = "exercise_completed/{repCount}",
            arguments = listOf(navArgument("repCount") { type = NavType.IntType })
        ) { backStackEntry ->
            val repCount = backStackEntry.arguments?.getInt("repCount") ?: 0
            ExerciseCompletedScreen(navController, repCount)
        }
    }
}