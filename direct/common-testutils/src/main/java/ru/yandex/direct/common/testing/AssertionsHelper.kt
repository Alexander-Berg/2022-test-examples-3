package ru.yandex.direct.common.testing

import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractByteAssert
import org.assertj.core.api.AbstractDoubleAssert
import org.assertj.core.api.AbstractFloatAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractShortAssert
import org.assertj.core.api.BooleanAssert
import org.assertj.core.api.ByteAssert
import org.assertj.core.api.DoubleAssert
import org.assertj.core.api.FloatAssert
import org.assertj.core.api.IntegerAssert
import org.assertj.core.api.LongAssert
import org.assertj.core.api.ShortAssert

// https://youtrack.jetbrains.com/issue/KT-21285

fun assertThatKt(actual: Byte?): AbstractByteAssert<*> = ByteAssert(actual)

fun assertThatKt(actual: Short?): AbstractShortAssert<*> = ShortAssert(actual)

fun assertThatKt(actual: Int?): AbstractIntegerAssert<*> = IntegerAssert(actual)

fun assertThatKt(actual: Long?): AbstractLongAssert<*> = LongAssert(actual)

fun assertThatKt(actual: Float?): AbstractFloatAssert<*> = FloatAssert(actual)

fun assertThatKt(actual: Double?): AbstractDoubleAssert<*> = DoubleAssert(actual)

fun assertThatKt(actual: Boolean?): AbstractBooleanAssert<*> = BooleanAssert(actual)
