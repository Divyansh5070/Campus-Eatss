import org.gradle.kotlin.dsl.implementation
import java.util.Properties

// Load signing secrets from secrets.properties (gitignored — never committed)
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
}


android {
    namespace = "com.divyansh.cueats"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.divyansh.cueats"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 16
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject secrets into BuildConfig (never appear in committed source)
        buildConfigField("String", "ONESIGNAL_APP_ID",
            "\"${secrets.getProperty("ONESIGNAL_APP_ID", "")}\"")
    }
    
    // Enable 16 KB page size support for Android 15+ devices
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/${secrets.getProperty("KEYSTORE_FILE", "upload-keystore.jks")}")
            storePassword = secrets.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = secrets.getProperty("KEY_ALIAS", "upload")
            keyPassword = secrets.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false  // ❌ turn off code shrinking/obfuscation
            ndk {
                debugSymbolLevel = "NONE" // disable native debug symbols
            }
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
        buildConfig = true
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
    implementation(libs.play.services.maps)
    implementation(libs.material3)
    implementation(libs.firebase.crashlytics.buildtools)
    // Firebase Storage - using BOM version
    implementation("com.google.firebase:firebase-storage-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)



    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // ADD THIS: Coroutines for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

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

    implementation("com.google.firebase:firebase-firestore-ktx:24.11.1")
    implementation ("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-messaging-ktx")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    // Firebase Auth
    implementation ("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation ("androidx.credentials:credentials:1.2.2")
    implementation ("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation ("androidx.compose.ui:ui:1.6.2")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation ("androidx.compose.material3:material3:1.2.1")

    // ADD THESE: For secure credential storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ADD THIS: For Material Icons Extended (if using Icons.Default.* in your LoginScreen)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    implementation ("com.google.code.gson:gson:2.10.1")

    // Make sure you have these Compose dependencies too
    implementation("androidx.compose.ui:ui:1.5.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.8")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Serialization for type-safe routes
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.osmdroid:osmdroid-android:6.1.16")

    // Glance dependencies for widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance:1.1.0")
    implementation("androidx.glance:glance-material3:1.0.0")
// Work Manager for periodic updates (optional)
    implementation("androidx.work:work-runtime-ktx:2.8.1")
// DataStore for widget state management
    implementation ("com.google.dagger:hilt-android:2.48")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")


    // Lottie for animated icons
    implementation("com.airbnb.android:lottie-compose:6.1.0")
}
