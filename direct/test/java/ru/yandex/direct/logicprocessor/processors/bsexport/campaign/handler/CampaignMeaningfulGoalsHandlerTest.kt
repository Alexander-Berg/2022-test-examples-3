package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.campaign.MeaningfulGoalList
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.meaningfulGoalsToDb
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.container.MeaningfulGoalsWithCurrency
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertProtoFilledCorrectly
import java.math.BigDecimal
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class CampaignMeaningfulGoalsHandlerTest {
    private val handler = CampaignMeaningfulGoalsHandler()

    @Test
    fun `null meaningful goals are mapped to proto correctly`() = assertProtoFilledCorrectly(
            handler,
            resource = MeaningfulGoalsWithCurrency(null, CurrencyCode.RUB),
            expectedProto = Campaign.newBuilder()
                    .setMeaningfulGoals(MeaningfulGoalList.getDefaultInstance())
                    .setMeaningfulGoalsHash(-3162216497309240828L)
                    .buildPartial(),
    )

    @Test
    fun `meaningful goals are mapped to proto correctly`() {
        val expectedMeaningfulGoal = ru.yandex.adv.direct.campaign.MeaningfulGoal.newBuilder()
                .setValue(10_000_000L)
                .setGoalId(12)

        val expectedMeaningfulGoalList = MeaningfulGoalList.newBuilder()
                .addMeaningfulGoal(expectedMeaningfulGoal)
                .build()

        assertProtoFilledCorrectly(
                handler,
                resource = MeaningfulGoalsWithCurrency(
                        meaningfulGoalsToDb(listOf(MeaningfulGoal().withGoalId(12).withConversionValue(BigDecimal.TEN)), false),
                        CurrencyCode.RUB,
                ),
                expectedProto = Campaign.newBuilder()
                        .setMeaningfulGoals(expectedMeaningfulGoalList)
                        .setMeaningfulGoalsHash(2211635861861141780L)
                        .buildPartial(),
        )
    }


    @Test
    fun `campaign with meaningful goals`() {
        val meaningfulGoals = listOf(
                MeaningfulGoal()
                        .withConversionValue(BigDecimal.TEN)
                        .withGoalId(12L),
        )

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
                        .withRawMeaningfulGoals(meaningfulGoalsToDb(meaningfulGoals, false))
                        .withMeaningfulGoals(meaningfulGoals),
                expectedResource = MeaningfulGoalsWithCurrency(meaningfulGoalsToDb(meaningfulGoals, false), CurrencyCode.RUB),
        )
    }
}
