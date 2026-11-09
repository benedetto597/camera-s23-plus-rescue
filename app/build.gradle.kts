plugins {
    id("com.android.application")
}

android {
    namespace = "com.s23plus.camera"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.s23plus.camera"
        minSdk = 30
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firmado con debug signingConfig para instalación directa en el S23+ personal
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
