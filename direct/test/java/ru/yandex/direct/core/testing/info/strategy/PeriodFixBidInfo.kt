package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.testing.data.strategy.TestPeriodFixBidStrategy.clientPeriodFixBidStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class PeriodFixBidInfo(
    clientInfo: ClientInfo,
    typedStrategy: PeriodFixBid,
): StrategyInfo<PeriodFixBid>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientPeriodFixBidStrategy())
    constructor() : this(ClientInfo())

}
