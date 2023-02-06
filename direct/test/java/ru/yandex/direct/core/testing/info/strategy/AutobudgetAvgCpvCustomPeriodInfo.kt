package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpvCustomPeriodStrategy.clientAutobudgetAvgCpvCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpvCustomPeriodInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpvCustomPeriod,
): StrategyInfo<AutobudgetAvgCpvCustomPeriod>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientAutobudgetAvgCpvCustomPeriodStrategy())
    constructor() : this(ClientInfo())

}
