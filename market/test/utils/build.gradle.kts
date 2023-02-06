import ru.yandex.market.sc.build.strictVersion
import ru.yandex.market.sc.build.Dependencies as Deps

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
    implementation(Deps.Junit.junit)
    implementation(Deps.AndroidX.lifecycleLivedata)
    implementation(Deps.Mockito.inline)
    implementation(Deps.Kotlin.Coroutines.test)
    implementation(Deps.AndroidX.Test.core)

    runtimeOnly(Deps.byteBuddy) { strictVersion() }
}
