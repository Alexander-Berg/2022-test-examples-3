package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

abstract class StrategyInfo<S : CommonStrategy>(
    var clientInfo: ClientInfo,
    var typedStrategy: S
) {

    val shard: Int get() = clientInfo.shard
    val uid: Long get() = clientInfo.uid

    val strategyId: Long get() = typedStrategy.id
    val campaignIds: List<Long> get() = typedStrategy.cids

    fun withTypedStrategy(typedStrategy: S) : StrategyInfo<S> {
        this.typedStrategy = typedStrategy
        return this
    }
}
