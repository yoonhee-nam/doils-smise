
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.room")
    id("com.google.devtools.ksp") version "2.0.21-1.0.26"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id ("com.google.dagger.hilt.android")

}


val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = "com.example.doilmise"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.doilmise"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "api_key", properties.getProperty("api_key"))
        buildConfigField("String", "kakao_key", properties.getProperty("kakao_key"))
        manifestPlaceholders["api_key"] = properties.getProperty("api_key")
        manifestPlaceholders["kakao_key"] = properties.getProperty("kakao_key")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    room {
        schemaDirectory("$projectDir/schemas")
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
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.ui)
    //noinspection UseTomlInstead
    implementation("androidx.compose.material3:material3")

    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // Optional - Integration with activities
    implementation (libs.androidx.activity.compose.v191)
    // Optional - Integration with ViewModels
    implementation (libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx.v284)

    implementation (libs.androidx.constraintlayout.compose)


    //retrofit2
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    //coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    implementation (libs.gms.play.services.location.v2101)

    implementation (libs.accompanist.swiperefresh.v0280)

    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.room.compiler)

    // To use Kotlin Symbol Processing (KSP)
    ksp(libs.androidx.room.compiler.v250)

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)

    // optional - RxJava2 support for Room
    implementation(libs.androidx.room.rxjava2)

    // optional - RxJava3 support for Room
    implementation(libs.androidx.room.rxjava3)

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation(libs.androidx.room.guava)

    // optional - Test helpers
    testImplementation(libs.androidx.room.testing)

    // optional - Paging 3 Integration
    implementation(libs.androidx.room.paging)

    implementation(libs.symbol.processing.api)

    implementation(kotlin("stdlib-jdk8"))
// Hilt
    implementation (libs.hilt.android.v252)
    ksp (libs.hilt.compiler)

    // For instrumentation tests
    androidTestImplementation  (libs.hilt.android.testing)
    ksp (libs.hilt.compiler)

    // For local unit tests
    testImplementation (libs.hilt.android.testing)
    ksp (libs.hilt.compiler)
}