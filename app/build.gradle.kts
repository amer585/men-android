plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.madrastna.teacher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.madrastna.teacher"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Read Turso credentials from local.properties (not committed to git).
        val localProps = rootProject.file("local.properties")
        val props = java.util.Properties()
        if (localProps.exists()) props.load(localProps.inputStream())

        buildConfigField("String", "TURSO_URL",
            "\"${props.getProperty("TURSO_URL", "https://amer-amer321.aws-eu-west-1.turso.io")}\"")
        buildConfigField("String", "TURSO_TOKEN",
            "\"${props.getProperty("TURSO_TOKEN", "")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation("org.json:json:20240303")
}
