package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class NumberOfCidsValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(null, 100, null),
        arrayOf(listOf(1L), 100, null),
        arrayOf(listOf(1L, 2L), 1, StrategyDefects.tooMuchCampaignsLinkedToStrategy(1))
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("linkingCampaigns=[{0}], maxNumberOfCidsAllowed=[{1}]")
    fun `isPublic validation is correct on creating`(
        cids: List<Long>?,
        maxNumberOfCids: Int,
        expectedDefect: Defect<List<Long>>?
    ) {
        testValidation(cids, maxNumberOfCids, expectedDefect)
    }

    private fun testValidation(
        cids: List<Long>?,
        maxNumberOfCids: Int,
        expectedDefect: Defect<List<Long>>?
    ) {
        val validator = NumberOfCidsValidator(maxNumberOfCids)
        val validationResult = validator.apply(cids)
        var matcher = Matchers.hasNoDefectsDefinitions<List<Long>>()
        if (expectedDefect != null) {
            matcher = Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    expectedDefect
                )
            )
        }
        Assertions.assertThat(validationResult).`is`(Conditions.matchedBy(matcher))
    }
}
