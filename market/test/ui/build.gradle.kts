import ru.yandex.market.sc.build.Dependencies as Deps

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    buildFeatures {
        compose = true
    }

    buildTypes {
        create("qa") {
            initWith(getByName("release"))
        }
    }
}

dependencies {
    implementation(Deps.Kakao.kakao)
    implementation(Deps.AndroidX.Compose.uiTestJunit)
    implementation(Deps.AndroidX.Compose.uiTestManifest)
}
