package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetMaxReachCustomPeriodInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetMaxReachCustomPeriod,
): StrategyInfo<AutobudgetMaxReachCustomPeriod>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientAutobudgetMaxReachCustomPeriodStrategy())
    constructor() : this(ClientInfo())

}
