package ru.yandex.direct.autobudget.restart

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.autobudget.restart.model.PackageStrategyDto
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.autobudget.restart.service.PackageStrategyAutobudgetRestartCalculator
import ru.yandex.direct.autobudget.restart.service.PackageStrategyData
import ru.yandex.direct.autobudget.restart.service.RestartDecision
import ru.yandex.direct.autobudget.restart.service.RestartDecision.Companion.MAX_LOCAL_DATE_TIME
import ru.yandex.direct.autobudget.restart.service.RestartType
import ru.yandex.direct.autobudget.restart.service.RestartType.FULL
import ru.yandex.direct.autobudget.restart.service.RestartType.NO_RESTART
import ru.yandex.direct.autobudget.restart.service.RestartType.SOFT
import ru.yandex.direct.autobudget.restart.service.StrategyState
import ru.yandex.direct.common.db.PpcPropertiesMock
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class PackageStrategyAutobudgetRestartCalculatorTest {
    val BASE = PackageStrategyDto(
        hasMoney = true,
        strategyType = StrategyName.AUTOBUDGET_CRR,
        isPublic = true
    )
    val MANUAL = BASE.copy()
    val DAY_BUDGET = MANUAL.copy(strategyType = StrategyName.DEFAULT_, dayBudget = 12.3.toBigDecimal())
    val AUTO_BUDGET = MANUAL.copy(strategyType = StrategyName.AUTOBUDGET, sum = 123.4.toBigDecimal())
    val AUTO_BUDGET_CPM_PERIOD = AUTO_BUDGET.copy(
        strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
        avgCpm = 1.34.toBigDecimal()
    )
    val AUTO_BUDGET_FIX_CPA = AUTO_BUDGET.copy(
        strategyType = StrategyName.AUTOBUDGET_AVG_CPA,
        isPayForConversionEnabled = true
    )
    val AUTO_BUDGET_FIX_CPI = AUTO_BUDGET.copy(
        strategyType = StrategyName.AUTOBUDGET_AVG_CPI,
        isPayForConversionEnabled = true
    )

    data class TestData(
        val description: String,
        val old: PackageStrategyDto,
        val new: PackageStrategyDto,
        val state: StrategyState,
        val expectedType: RestartType,
        val expectedRestartTime: LocalDateTime? = null,
        val times: RestartTimes? = null,
        val props: Map<String, Any> = emptyMap()
    ) {
        override fun toString() = description
    }

    /**
     * Набор тестовый данных для тестирования
     */
    fun testDataProvider(): List<TestData> {
        val weekAgo = LocalDate.now().minusDays(7)
        val future = LocalDate.now().plusDays(3)
        return listOf(
            TestData(
                "manual to day_budget",
                MANUAL,
                DAY_BUDGET,
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget increase sum",
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    sum = 123.4.toBigDecimal()
                ),
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    sum = 123.5.toBigDecimal()
                ),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget new money with avgCpa",
                AUTO_BUDGET.copy(avgCpa = BigDecimal.ONE, hasMoney = false),
                AUTO_BUDGET.copy(avgCpa = BigDecimal.ONE, hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpa) campaign after long stop",
                AUTO_BUDGET_FIX_CPA.copy(hasMoney = false),
                AUTO_BUDGET_FIX_CPA.copy(hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpi) campaign after long stop",
                AUTO_BUDGET_FIX_CPI.copy(hasMoney = false),
                AUTO_BUDGET_FIX_CPI.copy(hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "autobudget new money for cpm period - no restart",
                AUTO_BUDGET_CPM_PERIOD.copy(hasMoney = false),
                AUTO_BUDGET_CPM_PERIOD.copy(hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                NO_RESTART,
            ),
            TestData(
                "autobudget increase bid",
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    avgBid = 1.4.toBigDecimal()
                ),
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    avgBid = 1.5.toBigDecimal()
                ),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget decrease round bid",
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_AVG_CLICK,
                    avgBid = 25.toBigDecimal()
                ),
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_AVG_CLICK,
                    avgBid = 20.toBigDecimal()
                ),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget increase bid, but less then 4%",
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    avgBid = 1.4.toBigDecimal()
                ),
                BASE.copy(
                    strategyType = StrategyName.AUTOBUDGET_WEEK_BUNDLE,
                    avgBid = 1.41.toBigDecimal()
                ),
                StrategyState(),
                NO_RESTART,
            ),
            // cpa
            TestData(
                "cpm not-period - small change avgcpm",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH,
                    avgCpm = 1.34.toBigDecimal()
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH,
                    avgCpm = 1.34001.toBigDecimal()
                ),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "cpm not-period - significant change avgcpm",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH,
                    avgCpm = 1.34.toBigDecimal()
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH,
                    avgCpm = 1.4.toBigDecimal()
                ),
                StrategyState(),
                FULL,
            ),
            TestData(
                "cpm period - small change avgcpm",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34.toBigDecimal()
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34001.toBigDecimal()
                ),
                StrategyState(),
                SOFT,
            ),
            TestData(
                "cpm period - small change avgcpm and avgBid change, full restart should have priority",
                AUTO_BUDGET_CPM_PERIOD.copy(
                    avgCpm = 1.34.toBigDecimal(),
                    avgBid = 1.toBigDecimal()
                ),
                AUTO_BUDGET_CPM_PERIOD.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34001.toBigDecimal(),
                    avgBid = 2.toBigDecimal()
                ),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                FULL,
            ),
            TestData(
                "cpm period - change start_time in past, but greater than old restart time",
                AUTO_BUDGET_CPM_PERIOD.copy(
                    start = weekAgo
                ),
                AUTO_BUDGET_CPM_PERIOD.copy(
                    start = weekAgo.plusDays(4)
                ),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                times = RestartTimes(
                    restartTime = weekAgo.plusDays(2).atStartOfDay(),
                    softRestartTime = weekAgo.plusDays(2).atStartOfDay(),
                    restartReason = ""
                ),
                expectedType = FULL
            ),
            TestData(
                "cpm period - change start_time in past, but greater than old restart time",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD,
                    start = weekAgo
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD,
                    start = weekAgo.plusDays(4)
                ),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                times = RestartTimes(
                    restartTime = weekAgo.plusDays(2).atStartOfDay(),
                    softRestartTime = weekAgo.plusDays(2).atStartOfDay(),
                    restartReason = ""
                ),
                expectedType = FULL
            ),
            TestData(
                "cpm_default - dayBudget change",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.CPM_DEFAULT,
                    dayBudget = 500.toBigDecimal()
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.CPM_DEFAULT,
                    dayBudget = 700.toBigDecimal()
                ),
                StrategyState(),
                SOFT,
            ),
            TestData(
                "default - enabled cpc_hold",
                MANUAL,
                MANUAL.copy(enableCpcHold = true),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "default - disabled cpc_hold",
                MANUAL.copy(enableCpcHold = true),
                MANUAL,
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "autobudget - no effect of strategy_start increase",
                AUTO_BUDGET.copy(start = LocalDate.now().minusDays(1)),
                AUTO_BUDGET.copy(start = LocalDate.now().plusDays(5)),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "custom_period - restart for strategy_start increase",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34.toBigDecimal(),
                    start = LocalDate.now().minusDays(1)
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34.toBigDecimal(),
                    start = future
                ),
                StrategyState(),
                FULL, expectedRestartTime = future.atStartOfDay()
            ),
            TestData(
                "period_fix_bid - finish_time increase for cpm_price",
                AUTO_BUDGET.copy(
                    finish = weekAgo,
                    strategyType = StrategyName.PERIOD_FIX_BID
                ),
                AUTO_BUDGET.copy(
                    finish = future,
                    strategyType = StrategyName.PERIOD_FIX_BID
                ),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "start time more greater than max timestamp",
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34.toBigDecimal(),
                    start = LocalDate.of(2020, 1, 1)
                ),
                AUTO_BUDGET.copy(
                    strategyType = StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD,
                    avgCpm = 1.34.toBigDecimal(),
                    start = LocalDate.of(2042, 1, 1)
                ),
                StrategyState(),
                FULL,
                expectedRestartTime = MAX_LOCAL_DATE_TIME
            ),
            TestData(
                "avg_cpa does not matter if property is off",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART
            ),
            TestData(
                "avg_cpa matters if partially enabled property is set fully enabled >= 100%",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                SOFT,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_PERCENT.name to 100),
            ),
            TestData(
                "avg_cpa does not matter if partially enabled property is not set",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "avg_cpa does not matter if partially enabled property is not fully enabled (< 100%)",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_PERCENT.name to 50),
            ),
        )
    }

    private fun cmp(
        old: PackageStrategyDto,
        new: PackageStrategyDto,
        state: StrategyState,
        props: Map<String, Any>,
        times: RestartTimes? = null
    ): RestartDecision {
        val calculator = PackageStrategyAutobudgetRestartCalculator(PpcPropertiesMock.make(props))
        return calculator.compareStrategy(PackageStrategyData(old), PackageStrategyData(new), state, times)
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    fun typeOfRestart(data: TestData) {
        val res = cmp(data.old, data.new, data.state, data.props, data.times)

        val soft = SoftAssertions()
        soft.assertThat(res.type)
            .describedAs("${data.description} - restart type (${res.reason})")
            .isEqualTo(data.expectedType)
        soft.assertThat(res.time)
            .describedAs("${data.description} - time")
            .isEqualTo(data.expectedRestartTime)
        soft.assertAll()
    }
}

