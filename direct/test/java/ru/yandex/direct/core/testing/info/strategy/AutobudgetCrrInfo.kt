package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetCrrInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetCrr,
): StrategyInfo<AutobudgetCrr>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetCrr())
    constructor() : this(ClientInfo())

}
