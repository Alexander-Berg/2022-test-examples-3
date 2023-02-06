package ru.yandex.direct.core.entity.campaignstatistic.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaignstatistic.repository.CampaignGoalsStatisticRepository
import ru.yandex.direct.core.entity.campaignstatistic.repository.CampaignStatisticService
import ru.yandex.direct.core.entity.container.LocalDateRange
import ru.yandex.direct.grid.core.entity.model.GoalConversion
import ru.yandex.direct.grid.core.entity.model.campaign.AggregatorGoal
import java.time.LocalDate
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class CampaignStatisticServiceTest {
    private lateinit var campaignGoalsStatisticRepository: CampaignGoalsStatisticRepository
    private lateinit var campaignStatisticService: CampaignStatisticService

    @Before
    fun setUp() {
        campaignGoalsStatisticRepository = mock()
        campaignStatisticService = CampaignStatisticService(campaignGoalsStatisticRepository, mock())
    }

    @Test
    fun getCampaignGoalConversionsCount_emptyStat_AddZeroConversionCountForGoal() {
        whenever(campaignGoalsStatisticRepository
                .getCampaignGoalsConversionsCountByCampaignId(any(), any()))
                .thenReturn(mapOf())

        val now = LocalDate.now()
        val campaignId = 1L
        val goalId = 2L
        val conversions =
                campaignStatisticService.getCampaignGoalConversionsCount(
                        LocalDateRange()
                                .withFromInclusive(now)
                                .withToInclusive(now),
                        mapOf(Pair(campaignId, goalId)), emptyMap())

        softly {
            assertThat(conversions).isEqualTo(mapOf(Pair(
                    campaignId,
                    GoalConversion()
                            .withGoalId(goalId)
                            .withGoals(0L)
            )))
        }
    }

    @Test
    fun getCampaignGoalConversionsCount_hasAggregatedGoals_AddZeroConversionCountForGoal() {
        val now = LocalDate.now()
        val campaignId = 1L
        val aggregatedGoalId = 13L
        val goalIdFirst = 14L
        val goalIdSecond = 15L
        whenever(campaignGoalsStatisticRepository
                .getCampaignGoalsConversionsCountByCampaignId(any(), any()))
                .thenReturn(mapOf(Pair(campaignId,
                        listOf(GoalConversion().withGoalId(goalIdFirst).withGoals(5L),
                                GoalConversion().withGoalId(goalIdSecond).withGoals(15L)))))

        val conversions =
                campaignStatisticService.getCampaignGoalConversionsCount(
                        LocalDateRange()
                                .withFromInclusive(now)
                                .withToInclusive(now),
                        mapOf(Pair(campaignId, aggregatedGoalId)),
                        mapOf(Pair(campaignId,
                                listOf(AggregatorGoal()
                                        .withId(aggregatedGoalId)
                                        .withSubGoalIds(listOf(goalIdFirst, goalIdSecond))))))

        softly {
            assertThat(conversions).isEqualTo(mapOf(Pair(
                    campaignId,
                    GoalConversion()
                            .withGoalId(aggregatedGoalId)
                            .withGoals(20L)
            )))
        }
    }
}
