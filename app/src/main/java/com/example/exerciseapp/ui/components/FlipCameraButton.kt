// In your ui.components/FlipCameraButton.kt (or wherever it's defined)
package com.example.exerciseapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlipCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier // <--- ADD THIS MODIFIER PARAMETER
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier // <--- APPLY THE PASSED MODIFIER HERE
    ) {
        Icon(Icons.Filled.Cameraswitch, "Flip Camera")
    }
}

//package com.example.exerciseapp.ui.components
//
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Cameraswitch
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.Icon
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//@Composable
//fun FlipCameraButton(
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier // Accepts a Modifier to allow positioning from parent
//) {
//    FloatingActionButton(
//        onClick = onClick,
//        modifier = modifier // Apply the passed modifier
//    ) {
//        Icon(Icons.Filled.Cameraswitch, contentDescription = "Flip Camera")
//    }
//}