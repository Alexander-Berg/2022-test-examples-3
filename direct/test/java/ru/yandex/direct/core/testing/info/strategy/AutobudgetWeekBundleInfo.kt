package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetWeekBundleInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetWeekBundle,
): StrategyInfo<AutobudgetWeekBundle>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetWeekBundle())
    constructor() : this(ClientInfo())

}
