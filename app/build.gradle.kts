plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.poconnbandtomtom"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.poconnbandtomtom"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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


    val version = "1.26.0"
    implementation("com.tomtom.sdk.location:provider-default:$version")
    implementation("com.tomtom.sdk.location:provider-map-matched:$version")
    implementation("com.tomtom.sdk.location:provider-simulation:$version")
    implementation("com.tomtom.sdk.maps:map-display:$version")
    implementation("com.tomtom.sdk.datamanagement:navigation-tile-store:$version")
    implementation("com.tomtom.sdk.navigation:navigation-online:$version")
    implementation("com.tomtom.sdk.navigation:ui:$version")
    implementation("com.tomtom.sdk.routing:route-planner-online:$version")

    // Note: Additional navigation dependencies may not be available in this SDK version
    // The core navigation functionality is provided by tomtom-navigation-online

    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // For JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}