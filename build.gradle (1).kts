import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) localPropsFile.inputStream().use { localProps.load(it) }
val pvKey = localProps.getProperty("PICOVOICE_ACCESS_KEY", "")

android {
    namespace = "com.example.jarvisrealme"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.jarvisrealme"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"
        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"${pvKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("ai.picovoice:porcupine-android:3.0.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
