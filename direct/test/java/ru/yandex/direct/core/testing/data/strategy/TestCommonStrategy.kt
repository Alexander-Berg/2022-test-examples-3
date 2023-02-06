package ru.yandex.direct.core.testing.data.strategy

import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.service.StrategyConstants.STRATEGY_CLASS_TO_TYPE

object TestCommonStrategy {
    inline fun <reified T : CommonStrategy> fillCommonClientFields(strategy: T): T {
        strategy.type = getType<T>()
        strategy.attributionModel = StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK
        strategy.name = "strategy_name_" + RandomStringUtils.randomAlphabetic(5)
        return strategy
    }

    inline fun <reified T : BaseStrategy> getType(): StrategyName = STRATEGY_CLASS_TO_TYPE[T::class.java]!!
}
