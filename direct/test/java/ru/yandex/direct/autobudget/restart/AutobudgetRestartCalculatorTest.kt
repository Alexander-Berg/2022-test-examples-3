package ru.yandex.direct.autobudget.restart

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.autobudget.restart.model.StrategyDto
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.autobudget.restart.service.*
import ru.yandex.direct.autobudget.restart.service.RestartDecision.Companion.MAX_LOCAL_DATE_TIME
import ru.yandex.direct.autobudget.restart.service.RestartType.*
import ru.yandex.direct.common.db.PpcPropertiesMock
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName
import java.time.LocalDate
import java.time.LocalDateTime

val BASE = StrategyDto(
    strategy = CampaignsStrategyName.default_.literal, platform = "both", manualStrategy = "different_places",
    startTime = LocalDate.parse("2020-01-01"), statusShow = true,
)
val MANUAL = BASE.copy()
val DAY_BUDGET = MANUAL.copy(strategy = CampaignsStrategyName.default_.literal, dayBudget = "12.3".toBigDecimal())
val AUTO_BUDGET = MANUAL.copy(
    strategy = CampaignsStrategyName.autobudget.literal, autoBudgetSum = "123.4".toBigDecimal())
val AUTO_BUDGET_CPM_PERIOD = AUTO_BUDGET.copy(
    strategy = CampaignsStrategyName.autobudget_max_reach_custom_period.literal,
    avgCpm = "1.34".toBigDecimal())
val AUTO_BUDGET_WEEK_BUNDLE = BASE.copy(strategy = CampaignsStrategyName.autobudget_week_bundle.literal)
val AUTO_BUDGET_AVG_CLICK = BASE.copy(strategy = CampaignsStrategyName.autobudget_avg_click.literal)
val AUTO_BUDGET_MAX_REACH = AUTO_BUDGET.copy(strategy = CampaignsStrategyName.autobudget_max_reach.literal)
val AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD = AUTO_BUDGET.copy(
    strategy = CampaignsStrategyName.autobudget_max_reach_custom_period.literal)
val AUTO_BUDGET_AVG_CPV_CUSTOM_PERIOD = AUTO_BUDGET.copy(
    strategy = CampaignsStrategyName.autobudget_avg_cpv_custom_period.literal)
val CPM_DEFAULT = AUTO_BUDGET.copy(strategy = CampaignsStrategyName.cpm_default.literal)
val PERIOD_FIX_BID = AUTO_BUDGET.copy(strategy = CampaignsStrategyName.period_fix_bid.literal)
val AUTO_BUDGET_FIX_CPA = AUTO_BUDGET.copy(
    strategy = CampaignsStrategyName.autobudget_avg_cpa.literal,
    payForConversion = true)
