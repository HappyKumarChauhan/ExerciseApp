// BoundaryStatus.kt
package com.example.exerciseapp // Make sure this matches your package

enum class BoundaryStatus {
    GREEN,  // User is in ideal position (initial setup) or was and is still broadly visible.
    RED     // No pose detected, or user is not in the correct initial setup, or user is too far/too close/completely off-camera.
}