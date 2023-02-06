package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpcPerFilterInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpcPerFilter,
): StrategyInfo<AutobudgetAvgCpcPerFilter>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpcPerFilter())
    constructor() : this(ClientInfo())

}
