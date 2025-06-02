plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.example.share_everything_project"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.share_everything_project"
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Material Design Components
    implementation("com.google.android.material:material:1.9.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.core.ktx)
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // AndroidX
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime:2.6.2")

    // SUPABASE DEPENDENCIES
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:supabase-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.0")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}