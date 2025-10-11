// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Configuration for cleaning the build directory (optional but common)
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
