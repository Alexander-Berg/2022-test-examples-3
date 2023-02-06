package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathNode

@RunWith(JUnitParamsRunner::class)
class LinkedCampaignIdsValidatorTest {

    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(CampaignType.TEXT, StrategyDefects.inconsistentStrategyToCampaignType()),
        arrayOf(CampaignType.MOBILE_CONTENT, null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaignType=[{0}]")
    fun `only supported campaign type can be linked`(
        campaignType: CampaignType,
        expectedDefect: Defect<List<Long>>?
    ) {
        testValidation(campaignType, expectedDefect)
    }

    private fun testValidation(
        campaignType: CampaignType,
        expectedDefect: Defect<List<Long>>?
    ) {
        val campaign = defaultCampaignByCampaignType(campaignType).withId(1L)
        val validator = LinkedCampaignIdsValidator(listOf(campaign), true, StrategyName.AUTOBUDGET_AVG_CPI)
        val validationResult = validator.apply(listOf(1L))
        var matcher = Matchers.hasNoDefectsDefinitions<List<Long>>()
        if (expectedDefect != null) {
            matcher = Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathNode.Index(0)),
                    expectedDefect
                )
            )
        }
        Assertions.assertThat(validationResult).`is`(Conditions.matchedBy(matcher))
    }
}
