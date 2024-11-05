// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("androidx.room") version "2.6.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false
    kotlin("jvm") version "2.0.21" apply false
    id ("com.google.dagger.hilt.android") version "2.52" apply false




}
buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.0.21"))
    }
}