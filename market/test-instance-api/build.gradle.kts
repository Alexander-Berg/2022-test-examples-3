import ru.yandex.market.tpl.build.DependenciesDigest as Deps

plugins {
    kotlinJvm
}

dependencies {
    implementation(Deps.Kotlin.StdLib.jdk7)
    api(Deps.AndroidX.annotation)
}
