package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgClickInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgClick,
): StrategyInfo<AutobudgetAvgClick>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgClick())
    constructor() : this(ClientInfo())

}
