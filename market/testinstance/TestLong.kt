package ru.beru.android.processor.testinstance

@Retention(AnnotationRetention.BINARY)
@Target(allowedTargets = [AnnotationTarget.VALUE_PARAMETER])
annotation class TestLong(val value: Long = 42L)