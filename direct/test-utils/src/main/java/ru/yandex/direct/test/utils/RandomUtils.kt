package ru.yandex.direct.test.utils

import kotlin.random.Random

fun randomNegativeInt(minValue: Int = Int.MIN_VALUE) = Random.nextInt(minValue, 0)

fun randomPositiveInt(until: Int = Int.MAX_VALUE) = Random.nextInt(from = 1, until = until)

fun randomNegativeLong(minValue: Long = Long.MIN_VALUE) = Random.nextLong(minValue, 0)

fun randomPositiveLong(until: Long = Long.MAX_VALUE) = Random.nextLong(from = 1, until = until)

fun randomPositiveBigInteger() = randomPositiveLong().toBigInteger()

fun randomPositiveDouble(until: Double = Double.MAX_VALUE) = Random.nextDouble(from = Double.MIN_VALUE, until = until)

fun randomPositiveBigDecimal() = randomPositiveDouble().toBigDecimal()
