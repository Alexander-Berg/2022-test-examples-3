import ru.yandex.market.sc.build.Project
import ru.yandex.market.sc.build.Dependencies as Deps

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    flavorDimensions.add("scanner")

    productFlavors {
        create("tsd") { dimension = "scanner" }
        create("camera") { dimension = "scanner" }
    }

    buildTypes {
        create("qa") {
            initWith(getByName("release"))
        }
    }
}

dependencies {
    implementation(project(Project.Core.scanner))
    implementation(project(Project.Core.utils))
    implementation(project(Project.Core.data))

    implementation(Deps.AndroidX.Compose.uiTestJunit)
    implementation(Deps.AndroidX.Compose.uiTestManifest)
}
