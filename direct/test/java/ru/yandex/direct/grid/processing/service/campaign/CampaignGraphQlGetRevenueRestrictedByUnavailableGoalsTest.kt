package ru.yandex.direct.grid.processing.service.campaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.jooq.Select
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.schema.yt.Tables
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.utils.DateTimeUtils.getNowEpochSeconds
import ru.yandex.direct.ytwrapper.dynamic.dsl.YtQueryUtil.DECIMAL_MULT
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignGraphQlGetRevenueRestrictedByUnavailableGoalsTest {

    companion object {
        private const val REVENUE_OF_ALL_GOALS = 50
        private const val REVENUE_OF_AVAILABLE_GOAL = 30
        private const val REVENUE_OF_UNAVAILABLE_GOAL = 10
        private const val REVENUE_OF_STRATEGY_GOALS = REVENUE_OF_AVAILABLE_GOAL + REVENUE_OF_UNAVAILABLE_GOAL
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ytSupport: YtDynamicSupport

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    private val queryCaptor = argumentCaptor<Select<*>>()

    private val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger().toLong()

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var campaignId: Long = -1

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        campaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id

        doReturn(setOf(goalId)).`when`(metrikaGoalsService)
            .getAvailableMetrikaGoalIdsForClientWithExceptionHandling(
                eq(clientInfo.uid), eq(clientInfo.clientId)
            );
    }

    @Test
    fun useCampaignGoals_FeatureIsOff() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GET_REVENUE_ONLY_BY_AVAILABLE_GOALS, false)
        mockYtSupportForCampaignGoals()

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(campaignId), true, setOf())

        verify(ytSupport, times(2)).selectRows(queryCaptor.capture())
        val query = queryCaptor.secondValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query does not contain available goal")
                .doesNotContain(goalId.toString())
            it.assertThat(query)
                .`as`("Query does not contain unavailable goal")
                .doesNotContain(unavailableGoalId.toString())
            it.assertThat(campaignsContext.rowset[0].stats.revenue.toDouble())
                .`as`("Revenue from strategy goals")
                .isEqualTo(REVENUE_OF_STRATEGY_GOALS.toDouble())
        }
    }

    @Test
    fun useCampaignGoals_FeatureIsOn() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GET_REVENUE_ONLY_BY_AVAILABLE_GOALS, true)
        mockYtSupportForCampaignGoals()

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(campaignId), true, setOf())

        verify(ytSupport, times(2)).selectRows(queryCaptor.capture())
        val query = queryCaptor.secondValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query contains available goal")
                .contains(goalId.toString())
            it.assertThat(query)
                .`as`("Query does not contain unavailable goal")
                .doesNotContain(unavailableGoalId.toString())
            it.assertThat(campaignsContext.rowset[0].stats.revenue.toDouble())
                .`as`("Revenue from available strategy goals")
                .isEqualTo(REVENUE_OF_AVAILABLE_GOAL.toDouble())
        }
    }

    @Test
    fun specificGoals_FeatureIsOff() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GET_REVENUE_ONLY_BY_AVAILABLE_GOALS, false)
        mockYtSupportForSpecificGoals()

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(campaignId), false, setOf(goalId, unavailableGoalId))

        verify(ytSupport, times(1)).selectRows(queryCaptor.capture())
        val query = queryCaptor.firstValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query contains available goal")
                .contains(goalId.toString())
            it.assertThat(query)
                .`as`("Query contains unavailable goal")
                .contains(unavailableGoalId.toString())
            it.assertThat(query)
                .`as`("Query contains revenue for unavailable goal")
                .contains(GdiEntityStats.REVENUE.name() + unavailableGoalId.toString())
            it.assertThat(campaignsContext.rowset[0].stats.revenue.toDouble())
                .`as`("Revenue from strategy goals")
                .isEqualTo(REVENUE_OF_STRATEGY_GOALS.toDouble())
        }
    }

    @Test
    fun specificGoals_FeatureIsOn() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GET_REVENUE_ONLY_BY_AVAILABLE_GOALS, true)
        mockYtSupportForSpecificGoals()

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(campaignId), false, setOf(goalId, unavailableGoalId))

        verify(ytSupport, times(1)).selectRows(queryCaptor.capture())
        val query = queryCaptor.firstValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query contains available goal")
                .contains(goalId.toString())
            it.assertThat(query)
                .`as`("Query contains unavailable goal")
                .contains(unavailableGoalId.toString())
            it.assertThat(campaignsContext.rowset[0].stats.revenue.toDouble())
                .`as`("Revenue from available strategy goals")
                .isEqualTo(REVENUE_OF_AVAILABLE_GOAL.toDouble())
        }
    }

    private fun mockYtSupportForCampaignGoals() {
        // мокаем общую статистику по кампании
        val campaignRowSetBuilder = RowsetBuilder().add(RowBuilder()
            .withColValue("effectiveCampaignId", campaignId)
            .withColValue(GdiEntityStats.CLICKS.name(), 10)
            .withColValue(GdiEntityStats.SHOWS.name(), 11)
            .withColValue(GdiEntityStats.COST.name(), 12 * 1_000_000)
            .withColValue(GdiEntityStats.REVENUE.name(), REVENUE_OF_ALL_GOALS * DECIMAL_MULT.toDouble()))
        whenever(ytSupport.selectRows(argWhere {
            it.toString().contains(Tables.DIRECTGRIDSTAT_BS.name)
        })).thenReturn(campaignRowSetBuilder.build())

        // мокаем статистику статистику по конверсиям для случая когда НЕ фильтруем по доступным целям
        var goalsRowSetBuilder = RowsetBuilder().add(RowBuilder()
            .withColValue("effectiveCampaignId", campaignId)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM, 13)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR, REVENUE_OF_STRATEGY_GOALS)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME, getNowEpochSeconds())
        )
        whenever(ytSupport.selectRows(argWhere {
            val query = it.toString()
            query.contains(Tables.DIRECTGRIDGOALSSTAT_BS.name) &&
                !query.contains(Tables.DIRECTGRIDSTAT_BS.name) &&
                !query.contains(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID.name)
        })).thenReturn(goalsRowSetBuilder.build())

        // мокаем статистику статистику по конверсиям для случая когда фильтруем по доступным целям
        goalsRowSetBuilder = RowsetBuilder().add(RowBuilder()
            .withColValue("effectiveCampaignId", campaignId)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM, 13)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR, REVENUE_OF_AVAILABLE_GOAL)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME, getNowEpochSeconds())
        )
        whenever(ytSupport.selectRows(argWhere {
            val query = it.toString()
            query.contains(Tables.DIRECTGRIDGOALSSTAT_BS.name) &&
                !query.contains(Tables.DIRECTGRIDSTAT_BS.name) &&
                query.contains(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID.name)
        })).thenReturn(goalsRowSetBuilder.build())
    }

    private fun mockYtSupportForSpecificGoals() {
        // мокаем общую статистику по кампании
        val rowSetBuilder = RowsetBuilder().add(RowBuilder()
            .withColValue("effectiveCampaignId", campaignId)
            .withColValue(GdiEntityStats.CLICKS.name(), 10)
            .withColValue(GdiEntityStats.SHOWS.name(), 11)
            .withColValue(GdiEntityStats.COST.name(), 12 * 1_000_000)
            .withColValue(GdiEntityStats.REVENUE.name(),
                REVENUE_OF_ALL_GOALS * DECIMAL_MULT.toDouble())
            .withColValue(GdiEntityStats.REVENUE.name() + goalId,
                REVENUE_OF_AVAILABLE_GOAL * DECIMAL_MULT.toDouble())
            .withColValue(GdiEntityStats.REVENUE.name() + unavailableGoalId,
                REVENUE_OF_UNAVAILABLE_GOAL * DECIMAL_MULT.toDouble()))
        whenever(ytSupport.selectRows(any())).thenReturn(rowSetBuilder.build())
    }
}
