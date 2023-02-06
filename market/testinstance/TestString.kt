package ru.beru.android.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER])
annotation class TestString(val value: String = "")