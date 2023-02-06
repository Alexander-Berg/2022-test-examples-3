package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetMaxReachInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetMaxReach,
): StrategyInfo<AutobudgetMaxReach>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientAutobudgetReachStrategy())
    constructor() : this(ClientInfo())

}
