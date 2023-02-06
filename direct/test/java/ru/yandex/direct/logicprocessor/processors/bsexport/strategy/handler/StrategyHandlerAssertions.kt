package ru.yandex.direct.logicprocessor.processors.bsexport.strategy.handler

import org.assertj.core.api.Assertions
import ru.yandex.adv.direct.strategy.Strategy
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.logicprocessor.processors.bsexport.strategy.container.StrategyHandlerContainer
import ru.yandex.direct.logicprocessor.processors.bsexport.strategy.container.StrategyWithBuilder

const val CLIENT_ID = 13L
const val AGENCY_CLIENT_ID = 14L
private const val SHARD = 1

object StrategyHandlerAssertions {
    fun <T : BaseStrategy> assertProtoFilledCorrectly(
        handler: TypedStrategyResourceHandler<T>,
        strategy: T,
        expectedProto: Strategy,
    ) {
        val actualProto = resourceToProto(handler, strategy)
        Assertions.assertThat(actualProto).isEqualTo(expectedProto)
    }

    fun <T : BaseStrategy> assertCampaignHandledCorrectly(
        handler: TypedStrategyResourceHandler<T>,
        strategy: T,
        clientById: Map<Long, Client>,
    ) {
        val expectedProto = resourceToProto(handler, strategy)
        assertStrategyHandledCorrectly(handler, clientById, strategy, expectedProto)
    }

    fun assertStrategyHandledCorrectly(
        handler: IStrategyResourceHandler,
        clientById: Map<Long, Client>,
        strategy: BaseStrategy,
        expectedProto: Strategy,
    ) {
        val campaignWithBuilder = StrategyWithBuilder(strategy, Strategy.newBuilder())
        handler.handle(StrategyHandlerContainer(SHARD, clientById), mapOf(strategy.id to campaignWithBuilder))

        val actualProto = campaignWithBuilder.builder.buildPartial()
        Assertions.assertThat(actualProto).isEqualTo(expectedProto)
    }

    private fun <T : BaseStrategy> resourceToProto(handler: TypedStrategyResourceHandler<T>, strategy: T) =
        Strategy.newBuilder().also {
            val strategyById = mapOf(Pair(strategy.id, StrategyWithBuilder(strategy, it)))
            handler.fillExportObjects(
                StrategyHandlerContainer(
                    SHARD,
                    mapOf(
                        Pair(
                            CLIENT_ID, Client()
                                .withClientId(CLIENT_ID)
                                .withAgencyClientId(AGENCY_CLIENT_ID)
                                .withWorkCurrency(CurrencyCode.RUB)
                                .withNonResident(true)
                        )
                    )
                ), strategyById
            )
        }.buildPartial()
}
