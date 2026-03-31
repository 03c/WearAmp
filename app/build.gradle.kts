import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------
// Version management
// In CI the VERSION_CODE env var is set to the GitHub Actions run number and
// the full version name is computed as MAJOR.MINOR.RUN_NUMBER (e.g. 1.0.42).
// Bump VERSION_MAJOR_MINOR in version.properties for significant releases.
// ---------------------------------------------------------------------------
val versionProps = Properties().apply {
    rootProject.file("version.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
val majorMinor = versionProps.getProperty("VERSION_MAJOR_MINOR", "1.0")

android {
    namespace = "com.wearamp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wearamp"
        minSdk = 30
        targetSdk = 35
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_CODE")?.let { "$majorMinor.$it" } ?: "$majorMinor.0-dev"
    }

    // ---------------------------------------------------------------------------
    // Signing
    // Provide these four environment variables in CI (see .github/workflows/deploy.yml):
    //   KEYSTORE_PATH          – absolute path to the decoded .jks file
    //   KEY_STORE_PASSWORD     – keystore password
    //   KEY_ALIAS              – key alias inside the keystore
    //   KEY_PASSWORD           – key password
    // For local development no signing env vars are needed; the release build
    // type falls back to the debug signing config automatically.
    // ---------------------------------------------------------------------------
    val keystorePath = System.getenv("KEYSTORE_PATH")
    if (!keystorePath.isNullOrEmpty()) {
        val storePass = System.getenv("KEY_STORE_PASSWORD")
            ?: error("KEY_STORE_PASSWORD env var is required when KEYSTORE_PATH is set")
        val keyAliasVal = System.getenv("KEY_ALIAS")
            ?: error("KEY_ALIAS env var is required when KEYSTORE_PATH is set")
        val keyPass = System.getenv("KEY_PASSWORD")
            ?: error("KEY_PASSWORD env var is required when KEYSTORE_PATH is set")
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = storePass
                keyAlias = keyAliasVal
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (!System.getenv("KEYSTORE_PATH").isNullOrEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)

    // Wear Compose
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    // Horologist
    implementation(libs.horologist.composables)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.media3.backend)
    implementation(libs.horologist.media.ui)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle / ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Image loading
    implementation(libs.coil.compose)

    // Material Icons
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.wear.compose.ui.tooling)
}
