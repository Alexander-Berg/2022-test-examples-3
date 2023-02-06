package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import java.math.BigDecimal
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.strategy.DayBudgetShowMode
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode

private const val DAY_BUDGET = 11L

class StrategyDayBudgetHandlerTest {
    private val handler = StrategyDayBudgetHandler()

    @Test
    fun `day budget fields are mapped to proto correctly`() = StrategyHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        strategy = DefaultManualStrategy()
            .withId(1L)
            .withClientId(CLIENT_ID)
            .withDayBudget(BigDecimal.valueOf(DAY_BUDGET))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.STRETCHED),
        expectedProto = Strategy.newBuilder()
            .setDayBudget(DAY_BUDGET * 1_000_000)
            .setDayBudgetShowMode(DayBudgetShowMode.STRETCHED.number)
            .buildPartial(),
    )
}
