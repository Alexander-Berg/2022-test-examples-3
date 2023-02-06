package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class LinkedCampaignTypesValidatorTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(
            listOf(CampaignType.TEXT, CampaignType.DYNAMIC),
            StrategyDefects.campaignsWithDifferentTypesInOnePackage()
        ),
        arrayOf(listOf(CampaignType.TEXT, CampaignType.TEXT), null),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("campaignType=[{0}]")
    fun `only supported campaign type can be linked`(
        campaignTypes: List<CampaignType>,
        expectedDefect: Defect<*>?
    ) {
        val campaign = TestCampaigns.defaultCampaignByCampaignType(campaignTypes[0]).withId(1L)
        val campaign2 = TestCampaigns.defaultCampaignByCampaignType(campaignTypes[1]).withId(2L)

        val constraint = LinkedCampaignTypesValidator(listOf(campaign, campaign2))
        val validationResult = constraint.apply(autobudgetAvgCpa().withCids(listOf(1L, 2L)))

        expectedDefect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(PathHelper.emptyPath(), it)
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }
}
