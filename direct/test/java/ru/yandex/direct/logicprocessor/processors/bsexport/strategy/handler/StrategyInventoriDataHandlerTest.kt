package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.entity.strategy.type.withinventoridata.StrategyInventoriDataRepository

class StrategyInventoriDataHandlerTest {
    private val STRATEGY_ID: Long = 1L
    private fun strategyInventoriDataRepository(): StrategyInventoriDataRepository =
        mock {
            on { getStrategyInventoriData(1, setOf(STRATEGY_ID)) } doReturn mapOf(
                Pair(STRATEGY_ID, 3500L)
                )
        }
     private val handler = StrategyInventoriDataHandler(strategyInventoriDataRepository())
    @Test
    fun `inventori data are mapped to proto correctly for Fix Price strategies`() = StrategyHandlerAssertions.assertProtoFilledCorrectly(
        handler,
        strategy = PeriodFixBid()
            .withId(STRATEGY_ID),
        expectedProto = Strategy.newBuilder()
            .setGeneralInitialAuctionProbability(3500L)
            .buildPartial()
    )
}
