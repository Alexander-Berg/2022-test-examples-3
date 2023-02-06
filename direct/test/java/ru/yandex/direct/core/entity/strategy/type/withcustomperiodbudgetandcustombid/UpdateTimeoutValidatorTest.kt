package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.time.LocalDate
import java.time.LocalDateTime
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CpmCampaignWithCustomStrategyBeforeApplyValidator.CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.validation.result.Defect

@RunWith(JUnitParamsRunner::class)
internal class UpdateTimeoutValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf(
            "valid timeout: strategies types are different",
            TestData(
                clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(1),
                modelChanges = ModelChanges(1, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
                    .process(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD, CommonStrategy.TYPE)
            )
        ),
        arrayOf(
            "invalid timeout",
            TestData(
                clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(2)
                    .withDailyChangeCount(CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT.plus(1L))
                    .withLastUpdateTime(LocalDateTime.now()),
                modelChanges = ModelChanges(2, StrategyWithCustomPeriodBudgetAndCustomBid::class.java),
                defect = StrategyDefects.strategyChangingLimitWasExceeded(CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT)
            )
        ),
        arrayOf(
            "valid timeout: now is before modified strategy start",
            TestData(
                clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(2)
                    .withDailyChangeCount(CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT.plus(1L))
                    .withLastUpdateTime(LocalDateTime.now())
                    .withStart(LocalDate.now().plusDays(1)),
                modelChanges = ModelChanges(2, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            )
        ),
        arrayOf(
            "valid timeout: last update time is null",
            TestData(
                clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(3)
                    .withDailyChangeCount(CPM_STRATEGY_MAX_DAILY_CHANGE_COUNT.plus(1L))
                    .withLastUpdateTime(null),
                modelChanges = ModelChanges(3, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            )
        ),
        arrayOf(
            "valid timeout: modified strategy start is different from unmodified strategy start",
            TestData(
                clientAutobudgetMaxImpressionsCustomPeriodStrategy().withId(4),
                modelChanges = ModelChanges(4, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
                    .process(
                        clientAutobudgetMaxImpressionsCustomPeriodStrategy().start.minusDays(1),
                        StrategyWithCustomPeriodBudgetAndCustomBid.START
                    )
            )
        )
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val constraint = UpdateTimeoutValidator.validate(testData.strategy, testData.now)
        val validationResult = constraint.apply(testData.modelChanges)

        testData.defect?.let {
            Assert.assertEquals(
                validationResult,
                it
            )
        } ?: Assert.assertNull(validationResult)
    }

    companion object {
        private val NOW = LocalDate.now()

        data class TestData(
            val strategy: StrategyWithCustomPeriodBudgetAndCustomBid,
            val now: LocalDate = NOW,
            val modelChanges: ModelChanges<StrategyWithCustomPeriodBudgetAndCustomBid>,
            val defect: Defect<*>? = null
        )
    }
}

