package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpaPerCampInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpaPerCamp,
): StrategyInfo<AutobudgetAvgCpaPerCamp>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpaPerCamp())
    constructor() : this(ClientInfo())

}
