package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpaPerFilterInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpaPerFilter,
): StrategyInfo<AutobudgetAvgCpaPerFilter>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpaPerFilter())
    constructor() : this(ClientInfo())

}
