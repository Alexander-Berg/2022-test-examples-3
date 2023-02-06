package ru.yandex.direct.core.testing.info.strategy

import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

class DefaultManualStrategyInfo(
    clientInfo: ClientInfo,
    typedStrategy: DefaultManualStrategy,
): StrategyInfo<DefaultManualStrategy>(clientInfo, typedStrategy) {

    constructor(clientInfo: ClientInfo) : this(clientInfo, clientDefaultManualStrategy())
    constructor() : this(ClientInfo())

}
