package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa

private const val COUNTER = 199L

class StrategyMetrikaCounterIdsHandlerTest {
    private val handler = StrategyMetrikaCounterIdsHandler()

    @Test
    fun `metrika counters are mapped to proto correctly`() = StrategyHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        strategy = AutobudgetAvgCpa()
            .withId(1L)
            .withClientId(CLIENT_ID)
            .withMetrikaCounters(listOf(COUNTER)),
        expectedProto = Strategy.newBuilder()
            .addMetrikaCounterIds(COUNTER)
            .buildPartial(),
    )
}
