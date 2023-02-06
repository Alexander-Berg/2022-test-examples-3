package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetMaxImpressionsCustomPeriodInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetMaxImpressionsCustomPeriod,
): StrategyInfo<AutobudgetMaxImpressionsCustomPeriod>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientAutobudgetMaxImpressionsCustomPeriodStrategy())
    constructor() : this(ClientInfo())

}
