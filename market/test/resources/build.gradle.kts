import ru.yandex.market.sc.build.Project

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    buildTypes {
        create("qa") {
            initWith(getByName("release"))
        }
    }
}

dependencies {
    implementation(project(Project.Core.resources))
}
