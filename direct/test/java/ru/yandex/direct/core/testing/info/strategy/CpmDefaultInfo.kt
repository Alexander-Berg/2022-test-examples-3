package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.CpmDefault
import ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy.clientCpmDefaultStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class CpmDefaultInfo(
    clientInfo: ClientInfo,
    typedStrategy: CpmDefault,
): StrategyInfo<CpmDefault>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientCpmDefaultStrategy())
    constructor() : this(ClientInfo())

}
