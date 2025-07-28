package com.example.exerciseapp.ml

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.atan2
import kotlin.math.sqrt

object PoseUtils {

    /**
     * Calculates the angle between three pose landmarks, with the middle landmark being the vertex.
     * The angle is returned in degrees.
     *
     * @param pose The detected pose from ML Kit.
     * @param landmarkType1 The first landmark.
     * @param landmarkType2 The middle landmark (vertex of the angle).
     * @param landmarkType3 The third landmark.
     * @return The angle in degrees, or null if any landmark is missing.
     */
    fun calculateAngle(
        pose: Pose,
        landmarkType1: Int,
        landmarkType2: Int, // Vertex
        landmarkType3: Int
    ): Double? {
        val l1 = pose.getPoseLandmark(landmarkType1)
        val l2 = pose.getPoseLandmark(landmarkType2)
        val l3 = pose.getPoseLandmark(landmarkType3)

        if (l1 == null || l2 == null || l3 == null) {
            return null
        }

        val angleInRadians = atan2(
            (l3.position.y - l2.position.y).toDouble(),
            (l3.position.x - l2.position.x).toDouble()
        ) - atan2(
            (l1.position.y - l2.position.y).toDouble(),
            (l1.position.x - l2.position.x).toDouble()
        )

        val angle = Math.toDegrees(angleInRadians)

        return if (angle < 0) angle + 360 else angle
    }

    /**
     * Helper function to calculate Euclidean distance between two points.
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
}
