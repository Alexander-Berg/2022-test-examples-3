package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpv.autobudgetAvgCpv
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetAvgCpvInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetAvgCpv,
): StrategyInfo<AutobudgetAvgCpv>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetAvgCpv())
    constructor() : this(ClientInfo())

}
