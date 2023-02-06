package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerCampStrategy.autobudgetAvgCpcPerCamp
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpcPerCampInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpcPerCamp,
): StrategyInfo<AutobudgetAvgCpcPerCamp>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpcPerCamp())
    constructor() : this(ClientInfo())

}
