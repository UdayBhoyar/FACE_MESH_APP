/*
 * This file is the main build script for the application module.
 * It uses the Kotlin DSL (Domain Specific Language) for Gradle.
 */

// 1. PLUGINS
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 2. ANDROID CONFIGURATION
android {
    // This namespace MUST match your folder structure
    namespace = "com.example.face_mesh_app"
    compileSdk = 34

    defaultConfig {
        // This is the unique ID for your app on the device/Play Store
        applicationId = "com.example.face_mesh_app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- SOLUTION: This is the correct way to ensure native libraries are included ---
        // This tells Gradle to package the native (.so) libraries for these CPU architectures
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // --- SOLUTION 2: Additional packaging configuration ---
    // This ensures native libraries are properly included and handles conflicts
    packaging {
        pickFirst("lib/x86/libc++_shared.so")
        pickFirst("lib/x86_64/libc++_shared.so")
        pickFirst("lib/arm64-v8a/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst("lib/x86/libmediapipe_tasks_vision_jni.so")
        pickFirst("lib/x86_64/libmediapipe_tasks_vision_jni.so")
        pickFirst("lib/arm64-v8a/libmediapipe_tasks_vision_jni.so")
        pickFirst("lib/armeabi-v7a/libmediapipe_tasks_vision_jni.so")
    }

    // Remove the incorrect packaging block - it's not needed for this issue
}

// 3. DEPENDENCIES
dependencies {
    // --- Core Android & UI Libraries ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- MediaPipe Vision Task Library (for Face Landmarker) ---
    implementation("com.google.mediapipe:tasks-vision:0.10.9")

    // --- CameraX Libraries (for easy camera access and management) ---
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // --- Kotlin Coroutines (for managing background tasks) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- Testing Libraries ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}