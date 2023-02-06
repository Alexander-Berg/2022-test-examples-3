package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.currency.CurrencyCode
import java.math.BigDecimal

const val SOME_MOBILE_GOAL = Goal.MOBILE_GOAL_UPPER_BOUND - 1
const val SOME_METRIKA_GOAL = Goal.METRIKA_GOAL_UPPER_BOUND - 1

internal class CampaignMobileMarkHandlerTest {
    val handler = CampaignMobileMarkHandler()

    @Test
    fun `campaign without mobile goals`() {
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withConversionValue(BigDecimal.TEN)
                .withGoalId(SOME_METRIKA_GOAL),
        )

        val strategy = DbStrategy().apply {
            withStrategyData(
                StrategyData().withGoalId(SOME_METRIKA_GOAL))
        }

        assertCampaignHandledCorrectly(
            handler,
            campaign = DynamicCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.DYNAMIC)
                .withMeaningfulGoals(meaningfulGoals)
                .withStrategy(strategy),
            expectedResource = null,
        )
    }

    @Test
    fun `campaign with mobile goal at strategy`() {
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withConversionValue(BigDecimal.TEN)
                .withGoalId(SOME_METRIKA_GOAL),
        )

        val strategy = DbStrategy().apply {
            withStrategyData(
                StrategyData().withGoalId(SOME_MOBILE_GOAL))
        }

        assertCampaignHandledCorrectly(
            handler,
            campaign = DynamicCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.DYNAMIC)
                .withMeaningfulGoals(meaningfulGoals)
                .withStrategy(strategy),
            expectedResource = HasMobileGoalsValue,
        )
    }

    @Test
    fun `campaign with mobile goal at meaningful goals`() {
        val meaningfulGoals = listOf(
            MeaningfulGoal()
                .withConversionValue(BigDecimal.TEN)
                .withGoalId(SOME_MOBILE_GOAL),
        )

        val strategy = DbStrategy().apply {
            withStrategyData(
                StrategyData().withGoalId(SOME_METRIKA_GOAL))
        }

        assertCampaignHandledCorrectly(
            handler,
            campaign = DynamicCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.DYNAMIC)
                .withMeaningfulGoals(meaningfulGoals)
                .withStrategy(strategy),
            expectedResource = HasMobileGoalsValue,
        )
    }

    @Test
    fun `campaign with null at meaningful goals`() {
        val strategy = DbStrategy().apply {
            withStrategyData(
                StrategyData().withGoalId(SOME_MOBILE_GOAL))
        }

        assertCampaignHandledCorrectly(
            handler,
            campaign = DynamicCampaign()
                .withId(1)
                .withCurrency(CurrencyCode.RUB)
                .withName("")
                .withStatusArchived(false)
                .withStatusShow(true)
                .withClientId(15L)
                .withType(CampaignType.DYNAMIC)
                .withMeaningfulGoals(null)
                .withStrategy(strategy),
            expectedResource = HasMobileGoalsValue,
        )
    }
}

private fun assertCampaignHandledCorrectly(
    handler: CampaignMobileMarkHandler,
    campaign: BaseCampaign,
    expectedResource: List<Int>?,
) {
    val expectedProto = Campaign.newBuilder()
        .apply {
            if (expectedResource != null) {
                mobileAppIdsBuilder.addAllValues(expectedResource)
            }
        }
        .buildPartial()
    CampaignHandlerAssertions.assertCampaignHandledCorrectly(handler, campaign, expectedProto)
}
