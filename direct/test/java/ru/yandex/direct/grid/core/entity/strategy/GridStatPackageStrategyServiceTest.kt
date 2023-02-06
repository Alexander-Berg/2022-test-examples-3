package ru.yandex.direct.grid.core.entity.strategy

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignServiceUtils.PERCENT_MULTIPLIER
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.entity.model.GdiGoalConversion
import ru.yandex.direct.grid.core.entity.model.GdiGoalStats
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats
import ru.yandex.direct.grid.core.entity.strategy.repository.GridPackageStrategyYtRepository
import ru.yandex.direct.multitype.repository.filter.Filter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

internal class GridStatPackageStrategyServiceTest {

    private lateinit var gridStatRepository: GridPackageStrategyYtRepository
    private lateinit var campaignTypedRepository: CampaignTypedRepository
    private lateinit var shardHelper: ShardHelper
    private lateinit var metrikaGoalsService: MetrikaGoalsService
    private lateinit var gridStatService: GridStatPackageStrategyService

    private val campaign1 = TestCampaigns.defaultTextCampaign().withId(1L)
    private val campaign2 = TestCampaigns.defaultTextCampaign().withId(2L)
    private val goalId1 = 1L
    private val goalId2 = 2L

    private val clientId = ClientId.fromLong(1L)
    private val operatorUid = 1L

    private val today = LocalDate.now()
    private val tomorrow = today.plusDays(1)
    private val strategyId1 = 1L
    private val strategyId2 = 2L

    private val entityStats = GdiEntityStats()
        .withCost(BigDecimal.TEN)
        .withClicks(BigDecimal.TEN)

    private val entityGoalsStat1 = GdiGoalConversion().withGoalId(goalId1).withGoals(20).withRevenue(50)
    private val entityGoalsStat2 = GdiGoalConversion().withGoalId(goalId2).withGoals(20).withRevenue(50)

    private val goalConversions = listOf(entityGoalsStat1, entityGoalsStat2)

    @Before
    fun setUp() {
        gridStatRepository = mock(GridPackageStrategyYtRepository::class.java)
        campaignTypedRepository = mock(CampaignTypedRepository::class.java)
        shardHelper = mock(ShardHelper::class.java)
        metrikaGoalsService = mock(MetrikaGoalsService::class.java)

        whenever(shardHelper.getShardByClientId(any()))
            .thenReturn(1)

        whenever(
            campaignTypedRepository.getSafely(
                anyInt(),
                any<Filter>(),
                any()
            )
        ).thenReturn(listOf(campaign1, campaign2))

        gridStatService = GridStatPackageStrategyService(
            gridStatRepository,
            campaignTypedRepository,
            shardHelper,
            metrikaGoalsService
        )
    }

    @Test
    fun `success stats by goals on empty`() {
        whenever(gridStatRepository.strategyEntityStats(any()))
            .thenReturn(emptyMap())
        whenever(gridStatRepository.strategyGoalsConversions(any(), any(), anyOrNull()))
            .thenReturn(emptyMap())
        val result = gridStatService.getStatsByGoals(
            clientId,
            operatorUid,
            setOf(strategyId1, strategyId2),
            today,
            tomorrow,
            setOf(goalId1, goalId2),
            true
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `success stats for assigned goals on empty`() {
        whenever(gridStatRepository.strategyEntityStats(any()))
            .thenReturn(emptyMap())
        whenever(gridStatRepository.strategyGoalsConversions(any(), any(), anyOrNull()))
            .thenReturn(emptyMap())
        val result = gridStatService.getStatsForAssignedGoals(
            clientId,
            operatorUid,
            setOf(strategyId1, strategyId2),
            today,
            tomorrow,
            true
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `success stats by goals`() {
        whenever(gridStatRepository.strategyEntityStats(any()))
            .thenReturn(mapOf(strategyId1 to entityStats, strategyId2 to entityStats))
        whenever(gridStatRepository.strategyGoalsConversions(any(), any(), anyOrNull()))
            .thenReturn(mapOf(strategyId1 to goalConversions, strategyId2 to goalConversions))
        val result = gridStatService.getStatsByGoals(
            clientId,
            operatorUid,
            setOf(strategyId1, strategyId2),
            today,
            tomorrow,
            setOf(goalId1, goalId2),
            true
        )
        val goalStats = goalConversions.map {
            GdiGoalStats()
                .withGoalId(it.goalId)
                .withGoals(it.goals)
                .withConversionRate(conversionRate(entityStats, it))
                .withCostPerAction(costPerAction(entityStats, it))
                .withRevenue(it.revenue)
        }
        val expected = GdiCampaignStats()
            .withStat(entityStats)
            .withGoalStats(goalStats)
        assertThat(result).isEqualTo(mapOf(strategyId1 to expected, strategyId2 to expected))
    }

    @Test
    fun `success stats for assigned goals`() {
        whenever(gridStatRepository.strategyEntityStats(any()))
            .thenReturn(mapOf(strategyId1 to entityStats, strategyId2 to entityStats))
        whenever(gridStatRepository.strategyGoalsConversions(any(), any(), anyOrNull()))
            .thenReturn(mapOf(strategyId1 to goalConversions, strategyId2 to goalConversions))
        val result = gridStatService.getStatsForAssignedGoals(
            clientId,
            operatorUid,
            setOf(strategyId1, strategyId2),
            today,
            tomorrow,
            true
        )
        val sum = goalConversions.reduce { a, b ->
            a.revenue += b.revenue
            a.goals += b.goals
            a
        }
        val expected = GdiCampaignStats()
            .withStat(
                GdiEntityStats()
                    .withClicks(entityStats.clicks)
                    .withCost(entityStats.cost)
                    .withRevenue(sum.revenue.toBigDecimal())
                    .withGoals(sum.goals.toBigDecimal())
                    .withConversionRate(conversionRate(entityStats, sum).setScale(2))
                    .withAvgGoalCost(BigDecimal.ZERO)
                    .withProfitability(
                        (sum.revenue.toBigDecimal() - entityStats.cost).divide(
                            entityStats.cost,
                            RoundingMode.HALF_UP
                        )
                    )
                    .withCrr(
                        (entityStats.cost * PERCENT_MULTIPLIER.toBigDecimal()).divide(
                            sum.revenue.toBigDecimal(),
                            RoundingMode.HALF_UP
                        )
                    )
            )
        assertThat(result).isEqualTo(mapOf(strategyId1 to expected, strategyId2 to expected))
    }

    private fun costPerAction(entityStats: GdiEntityStats, goalConversion: GdiGoalConversion) =
        entityStats.cost.divide(goalConversion.goals.toBigDecimal(), RoundingMode.HALF_UP)

    private fun conversionRate(entityStats: GdiEntityStats, goalConversion: GdiGoalConversion) =
        BigDecimal.valueOf(goalConversion.goals * PERCENT_MULTIPLIER)
            .divide(entityStats.clicks, RoundingMode.HALF_UP)
}
