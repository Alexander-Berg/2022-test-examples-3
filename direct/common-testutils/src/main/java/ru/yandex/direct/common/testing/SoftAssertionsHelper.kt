package ru.yandex.direct.common.testing

import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractByteAssert
import org.assertj.core.api.AbstractDoubleAssert
import org.assertj.core.api.AbstractFloatAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractShortAssert
import org.assertj.core.api.SoftAssertions


fun softly(softlyBlock: SoftAssertions.() -> Unit) {
    val assertions = SoftAssertions()
    assertions.softlyBlock()
    assertions.assertAll()
}

fun SoftAssertions.assertThatVerificationOk(block: (Unit) -> Unit) {
    assertThatCode { block.invoke(Unit) }.doesNotThrowAnyException()
}

// https://youtrack.jetbrains.com/issue/KT-21285

fun SoftAssertions.assertThatKt(actual: Byte?): AbstractByteAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Short?): AbstractShortAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Int?): AbstractIntegerAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Long?): AbstractLongAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Float?): AbstractFloatAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Double?): AbstractDoubleAssert<*> = this.assertThat(actual)

fun SoftAssertions.assertThatKt(actual: Boolean?): AbstractBooleanAssert<*> = this.assertThat(actual)
