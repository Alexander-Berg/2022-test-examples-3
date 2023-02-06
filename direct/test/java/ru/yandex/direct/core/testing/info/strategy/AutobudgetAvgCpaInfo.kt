package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpaInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpa,
): StrategyInfo<AutobudgetAvgCpa>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpa())
    constructor() : this(ClientInfo())

}
