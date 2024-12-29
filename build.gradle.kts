// build.gradle.kts

/*
 * The buildscript block is used to define dependencies and repositories for the build script itself.
 * This is where you specify dependencies required by your build scripts, not by your app/module code.
 */
buildscript {
    dependencies {
        // Adds the Google Services Gradle plugin to the build script classpath.
        // The 'libs.google.services' refers to the version specified in your dependency management system.
        classpath(libs.google.services)
    }
}

/*
 * Top-level build file where you can add configuration options common to all sub-projects/modules.
 * The plugins block applies plugins to the project. Applying a plugin adds tasks and configurations
 * that are needed to build your project.
 */
plugins {
    // Applies the Android application plugin, but does not apply it immediately to this project.
    // 'apply false' means the plugin is available to sub-projects but not applied to the root project.
    alias(libs.plugins.androidApplication) apply false

    /*
     * Applies the Google Services Gradle plugin, which enables Firebase services.
     * This plugin reads the 'google-services.json' file and configures your app to use Firebase.
     * The 'apply false' statement means it will not be applied to the root project, but will be available
     * for sub-projects/modules to apply.
     */
    id("com.google.gms.google-services") version "4.4.2" apply false
}