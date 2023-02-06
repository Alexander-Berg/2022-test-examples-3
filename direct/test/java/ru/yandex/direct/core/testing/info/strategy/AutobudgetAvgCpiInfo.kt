package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpiInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpi,
): StrategyInfo<AutobudgetAvgCpi>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpi())
    constructor() : this(ClientInfo())

}
