package com.example.exerciseapp

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.exerciseapp.ml.PoseAnalyzer
import com.example.exerciseapp.ui.theme.ExerciseAppTheme
import com.google.mlkit.vision.pose.Pose
import android.Manifest.permission.CAMERA
import androidx.compose.foundation.layout.Box
import com.example.exerciseapp.ui.components.CameraPreviewView
import com.example.exerciseapp.ui.components.PoseOverlay

class MainActivity : ComponentActivity() {

    private val poseAnalyzer = PoseAnalyzer()

    private val _poseState = mutableStateOf<Pose?>(null)
    val poseState: State<Pose?> = _poseState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission()
        enableEdgeToEdge()
        setContent {
            ExerciseAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreviewView { imageProxy ->
                        poseAnalyzer.detectPose(imageProxy) { pose ->
                            _poseState.value = pose
                        }
                    }

                    PoseOverlay(pose = poseState.value, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) // Changed from Manifest.permission.CAMERA
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA), // Changed from Manifest.permission.CAMERA
                100
            )
        }
    }

}

