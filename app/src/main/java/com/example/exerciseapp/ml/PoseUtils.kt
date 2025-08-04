package com.example.exerciseapp.ml

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
     * Calculates the angle of the arm relative to the vertical axis.
     * This is a more robust measure for arm elevation than a joint angle.
     * Hands down: angle will be close to 180°.
     * Hands up: angle will be close to 0°.
     *
     * @param pose The detected pose from ML Kit.
     * @param shoulder The shoulder landmark.
     * @param wrist The wrist landmark.
     * @return The angle in degrees (0-180), or null if any landmark is missing.
     */
    fun calculateVerticalAngle(pose: Pose, shoulder: Int, wrist: Int): Double? {
        val shoulderLandmark = pose.getPoseLandmark(shoulder)
        val wristLandmark = pose.getPoseLandmark(wrist)

        if (shoulderLandmark == null || wristLandmark == null) {
            return null
        }

        // Vector for the arm (shoulder to wrist)
        val armVectorX = wristLandmark.position.x - shoulderLandmark.position.x
        val armVectorY = wristLandmark.position.y - shoulderLandmark.position.y

        // A vertical vector pointing straight up. Y-axis is typically inverted in image coordinates.
        // So, a vector pointing "up" has a negative Y component.
        val verticalVectorX = 0f
        val verticalVectorY = -1f

        // Calculate the dot product
        val dotProduct = (armVectorX * verticalVectorX) + (armVectorY * verticalVectorY)

        // Calculate the magnitude of each vector
        val armMagnitude = sqrt((armVectorX * armVectorX + armVectorY * armVectorY).toDouble())
        val verticalMagnitude = sqrt((verticalVectorX * verticalVectorX + verticalVectorY * verticalVectorY).toDouble())

        // Prevent division by zero
        if (armMagnitude == 0.0 || verticalMagnitude == 0.0) {
            return null
        }

        // Calculate the cosine of the angle using the dot product formula
        val cosAngle = dotProduct / (armMagnitude * verticalMagnitude)

        // Ensure the value is within the valid range for acos [-1, 1]
        val clampedCosAngle = min(1.0, max(-1.0, cosAngle))

        // Calculate the angle in radians, then convert to degrees
        val angleInRadians = acos(clampedCosAngle)
        val angleInDegrees = Math.toDegrees(angleInRadians)

        return angleInDegrees
    }

    /**
     * Helper function to calculate Euclidean distance between two points.
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
}
/**
 * Returns a list of pairs representing the connections between pose landmarks.
 * This is used for drawing the skeleton on the overlay.
 */
fun getPoseConnections(): List<Pair<Int, Int>> {
    // ... rest of the code remains the same ...
    return listOf(
        // Torso
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
        Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
        Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),

        // Left Arm
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
        Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
        Pair(PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_PINKY),
        Pair(PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_INDEX),
        Pair(PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_THUMB),
        Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_SHOULDER),

        // Right Arm
        Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
        Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
        Pair(PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_PINKY),
        Pair(PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_INDEX),
        Pair(PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_THUMB),
        Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_SHOULDER),

        // Left Leg
        Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
        Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
        Pair(PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_FOOT_INDEX),

        // Right Leg
        Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
        Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
        Pair(PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_FOOT_INDEX),

        // Head and Face
        Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_EAR),
        Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_EAR),
        Pair(PoseLandmark.LEFT_EAR, PoseLandmark.LEFT_EYE_OUTER),
        Pair(PoseLandmark.RIGHT_EAR, PoseLandmark.RIGHT_EYE_OUTER),
        Pair(PoseLandmark.LEFT_EYE_OUTER, PoseLandmark.LEFT_EYE),
        Pair(PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EYE_INNER),
        Pair(PoseLandmark.LEFT_EYE_INNER, PoseLandmark.NOSE),
        Pair(PoseLandmark.NOSE, PoseLandmark.RIGHT_EYE_INNER),
        Pair(PoseLandmark.RIGHT_EYE_INNER, PoseLandmark.RIGHT_EYE),
        Pair(PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EYE_OUTER),
        Pair(PoseLandmark.LEFT_MOUTH, PoseLandmark.RIGHT_MOUTH),
        Pair(PoseLandmark.LEFT_MOUTH, PoseLandmark.NOSE),
        Pair(PoseLandmark.RIGHT_MOUTH, PoseLandmark.NOSE)
    )
}