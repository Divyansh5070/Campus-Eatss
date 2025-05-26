plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.cueats"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cueats"
        minSdk = 24
        targetSdk = 34
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
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-database-ktx:20.2.2")

        implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.30.1")

    implementation("com.google.accompanist:accompanist-placeholder-material3:0.31.1-alpha")
    implementation("androidx.compose.foundation:foundation:1.7.8")
    implementation("androidx.compose.animation:animation:1.7.8")
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.30.0")
    implementation ("com.google.accompanist:accompanist-placeholder-material:0.30.0")

    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.0")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("com.google.accompanist:accompanist-pager:0.32.0")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation ("com.onesignal:OneSignal:[5.0.0, 5.1.99]")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.4")

    implementation("io.ktor:ktor-client-android:2.3.4")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-utils:2.3.4")





}
