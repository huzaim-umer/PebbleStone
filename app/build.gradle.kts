import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing secrets live in keystore.properties (git-ignored), never in VCS.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "com.spore.gemleap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spore.gemleap"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.6"
    }

    signingConfigs {
        // Only wired up when keystore.properties is present (e.g. on your machine
        // or CI). Absent it, release builds are simply unsigned.
        if (keystorePropsFile.exists()) {
            create("release") {
                // storeFile in keystore.properties is relative to the repo root.
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Monetization
    implementation("com.google.android.gms:play-services-ads:24.5.0")   // AdMob (interstitial + rewarded)
    implementation("com.google.android.ump:user-messaging-platform:4.0.0") // GDPR consent
    implementation("com.android.billingclient:billing:8.0.0")           // Play Billing (Remove Ads)
    implementation("com.google.android.play:app-update:2.1.0")          // In-app "new version" prompt

    // A library on the classpath pulls in an old Fragment; the leaderboard launcher uses the
    // Activity Result API, which needs Fragment 1.3.0+ (release lint enforces this).
    implementation("androidx.fragment:fragment:1.8.9")

    // Global leaderboard (Play Games Services)
    implementation("com.google.android.gms:play-services-games-v2:22.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
