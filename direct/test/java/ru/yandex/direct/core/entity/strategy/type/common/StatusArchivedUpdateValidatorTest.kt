package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class StatusArchivedTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(false, null),
        arrayOf(true, StrategyDefects.archivedStrategyModification()),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("statusArchived=[{0}]")
    fun `validate statusArchived when update strategy`(statusArchived: Boolean, defect: Defect<Boolean>?) {
        val validationResult = StatusArchivedUpdateValidator.apply(statusArchived)

        defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith<Boolean>(
                    Matchers.validationError(
                        PathHelper.emptyPath(),
                        it
                    )
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }
}
