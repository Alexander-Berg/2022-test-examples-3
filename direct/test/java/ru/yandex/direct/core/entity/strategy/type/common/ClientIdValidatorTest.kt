package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class ClientIdValidatorTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, null),
        arrayOf(0L, CommonDefects.isNull()),
        arrayOf(123L, CommonDefects.isNull()),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("clientId=[{0}]")
    fun `clientId validation is correct`(clientId: Long?, defect: Defect<Long?>?) {
        val validationResult = ClientIdValidator().apply(clientId)
        if (defect != null) {
            val matcher = Matchers.hasDefectDefinitionWith<Long?>(
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
