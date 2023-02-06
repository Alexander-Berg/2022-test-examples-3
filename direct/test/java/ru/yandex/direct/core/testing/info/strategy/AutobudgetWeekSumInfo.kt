package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy.autobudget
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetWeekSumInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetWeekSum,
): StrategyInfo<AutobudgetWeekSum>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudget())
    constructor() : this(ClientInfo())

}
