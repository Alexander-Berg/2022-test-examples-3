import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import ru.yandex.market.tpl.build.DependenciesDigest as Deps

plugins {
    kotlinJvm
    kotlinApt
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview"
    }
}

dependencies {
    implementation(project(":test-instance-api"))
    implementation(project(":annotation-processor-commons"))
    implementation(Deps.Kotlin.reflection)
    implementation(Deps.KotlinPoet.core)
    implementation(Deps.KotlinPoet.metadata)
    implementation(Deps.AndroidX.annotation)

    implementation(Deps.AutoService.annotations)
    kapt(Deps.AutoService.processor)

    implementation(Deps.GradleIncapHelper.library)
    kapt(Deps.GradleIncapHelper.compiler)

    implementation(Deps.Dagger.api)
    kapt(Deps.Dagger.processor)
}