val AUTO_BUDGET_FIX_CPI = AUTO_BUDGET.copy(
    strategy = CampaignsStrategyName.autobudget_avg_cpi.literal,
    payForConversion = true)


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class AutobudgetRestartCalculatorTest {

    data class TestData(
        val description: String,
        val old: StrategyDto, val new: StrategyDto, val state: StrategyState,
        val expectedType: RestartType,
        val expectedRestartTime: LocalDateTime? = null,
        val times: RestartTimes? = null,
        val props: Map<String, Any> = emptyMap(),
        val orderId: Long = 1L,
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
                "manual: change platform",
                MANUAL,
                MANUAL.copy(platform = "search"),
                StrategyState(),
                NO_RESTART
            ),
            TestData(
                "manual to day_budget",
                MANUAL,
                DAY_BUDGET,
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget increase startTime",
                AUTO_BUDGET,
                AUTO_BUDGET.copy(startTime = future),
                StrategyState(),
                FULL, expectedRestartTime = future.atStartOfDay()
            ),
            TestData(
                "autobudget - start camp by finish_time increase",
                AUTO_BUDGET.copy(finishTime = weekAgo),
                AUTO_BUDGET.copy(finishTime = future),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget increase sum",
                AUTO_BUDGET_WEEK_BUNDLE.copy(autoBudgetSum = "123.4".toBigDecimal()),
                AUTO_BUDGET_WEEK_BUNDLE.copy(autoBudgetSum = "123.5".toBigDecimal()),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget new money with avgCpa",
                AUTO_BUDGET.copy(avgCpa = "1".toBigDecimal(), hasMoney = false),
                AUTO_BUDGET.copy(avgCpa = "1".toBigDecimal(), hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpa) campaign after long stop (due to lack of money)",
                AUTO_BUDGET_FIX_CPA.copy(hasMoney = false),
                AUTO_BUDGET_FIX_CPA.copy(hasMoney = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpi) campaign after long stop (due to lack of money)",
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
                AUTO_BUDGET_WEEK_BUNDLE.copy(avgBid = "1.4".toBigDecimal()),
                AUTO_BUDGET_WEEK_BUNDLE.copy(avgBid = "1.5".toBigDecimal()),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget decrease round bid",
                AUTO_BUDGET_AVG_CLICK.copy(avgBid = "25".toBigDecimal()),
                AUTO_BUDGET_AVG_CLICK.copy(avgBid = "20".toBigDecimal()),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget increase bid, but less then 4%",
                AUTO_BUDGET_WEEK_BUNDLE.copy(avgBid = "1.4".toBigDecimal()),
                AUTO_BUDGET_WEEK_BUNDLE.copy(avgBid = "1.41".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "start after 1h - no restart",
                AUTO_BUDGET.copy(statusShow = false),
                AUTO_BUDGET.copy(statusShow = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(1)),
                NO_RESTART,
            ),
            TestData(
                "start after 5h - full restart",
                AUTO_BUDGET.copy(statusShow = false),
                AUTO_BUDGET.copy(statusShow = true, avgBid = "1".toBigDecimal()),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpa) campaign after long stop (due to manual stop)",
                AUTO_BUDGET_FIX_CPA.copy(statusShow = false),
                AUTO_BUDGET_FIX_CPA.copy(statusShow = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "full restart for paid action (fix_cpi) campaign after long stop (due to manual stop)",
                AUTO_BUDGET_FIX_CPI.copy(statusShow = false),
                AUTO_BUDGET_FIX_CPI.copy(statusShow = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                FULL,
            ),
            TestData(
                "start after 5h - for cpm period - no restart",
                AUTO_BUDGET_CPM_PERIOD.copy(statusShow = false),
                AUTO_BUDGET_CPM_PERIOD.copy(statusShow = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                NO_RESTART,
            ),
            // cpa
            TestData(
                "cpm not-period - small change avgcpm",
                AUTO_BUDGET_MAX_REACH.copy(avgCpm = "1.34".toBigDecimal()),
                AUTO_BUDGET_MAX_REACH.copy(avgCpm = "1.34001".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "cpm not-period - significant change avgcpm",
                AUTO_BUDGET_MAX_REACH.copy(avgCpm = "1.34".toBigDecimal()),
                AUTO_BUDGET_MAX_REACH.copy(avgCpm = "1.4".toBigDecimal()),
                StrategyState(),
                FULL,
            ),
            TestData(
                "cpm period - small change avgcpm",
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(avgCpm = "1.34".toBigDecimal()),
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(avgCpm = "1.34001".toBigDecimal()),
                StrategyState(),
                SOFT,
            ),
            TestData(
                "cpm period - small change avgcpm and avgBid change, full restart should have priority",
                AUTO_BUDGET_CPM_PERIOD.copy(
                    avgCpm = "1.34".toBigDecimal(), avgBid = "1".toBigDecimal()
                ),
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(
                    avgCpm = "1.34001".toBigDecimal(), avgBid = "2".toBigDecimal()
                ),
                StrategyState(stopTime = LocalDateTime.now().minusHours(5)),
                FULL,
            ),
            TestData(
                "cpm period - change start_time in past, but greater than old restart time",
                AUTO_BUDGET_CPM_PERIOD.copy(
                    strategyStart = weekAgo
                ),
                AUTO_BUDGET_CPM_PERIOD.copy(
                    strategyStart = weekAgo.plusDays(4)
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
                AUTO_BUDGET_AVG_CPV_CUSTOM_PERIOD.copy(strategyStart = weekAgo),
                AUTO_BUDGET_AVG_CPV_CUSTOM_PERIOD.copy(strategyStart = weekAgo.plusDays(4)),
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
                CPM_DEFAULT.copy(dayBudget = "500".toBigDecimal()),
                CPM_DEFAULT.copy(dayBudget = "700".toBigDecimal()),
                StrategyState(),
                SOFT,
            ),
            TestData(
                "default - enabled cpc_hold",
                MANUAL,
                MANUAL.copy(enableCpcHold = true),
                StrategyState(),
                FULL,
            ),
            TestData(
                "default - disabled cpc_hold",
                MANUAL.copy(enableCpcHold = true),
                MANUAL,
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "default - enabled cpc_hold, switch platform",
                MANUAL.copy(platform = "search", enableCpcHold = true),
                MANUAL.copy(platform = "both", enableCpcHold = true),
                StrategyState(),
                FULL,
            ),
            TestData(
                "autobudget - no effect of strategy_start increase",
                AUTO_BUDGET.copy(strategyStart = LocalDate.now().minusDays(1)),
                AUTO_BUDGET.copy(strategyStart = LocalDate.now().plusDays(5)),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "custom_period - restart for strategy_start increase",
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(
                    avgCpm = "1.34".toBigDecimal(),
                    strategyStart = LocalDate.now().minusDays(1)
                ),
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(
                    avgCpm = "1.34".toBigDecimal(),
                    strategyStart = future
                ),
                StrategyState(),
                FULL, expectedRestartTime = future.atStartOfDay()
            ),
            TestData(
                "autobudget - no effect of autobudget after stop and start for 'all' cpm_price campaigns",
                PERIOD_FIX_BID.copy(statusShow = false),
                PERIOD_FIX_BID.copy(statusShow = true),
                StrategyState(stopTime = LocalDateTime.now().minusHours(100)),
                NO_RESTART
            ),
            TestData(
                "dayBudget -> manual with net_cpc_optimize",
                DAY_BUDGET.copy(platform = "both", enableCpcHold = true, manualStrategy = ""),
                MANUAL.copy(platform = "both", enableCpcHold = true, manualStrategy = ""),
                StrategyState(),
                SOFT,
            ),
            TestData(
                "period_fix_bid - finish_time increase for cpm_price",
                PERIOD_FIX_BID.copy(finishTime = weekAgo, platform = "context"),
                PERIOD_FIX_BID.copy(finishTime = future, platform = "context"),
                StrategyState(),
                NO_RESTART,
            ),
            TestData(
                "start time more greater than max timestamp",
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(
                    avgCpm = "1.34".toBigDecimal(),
                    strategyStart = LocalDate.of(2020, 1, 1)
                ),
                AUTO_BUDGET_MAX_REACH_CUSTOM_PERIOD.copy(
                    avgCpm = "1.34".toBigDecimal(),
                    strategyStart = LocalDate.of(2042, 1, 1)
                ),
                StrategyState(),
                FULL, expectedRestartTime = MAX_LOCAL_DATE_TIME
            ),
            TestData(
                "avg_cpa does not matter if properties are off",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART
            ),
            TestData(
                "avg_cpa matters if orderId is enabled in property",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                SOFT,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_ORDERIDS.name to setOf(12345L)),
                orderId = 12345L
            ),
            TestData(
                "avg_cpa does not matter if orderId is not listed in property",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_ORDERIDS.name to setOf(12346L)),
                orderId = 12345L
            ),
            TestData(
                "avg_cpa matters if partially enabled property is set and applies (percent)",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                SOFT,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_PERCENT.name to 50),
                orderId = 10009L
            ),
            TestData(
                "avg_cpa does not matter if partially enabled property is set but does not apply (percent)",
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "1.00".toBigDecimal()),
                AUTO_BUDGET_FIX_CPA.copy(avgCpa = "10.00".toBigDecimal()),
                StrategyState(),
                NO_RESTART,
                props = mapOf(PpcPropertyNames.ENABLE_NEW_CPA_AUTOBUDGET_RESTART_LOGIC_PERCENT.name to 50),
                orderId = 10051L
            ),
        )
    }

    private fun cmp(
        old: StrategyDto,
        new: StrategyDto,
        state: StrategyState,
        props: Map<String, Any>,
        times: RestartTimes? = null,
        orderId: Long
    ): RestartDecision {
        val calculator = AutobudgetRestartCalculator(PpcPropertiesMock.make(props))
        return calculator.compareStrategy(StrategyData(old), StrategyData(new), state, times, orderId)
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    fun typeOfRestart(data: TestData) {
        val res = cmp(data.old, data.new, data.state, data.props, data.times, data.orderId)

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
