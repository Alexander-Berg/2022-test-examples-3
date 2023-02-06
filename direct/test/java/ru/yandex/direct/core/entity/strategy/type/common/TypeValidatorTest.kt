package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMedia
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.CpmDefault
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CLICK
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPI
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPV
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_CRR
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_MAX_IMPRESSIONS
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_MAX_REACH
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_MEDIA
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_ROI
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_WEEK_BUNDLE
import ru.yandex.direct.core.entity.strategy.model.StrategyName.CPM_DEFAULT
import ru.yandex.direct.core.entity.strategy.model.StrategyName.DEFAULT_
import ru.yandex.direct.core.entity.strategy.model.StrategyName.PERIOD_FIX_BID
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class TypeValidatorTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        arrayOf(DefaultManualStrategy(), null, notNull()),
        //strategy types not null
        arrayOf(AutobudgetAvgClick(), AUTOBUDGET_AVG_CLICK, null),
        arrayOf(AutobudgetAvgCpcPerCamp(), AUTOBUDGET_AVG_CPC_PER_CAMP, null),
        arrayOf(AutobudgetAvgCpcPerFilter(), AUTOBUDGET_AVG_CPC_PER_FILTER, null),
        arrayOf(AutobudgetAvgCpv(), AUTOBUDGET_AVG_CPV, null),
        arrayOf(AutobudgetWeekBundle(), AUTOBUDGET_WEEK_BUNDLE, null),
        arrayOf(AutobudgetAvgCpvCustomPeriod(), AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD, null),
        arrayOf(AutobudgetAvgCpa(), AUTOBUDGET_AVG_CPA, null),
        arrayOf(AutobudgetAvgCpaPerCamp(), AUTOBUDGET_AVG_CPA_PER_CAMP, null),
        arrayOf(AutobudgetAvgCpaPerFilter(), AUTOBUDGET_AVG_CPA_PER_FILTER, null),
        arrayOf(AutobudgetAvgCpi(), AUTOBUDGET_AVG_CPI, null),
        arrayOf(AutobudgetCrr(), AUTOBUDGET_CRR, null),
        arrayOf(AutobudgetRoi(), AUTOBUDGET_ROI, null),
        arrayOf(AutobudgetWeekSum(), AUTOBUDGET, null),
        arrayOf(DefaultManualStrategy(), DEFAULT_, null),
        arrayOf(AutobudgetMaxImpressions(), AUTOBUDGET_MAX_IMPRESSIONS, null),
        arrayOf(AutobudgetMaxImpressionsCustomPeriod(), AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD, null),
        arrayOf(AutobudgetMaxReach(), AUTOBUDGET_MAX_REACH, null),
        arrayOf(AutobudgetMaxReachCustomPeriod(), AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, null),
        arrayOf(CpmDefault(), CPM_DEFAULT, null),
        arrayOf(PeriodFixBid(), PERIOD_FIX_BID, null),
        arrayOf(AutobudgetMedia(), AUTOBUDGET_MEDIA, null),
        //strategies types are null
        arrayOf(AutobudgetAvgClick(), null, notNull()),
        arrayOf(AutobudgetAvgCpcPerCamp(), null, notNull()),
        arrayOf(AutobudgetAvgCpcPerFilter(), null, notNull()),
        arrayOf(AutobudgetAvgCpv(), null, notNull()),
        arrayOf(AutobudgetWeekBundle(), null, notNull()),
        arrayOf(AutobudgetAvgCpvCustomPeriod(), null, notNull()),
        arrayOf(AutobudgetAvgCpa(), null, notNull()),
        arrayOf(AutobudgetAvgCpaPerCamp(), null, notNull()),
        arrayOf(AutobudgetAvgCpaPerFilter(), null, notNull()),
        arrayOf(AutobudgetAvgCpi(), null, notNull()),
        arrayOf(AutobudgetCrr(), null, notNull()),
        arrayOf(AutobudgetRoi(), null, notNull()),
        arrayOf(AutobudgetWeekSum(), null, notNull()),
        arrayOf(DefaultManualStrategy(), null, notNull()),
        arrayOf(AutobudgetMaxImpressions(), null, notNull()),
        arrayOf(AutobudgetMaxImpressionsCustomPeriod(), null, notNull()),
        arrayOf(AutobudgetMaxReach(), null, notNull()),
        arrayOf(AutobudgetMaxReachCustomPeriod(), null, notNull()),
        arrayOf(CpmDefault(), null, notNull()),
        arrayOf(PeriodFixBid(), null, notNull()),
        arrayOf(AutobudgetMedia(), null, notNull()),
        arrayOf(DefaultManualStrategy(), null, notNull())
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("strategy=[{0}], type=[{1}]")
    fun `validate strategy type`(commonStrategy: CommonStrategy, type: StrategyName?, defect: Defect<Void>? = null) {
        val validationResult = TypeValidator(commonStrategy.withType(type)).apply(type)

        defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(PathHelper.emptyPath(), it)
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }
}
