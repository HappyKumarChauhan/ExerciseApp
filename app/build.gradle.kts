plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.exerciseapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.exerciseapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true // ✅ Required for Guava and Java 8+
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.pose.detection.common)
    implementation(libs.pose.detection.accurate)
    implementation(libs.pose.detection)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation("androidx.camera:camera-camera2:1.3.0")

    implementation(libs.androidx.runtime)
    implementation("androidx.compose.material:material-icons-extended")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // ✅ Required for ListenableFuture (used by CameraX)
    implementation("com.google.guava:guava:31.1-android")

    // ✅ Required for Java 8 features like lambdas, streams, etc.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//}
//
//android {
//    namespace = "com.example.exerciseapp"
//    compileSdk = 35 // Keeping compileSdk at 35 as you have it
//
//    defaultConfig {
//        applicationId = "com.example.exerciseapp"
//        minSdk = 24
//        targetSdk = 35 // Keeping targetSdk at 35 as you have it
//        versionCode = 1
//        versionName = "1.0"
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//        isCoreLibraryDesugaringEnabled = true // Required for Guava and Java 8+ features
//    }
//
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//
//    buildFeatures {
//        compose = true
//    }
//
//    composeOptions {
//        // You might need to set a specific Kotlin Compiler Extension version
//        // depending on your Kotlin version. For Kotlin 1.9.0, use 1.5.10.
//        // For Kotlin 1.9.20+, use 1.5.4 or higher.
//        // If you encounter issues, uncomment and adjust this.
//        // kotlinCompilerExtensionVersion = "1.5.10"
//    }
//}
//
//dependencies {
//    // Android KTX and Lifecycle
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//
//    // Compose
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom)) // BOM for Compose libraries
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    implementation("androidx.compose.material:material-icons-extended") // Extended Material Icons
//
//
//    // ML Kit Pose Detection
//    // Using explicit versions to ensure consistency and the accurate model.
//    // As of early 2024, 18.0.0-beta4 is commonly used for the accurate model.
//    implementation("com.google.mlkit:pose-detection:18.0.0-beta4")
//    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta4")
//    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
//    // CameraX
//    // It's critical that all CameraX dependencies use the same version.
//    // Using 1.3.3, which is a recent stable version.
//    implementation("androidx.camera:camera-core:1.3.3")
//    implementation("androidx.camera:camera-camera2:1.3.3")
//    implementation("androidx.camera:camera-lifecycle:1.3.3")
//    implementation("androidx.camera:camera-view:1.3.3")
//
//    // General Android Runtime (if needed, but often covered by KTX or other libs)
//    implementation(libs.androidx.runtime)
//
//    // Testing dependencies
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//
//    // Required for ListenableFuture (used by CameraX) and other Java 8+ features
//    implementation("com.google.guava:guava:32.1.1-android") // Updated to a slightly newer version for stability
//    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4") // Required for Java 8+ features on older Android
//}