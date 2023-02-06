import ru.yandex.market.sc.build.Project
import ru.yandex.market.sc.build.Dependencies as Deps

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    id("io.qameta.allure")
}

android {
    buildTypes {
        create("qa") {
            initWith(getByName("release"))
        }
    }
}

dependencies {
    implementation(project(Project.Core.data))
    implementation(project(Project.Core.network))
    implementation(project(Project.Core.utils))
    implementation(project(Project.Test.data))

    testImplementation(project(Project.Test.utils))

    implementation(Deps.Google.Hilt.hiltAndroid)
    implementation(Deps.Google.Hilt.hiltTesting)
    kapt(Deps.AndroidX.Hilt.compiler)
    kapt(Deps.Google.Hilt.hiltCompiler)

    implementation(Deps.Kotlin.Coroutines.playServicesIntegration)
    implementation(Deps.AndroidX.lifecycleLivedata)

    implementation(Deps.Squareup.okhttp3)
    implementation(Deps.Squareup.okhttp3Logging)
    implementation(Deps.Squareup.retrofit)

    implementation(Deps.Qameta.commons)
}
