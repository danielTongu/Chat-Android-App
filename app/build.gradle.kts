// build.gradle.kts

/*
 * This is the build script for the ChatAndroidApp module.
 * It uses Gradle's Kotlin DSL to configure the build settings, plugins, and dependencies.
 */

plugins {
    // Applies the Android application plugin using the alias from the version catalog.
    alias(libs.plugins.androidApplication)

    //------ Firebase setup ---------//
    // Applies the Google Services Gradle plugin for Firebase integration.
    id("com.google.gms.google-services") // Add the Google services Gradle plugin
    //-------------------------------//
}

android {
    // Defines the namespace for the app, which is used for code generation and resource packaging.
    namespace = "com.example.chatandroidapp"
    // Specifies the SDK version to compile the app against.
    compileSdk = 34

    defaultConfig {
        // Unique application ID for the app.
        applicationId = "com.example.chatandroidapp"
        // Minimum SDK version that the app supports.
        minSdk = 24
        // Target SDK version that the app is tested against.
        targetSdk = 34
        // Internal version code that increments with each release.
        versionCode = 1
        // User-friendly version name.
        versionName = "1.0"

        // Specifies the instrumentation runner for Android tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Disables code shrinking, obfuscation, and optimization for the release build.
            isMinifyEnabled = false
            // Specifies ProGuard rules files for code shrinking and obfuscation.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // Sets the Java source and target compatibility to Java 8.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        // Enables View Binding, which generates binding classes for your XML layouts.
        viewBinding = true // Connects layout XML directly to Java code without findViewById
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.appcompat) // Provides backward-compatible versions of Android components
    implementation(libs.material) // Material Design components and theming
    implementation(libs.activity) // Support library for Android activities
    implementation(libs.constraintlayout) // Enables complex layouts with a flat view hierarchy
    implementation(libs.roundedimageview) // Library for displaying images with rounded corners

    //------ Firebase setup ---------//
    implementation(libs.firebase.bom) // Firebase BoM to manage Firebase dependencies' versions
    implementation(libs.firebase.messaging) // Firebase Cloud Messaging for push notifications
    implementation(libs.firebase.firestore) // Firebase Cloud Firestore for real-time database
    //-------------------------------//

    // Testing libraries
    testImplementation(libs.junit) // JUnit framework for unit tests
    androidTestImplementation(libs.ext.junit) // AndroidX extensions for JUnit
    androidTestImplementation(libs.espresso.core) // Espresso UI testing framework
}