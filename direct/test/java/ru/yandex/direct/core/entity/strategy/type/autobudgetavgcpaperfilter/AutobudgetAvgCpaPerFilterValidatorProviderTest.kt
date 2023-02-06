package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpaperfilter

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.validation.AbstractStrategyValidatorProvider
import ru.yandex.direct.core.entity.strategy.validation.validators.FilterAvgCpaOrCpiValidationBaseTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.model.ModelProperty

@RunWith(JUnitParamsRunner::class)
class AutobudgetAvgCpaPerFilterValidatorProviderTest :
    FilterAvgCpaOrCpiValidationBaseTest<AutobudgetAvgCpaPerFilter>() {
    override fun strategy(testData: Companion.TestData): AutobudgetAvgCpaPerFilter =
        autobudgetAvgCpaPerFilter()
            .withSum(testData.sum?.toBigDecimal())
            .withIsPayForConversionEnabled(testData.payForConversionEnabled)
            .withFilterAvgCpa(testData.avgCpaOrCpiValue?.toBigDecimal())
            .withIsPublic(false)

    override fun testedModelProperty(): ModelProperty<*, BigDecimal> = AutobudgetAvgCpaPerFilter.FILTER_AVG_CPA

    override fun validatorProvider(): AbstractStrategyValidatorProvider<AutobudgetAvgCpaPerFilter> =
        AutobudgetAvgCpaPerFilterValidatorProvider

}
