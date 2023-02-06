package ru.yandex.market.tpl.courier.extensions

import io.kotest.matchers.Matcher
import io.kotest.matchers.types.beInstanceOf
import ru.yandex.market.tpl.courier.arch.fp.SuccessOrFailure
import ru.yandex.market.tpl.courier.arch.validation.ValidationException

fun <T> matchValidationFailure(): Matcher<SuccessOrFailure<T, Throwable>> {
    return failureWith(beInstanceOf<ValidationException>())
}