package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsStrategy.clientAutobudgetMaxImpressionsStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetMaxImpressionsInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetMaxImpressions,
): StrategyInfo<AutobudgetMaxImpressions>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientAutobudgetMaxImpressionsStrategy())
    constructor() : this(ClientInfo())

}
