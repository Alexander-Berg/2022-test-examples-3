package ru.yandex.direct.oneshot.oneshots

import org.assertj.core.api.ObjectAssert
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.pathFromStrings
import ru.yandex.direct.validation.result.ValidationResult

class OneshotTestsUtils {

    companion object {
        fun <T : ValidationResult<*, *>> ObjectAssert<T>.hasDefect(path: String, defect: Defect<*>) =
                this.`is`(
                        matchedBy(
                                Matchers.hasDefectWithDefinition<Any>(
                                        validationError(pathFromStrings(path), defect)
                                )
                        )
                )
    }

}
