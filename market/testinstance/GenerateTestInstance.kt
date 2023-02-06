package ru.beru.android.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
annotation class GenerateTestInstance(val jvmOverloads: JvmOverloadsMode = JvmOverloadsMode.None)

enum class JvmOverloadsMode { None, Full, NoArgOnly }