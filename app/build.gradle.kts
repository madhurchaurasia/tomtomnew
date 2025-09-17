plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.poconnbandtomtom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.poconnbandtomtom"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TOMTOM_API_KEY", "\"${project.findProperty("TOMTOM_API_KEY") ?: ""}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // TomTom Maps SDK - Using version catalog references
    implementation(libs.tomtom.maps.display)
    implementation(libs.tomtom.location.provider)
    implementation(libs.tomtom.routing.online)
    implementation(libs.tomtom.navigation.online)
    implementation(libs.tomtom.search.online)
    implementation(libs.tomtom.vehicle.model)

    // Note: Additional navigation dependencies may not be available in this SDK version
    // The core navigation functionality is provided by tomtom-navigation-online

    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // For JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}