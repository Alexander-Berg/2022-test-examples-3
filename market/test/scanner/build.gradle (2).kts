plugins {
    id("settings-config")
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = ru.yandex.market.mobile.android.build.Versions.SDK.compileSdk

    defaultConfig {
        minSdk = ru.yandex.market.mobile.android.build.Versions.SDK.minSdk
        targetSdk = ru.yandex.market.mobile.android.build.Versions.SDK.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core:scanner-api"))
}
