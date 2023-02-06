package ru.yandex.direct.grid.processing.service.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.entity.model.GdiGoalStats
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats
import ru.yandex.direct.grid.model.campaign.GdTextCampaign
import java.math.BigDecimal

@RunWith(JUnitParamsRunner::class)
class ConversionStrategyLearningStatusDataLoaderGoalsCrrTest {

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun checkGoalsCrr(testData: TestData) {
        assertThat(testData.learningData.goalsCrr()).isEqualTo(testData.expectedCrr)
    }

    fun testData(): List<TestData> {
        return listOf(
            TestData(learningData(stats(null, emptyList())), null),
            TestData(learningData(stats(BigDecimal.ZERO, emptyList())), null),
            TestData(learningData(stats(BigDecimal.ZERO, listOf(goalStats(100)))), BigDecimal.ZERO.setScale(1)),
            TestData(learningData(stats(BigDecimal.TEN, listOf(goalStats(0)))), null),
            TestData(learningData(stats(BigDecimal.TEN, listOf(goalStats(100)))), BigDecimal.TEN.setScale(1)),
            TestData(learningData(stats(BigDecimal.TEN, listOf(goalStats(50), goalStats(20)))), BigDecimal.valueOf(143, 1)),
            TestData(learningData(stats(BigDecimal.TEN, listOf(goalStats(10), goalStats(20)))), BigDecimal.valueOf(333, 1)),
            TestData(learningData(stats(BigDecimal.TEN, listOf(goalStats(15), goalStats(20), goalStats(5)))), BigDecimal.valueOf(250, 1)),
        )
    }

    private fun learningData(stats: GdiCampaignStats) =
        ConversionStrategyWithCampaignLearningData(GdTextCampaign(), null, null, stats, emptyStats())

    private fun stats(cost: BigDecimal?, goalStats: List<GdiGoalStats>): GdiCampaignStats =
        GdiCampaignStats()
            .withStat(GdiEntityStats().withCost(cost))
            .withGoalStats(goalStats)

    private fun emptyStats(): GdiCampaignStats =
        GdiCampaignStats()
            .withStat(GdiEntityStats())

    private fun goalStats(revenue: Long): GdiGoalStats =
        GdiGoalStats().withRevenue(revenue)

    data class TestData(val learningData: ConversionStrategyWithCampaignLearningData, val expectedCrr: BigDecimal?) {
        override fun toString(): String {
            return "TestData(stat.cost=${learningData.stats.stat.cost}" +
                ", goalStats=${learningData.stats.goalStats}}" +
                ", expectedCrr=$expectedCrr)"
        }
    }
}
