package ru.yandex.direct.testing.matchers

import org.assertj.core.api.Assert
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoWarnings
import ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.ValidationResult

fun Assert<*, ValidationResult<*, Defect<*>>>.hasErrorOrWarning(path: Path, defectId: DefectId<*>) {
    this.`is`(
        matchedBy(
            hasDefectDefinitionWith<Any?>(
                validationError(
                    path,
                    defectId
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasErrorOrWarning(path: Path, defect: Defect<*>) {
    this.`is`(
        matchedBy(
            hasDefectDefinitionWith<Any?>(
                validationError(
                    path,
                    defect
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasError(path: Path, defectId: DefectId<*>) {
    this.`is`(
        matchedBy(
            hasDefectWithDefinition<Any?>(
                validationError(
                    path,
                    defectId
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasError(path: Path, defect: Defect<*>) {
    this.`is`(
        matchedBy(
            hasDefectWithDefinition<Any?>(
                validationError(
                    path,
                    defect
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasWarning(path: Path, defectId: DefectId<*>) {
    this.`is`(
        matchedBy(
            hasWarningWithDefinition<Any?>(
                validationError(
                    path,
                    defectId
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasWarning(path: Path, defect: Defect<*>) {
    this.`is`(
        matchedBy(
            hasWarningWithDefinition<Any?>(
                validationError(
                    path,
                    defect
                )
            )
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasNoWarnings() {
    this.`is`(
        matchedBy(
            hasNoWarnings<Any?>()
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasNoErrors() {
    this.`is`(
        matchedBy(
            hasNoErrors<Any?>()
        )
    )
}

fun Assert<*, ValidationResult<*, Defect<*>>>.hasNoErrorsOrWarnings() {
    this.`is`(
        matchedBy(
            hasNoErrorsAndWarnings<Any?>()
        )
    )
}
