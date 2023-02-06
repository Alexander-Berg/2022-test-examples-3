package ru.yandex.direct.grid.processing.util.validation

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.InstanceOfAssertFactories.throwable
import ru.yandex.direct.grid.processing.exception.GridValidationException
import ru.yandex.direct.grid.processing.model.api.GdApiResponse
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path

fun AbstractThrowableAssert<*, out Throwable>.asGridValidationException(): AbstractThrowableAssert<*, GridValidationException> =
    asInstanceOf(throwable(GridValidationException::class.java))

fun AbstractObjectAssert<*, out GdApiResponse>.extractingValidationResult(): AbstractObjectAssert<*, GdValidationResult> =
    extracting(GdApiResponse::getValidationResult)

fun AbstractThrowableAssert<*, GridValidationException>.extractingValidationResult(): AbstractObjectAssert<*, GdValidationResult> =
    extracting(GridValidationException::getValidationResult)

fun AbstractObjectAssert<out AbstractObjectAssert<*, GdValidationResult>, GdValidationResult>.hasErrorsWith(
    path: Path, defect: Defect<*>,
): AbstractObjectAssert<*, GdValidationResult> =
    `is`(Conditions.matchedBy(GridValidationMatchers.hasErrorsWith(GridValidationMatchers.gridDefect(path, defect))))
