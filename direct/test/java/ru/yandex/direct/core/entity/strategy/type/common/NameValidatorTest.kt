package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.service.StrategyConstants.MAX_NAME_LENGTH
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.defect.StringDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class NameValidatorTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf("null", null, null),
        arrayOf("empty name", "", StringDefects.notEmptyString()),
        arrayOf("space", " ", StringDefects.notEmptyString()),
        arrayOf("spaces", "   ", StringDefects.notEmptyString()),
        arrayOf("not utf8", "\u200B\uD83D\uDD25 text", StringDefects.admissibleChars()),
        arrayOf("max length", randomAlphanumeric(MAX_NAME_LENGTH), null),
        arrayOf(
            "more than max length",
            randomAlphanumeric(MAX_NAME_LENGTH + 1),
            CollectionDefects.maxStringLength(MAX_NAME_LENGTH)
        ),
        arrayOf("valid name", "simpleName", null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("name=[{0}]")
    fun `name validation is correct`(testName: String, name: String?, defect: Defect<String?>?) {
        val validationResult = NameValidator.apply(name)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<String?>(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    defect
                )
            )
            validationResult.check(matcher)
        } else {
            validationResult.check(Matchers.hasNoDefectsDefinitions())
        }
    }
}
