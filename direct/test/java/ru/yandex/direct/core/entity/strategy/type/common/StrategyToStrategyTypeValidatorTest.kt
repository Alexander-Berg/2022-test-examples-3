package ru.yandex.direct.core.entity.strategy.type.common

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
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
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class StrategyToStrategyTypeValidatorTest {
    fun testData(): Array<Array<Any?>> = arrayOf(
        //strategies match their strategy types
        arrayOf(AutobudgetAvgClick(), StrategyName.AUTOBUDGET_AVG_CLICK, null),
        arrayOf(AutobudgetAvgCpcPerCamp(), StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP, null),
        arrayOf(AutobudgetAvgCpcPerFilter(), StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER, null),
        arrayOf(AutobudgetAvgCpv(), StrategyName.AUTOBUDGET_AVG_CPV, null),
        arrayOf(AutobudgetWeekBundle(), StrategyName.AUTOBUDGET_WEEK_BUNDLE, null),
        arrayOf(AutobudgetAvgCpvCustomPeriod(), StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD, null),
        arrayOf(AutobudgetAvgCpa(), StrategyName.AUTOBUDGET_AVG_CPA, null),
        arrayOf(AutobudgetAvgCpaPerCamp(), StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP, null),
        arrayOf(AutobudgetAvgCpaPerFilter(), StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER, null),
        arrayOf(AutobudgetAvgCpi(), StrategyName.AUTOBUDGET_AVG_CPI, null),
        arrayOf(AutobudgetCrr(), StrategyName.AUTOBUDGET_CRR, null),
        arrayOf(AutobudgetRoi(), StrategyName.AUTOBUDGET_ROI, null),
        arrayOf(AutobudgetWeekSum(), StrategyName.AUTOBUDGET, null),
        arrayOf(DefaultManualStrategy(), StrategyName.DEFAULT_, null),
        arrayOf(AutobudgetMaxImpressions(), StrategyName.AUTOBUDGET_MAX_IMPRESSIONS, null),
        arrayOf(AutobudgetMaxImpressionsCustomPeriod(), StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD, null),
        arrayOf(AutobudgetMaxReach(), StrategyName.AUTOBUDGET_MAX_REACH, null),
        arrayOf(AutobudgetMaxReachCustomPeriod(), StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, null),
        arrayOf(CpmDefault(), StrategyName.CPM_DEFAULT, null),
        arrayOf(PeriodFixBid(), StrategyName.PERIOD_FIX_BID, null),
        arrayOf(AutobudgetMedia(), StrategyName.AUTOBUDGET_MEDIA, null),
        //strategies don't match their strategy types
        arrayOf(AutobudgetAvgClick(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(
            AutobudgetAvgCpcPerCamp(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(
            AutobudgetAvgCpcPerFilter(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(AutobudgetAvgCpv(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(AutobudgetWeekBundle(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(
            AutobudgetAvgCpvCustomPeriod(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(AutobudgetAvgCpa(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(
            AutobudgetAvgCpaPerCamp(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(
            AutobudgetAvgCpaPerFilter(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(AutobudgetAvgCpi(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(AutobudgetCrr(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(AutobudgetRoi(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(AutobudgetWeekSum(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(
            DefaultManualStrategy(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(
            AutobudgetMaxImpressions(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(
            AutobudgetMaxImpressionsCustomPeriod(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(AutobudgetMaxReach(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(
            AutobudgetMaxReachCustomPeriod(),
            StrategyName.CPM_DEFAULT,
            StrategyDefects.inconsistentStrategyToStrategyType()
        ),
        arrayOf(CpmDefault(), StrategyName.AUTOBUDGET_MEDIA, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(PeriodFixBid(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(AutobudgetMedia(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType()),
        arrayOf(DefaultManualStrategy(), StrategyName.CPM_DEFAULT, StrategyDefects.inconsistentStrategyToStrategyType())
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("strategy=[{0}], type=[{1}]")
    fun `validate strategy type`(commonStrategy: CommonStrategy, type: StrategyName?, defect: Defect<Void>? = null) {
        val validationResult = StrategyToStrategyTypeValidator().apply(commonStrategy.withType(type))

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
