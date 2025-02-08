plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.example.bonfie"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bonfie"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 📌 Добавляем ndk и abiFilters для Chaquopy
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

chaquopy {
    defaultConfig {

        pip {
            install("geoip2")
            install("requests")
            install("python-whois")
        }
    }
}


dependencies {

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview.v160)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3.v120)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v1100)

    debugImplementation(libs.androidx.ui.tooling.v160)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core)
}
