plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.vibecheck_dev"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vibecheck_dev"
        minSdk = 26 // <--- SEKARANG MENDUKUNG ANDROID 8 KE ATAS!
        targetSdk = 34 // (Catatan: Saya sarankan turunkan ke 34 karena API 36 masih sangat eksperimental/belum stabil)
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.compose.foundation)
//    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 1. Compose UI (Bawaan)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")

    // 2. CameraX (Untuk ngambil gambar dari lensa)
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    implementation("androidx.camera:camera-video:1.3.0")

    // 3. Ktor Server (Sebagai pengganti "shelf" di Flutter buat Local Server & WebSocket)
    val ktor_version = "2.3.7"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")

    // 4. ML Kit (Untuk Smart Gesture Shutter - Deteksi Tangan)
    implementation("com.google.mlkit:pose-detection:17.0.0")

    // 5. WebRTC (Untuk mancarin video P2P)
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.navigation:navigation-compose:2.7.5")

    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.print:print:1.0.0")

}