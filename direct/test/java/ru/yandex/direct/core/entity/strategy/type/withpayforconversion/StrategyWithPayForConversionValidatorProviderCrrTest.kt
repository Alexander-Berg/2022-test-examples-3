package ru.yandex.direct.core.entity.strategy.type.withpayforconversion

import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr

class StrategyWithPayForConversionValidatorProviderCrrTest : StrategyWithPayForConversionValidatorProviderBaseTest() {
    override fun strategy(): StrategyWithPayForConversion =
        autobudgetCrr()
            .withGoalId(metrikaGoal.id)
            .withClientId(clientId)
}
