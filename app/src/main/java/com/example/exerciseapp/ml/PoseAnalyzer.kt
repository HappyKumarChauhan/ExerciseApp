package com.example.exerciseapp.ml

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.Pose

class PoseAnalyzer {

    private val poseDetector: PoseDetector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()

        PoseDetection.getClient(options)

    }

    @OptIn(ExperimentalGetImage::class)
    fun detectPose(imageProxy: ImageProxy, onPoseDetected: (Pose) -> Unit) {
        val mediaImage = imageProxy.image ?: return

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                onPoseDetected(pose)
            }
            .addOnFailureListener {
                Log.e("PoseAnalyzer", "Pose detection failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
