package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy.autobudgetRoi
import ru.yandex.direct.core.testing.info.ClientInfo

class AutobudgetRoiInfo(
    clientInfo: ClientInfo,
    typedStrategy: AutobudgetRoi,
): StrategyInfo<AutobudgetRoi>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, autobudgetRoi())
    constructor() : this(ClientInfo())

}
