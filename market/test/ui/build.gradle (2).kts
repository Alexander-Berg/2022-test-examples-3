import ru.yandex.market.mobile.android.build.Dependencies
import ru.yandex.market.mobile.android.build.Versions.SDK

plugins {
    id("settings-config")
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = SDK.compileSdk

    defaultConfig {
        minSdk = SDK.minSdk
        targetSdk = SDK.targetSdk

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
    implementation(Dependencies.Kakao.kakao)
    implementation(Dependencies.AndroidX.Compose.uiTestJunit)
    implementation(Dependencies.AndroidX.Compose.uiTestManifest)
}
