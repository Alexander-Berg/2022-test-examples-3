package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpi

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.validation.AbstractStrategyValidatorProvider
import ru.yandex.direct.core.entity.strategy.validation.validators.FilterAvgCpaOrCpiValidationBaseTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.model.ModelProperty

@RunWith(JUnitParamsRunner::class)
class AutobudgetAvgCpiValidatorProviderTest : FilterAvgCpaOrCpiValidationBaseTest<AutobudgetAvgCpi>() {
    override fun strategy(testData: Companion.TestData): AutobudgetAvgCpi =
        autobudgetAvgCpi()
            .withSum(testData.sum?.toBigDecimal())
            .withIsPayForConversionEnabled(testData.payForConversionEnabled)
            .withAvgCpi(testData.avgCpaOrCpiValue?.toBigDecimal())

    override fun testedModelProperty(): ModelProperty<*, BigDecimal> = AutobudgetAvgCpi.AVG_CPI

    override fun validatorProvider(): AbstractStrategyValidatorProvider<AutobudgetAvgCpi> =
        AutobudgetAvgCpiValidatorProvider

}

