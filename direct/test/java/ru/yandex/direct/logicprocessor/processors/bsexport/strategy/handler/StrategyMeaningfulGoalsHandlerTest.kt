package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.MeaningfulGoalList
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import java.math.BigDecimal

class StrategyMeaningfulGoalsHandlerTest {
    private val handler = StrategyMeaningfulGoalsHandler()

    @Test
    fun `meaningful goals are mapped to proto correctly`() = StrategyHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        strategy = AutobudgetAvgCpa()
            .withId(1L)
            .withClientId(CLIENT_ID)
            .withMeaningfulGoals(
                listOf(
                    MeaningfulGoal()
                        .withGoalId(1)
                        .withConversionValue(BigDecimal.TEN)
                        .withIsMetrikaSourceOfValue(true)
                )

            ),
        expectedProto = Strategy.newBuilder()
            .setMeaningfulGoals(
                MeaningfulGoalList.newBuilder().addMeaningfulGoal(
                    ru.yandex.adv.direct.campaign.MeaningfulGoal.newBuilder()
                        .setGoalId(1)
                        .setValue(10_000_000)
                        .setIsMetrikaSourceOfValue(true)
                )
            )
            .buildPartial(),
    )
}
