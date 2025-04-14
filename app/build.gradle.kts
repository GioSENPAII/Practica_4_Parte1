plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Añadir el plugin de kapt para Room (corregido)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.gestorarchivos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gestorarchivos"
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

    buildFeatures {
        // Deshabilitar Compose
        compose = false
        // Habilitar ViewBinding
        viewBinding = true
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
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewModel y LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room para almacenamiento local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Glide para carga de imágenes
    implementation(libs.glide)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.android)

    // Libraries para visualización de documentos
    implementation(libs.gson)
    implementation(libs.simple.xml)

    // PhotoView para zoom en imágenes
    implementation(libs.photoview)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}