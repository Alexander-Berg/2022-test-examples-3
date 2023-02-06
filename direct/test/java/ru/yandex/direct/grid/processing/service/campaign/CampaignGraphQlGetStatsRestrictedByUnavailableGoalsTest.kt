package ru.yandex.direct.grid.processing.service.campaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.model.GdEntityStats
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContext
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import java.math.BigDecimal

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class CampaignGraphQlGetStatsRestrictedByUnavailableGoalsTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        private const val GOAL_ID = 1111L
        private const val UNAVAILABLE_GOAL_ID = 1121L
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ytSupport: YtDynamicSupport

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        doReturn(setOf(GOAL_ID)).`when`(metrikaGoalsService)
            .getAvailableMetrikaGoalIdsForClientWithExceptionHandling(
                eq(clientInfo.uid), eq(clientInfo.clientId)
            )
        steps.featureSteps().addClientFeature(clientId, FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT, true)
    }

    @Suppress("unused")
    private fun testData() = listOf(
        listOf(true, setOf<Long>()),
        listOf(false, setOf(GOAL_ID, UNAVAILABLE_GOAL_ID)),
    )

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsNotAllowed_noGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        assertNotRestrictedStats(campaignsContext, statsByCid)
    }

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsAllowed_noGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        assertNotRestrictedStats(campaignsContext, statsByCid)
    }

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsAllowed_onlyAvailableGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val textCampaign1 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(GOAL_ID)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign1).id

        val textCampaign2 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            meaningfulGoals = listOf(MeaningfulGoal().apply { goalId = GOAL_ID })
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign2).id

        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        assertNotRestrictedStats(campaignsContext, statsByCid)
    }

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsAllowed_availableAndEngagedSessionGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val textCampaign1 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(GOAL_ID)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign1).id

        val textCampaign2 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            meaningfulGoals = listOf(MeaningfulGoal().apply { goalId = ENGAGED_SESSION_GOAL_ID })
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign2).id

        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        assertNotRestrictedStats(campaignsContext, statsByCid)
    }

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsAllowed_availableAndUnavailableGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val textCampaign1 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(GOAL_ID)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign1).id

        val textCampaign2 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(UNAVAILABLE_GOAL_ID, payForConversion = true)
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign2).id

        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        val campaignById = campaignsContext.rowset.associateBy { it.id }
        SoftAssertions.assertSoftly {
            val expectedStats1 = statsByCid[cid1]!!
            val expectedStats2 = statsByCid[cid2]!!
            assertStats(it, campaignById[cid1]!!.stats, expectedStats1)
            assertStats(it, campaignById[cid2]!!.stats, expectedStats2.apply { clicks = 0 })
            assertStats(it, campaignsContext.totalStats, expectedStats1)
        }
    }

    @Test
    @Parameters(method = "testData")
    fun unavailableGoalsAllowed_onlyUnavailableGoals(
        useCampaignGoalIds: Boolean,
        goalIds: Set<Long>
    ) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val textCampaign1 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(UNAVAILABLE_GOAL_ID, payForConversion = true)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign1).id

        val textCampaign2 = defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = createStrategy(payForConversion = false)
            meaningfulGoals = listOf(MeaningfulGoal().apply { goalId = UNAVAILABLE_GOAL_ID })
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, textCampaign2).id

        val statsByCid = mockYtStat(cid1, cid2)

        val campaignsContext = ktGraphQLTestExecutor.getTextCampaignsWithStatsAndAccess(
            clientInfo.uid, setOf(cid1, cid2), useCampaignGoalIds, goalIds)
        val campaignById = campaignsContext.rowset.associateBy { it.id }
        SoftAssertions.assertSoftly {
            val expectedStats1 = statsByCid[cid1]!!
            val expectedStats2 = statsByCid[cid2]!!
            assertStats(it, campaignById[cid1]!!.stats, expectedStats1.apply { clicks = 0 })
            assertStats(it, campaignById[cid2]!!.stats, expectedStats2)
            assertStats(it, campaignsContext.totalStats, GdEntityStats().apply {
                shows = 0
                clicks = 0
                cost = BigDecimal.ZERO
                goals = 0
            })
        }
    }

    private fun createStrategy(goalId: Long? = null, payForConversion: Boolean = false) =
        defaultAutobudgetStrategy(goalId).apply {
            strategyData.apply {
                this.payForConversion = payForConversion
            }
        }

    private fun mockYtStat(cid1: Long, cid2: Long): Map<Long, GdEntityStats> {
        val statsByCid = mapOf(
            cid1 to GdEntityStats().apply {
                shows = 22
                clicks = 33
                cost = BigDecimal.valueOf(44)
            },
            cid2 to GdEntityStats().apply {
                shows = 55
                clicks = 66
                cost = BigDecimal.valueOf(77)
            })
        val rowsetBuilder = RowsetBuilder()
        statsByCid.forEach { (cid, stat) ->
            rowsetBuilder.add(RowBuilder()
                .withColValue("effectiveCampaignId", cid)
                .withColValue(GdiEntityStats.CLICKS.name(), stat.clicks)
                .withColValue(GdiEntityStats.COST.name(), stat.cost.toLong() * 1_000_000)
                .withColValue(GdiEntityStats.SHOWS.name(), stat.shows)
            )
        }
        whenever(ytSupport.selectRows(any())).thenReturn(rowsetBuilder.build())
        return statsByCid
    }

    private fun assertNotRestrictedStats(campaignsContext: GdCampaignsContext, statsByCid: Map<Long, GdEntityStats>) {
        val campaignById = campaignsContext.rowset.associateBy { it.id }
        SoftAssertions.assertSoftly {
            val expectedTotalStats = GdEntityStats()
            campaignById.forEach { (cid, campaign) ->
                val expectedStats = statsByCid[cid]!!
                assertStats(it, campaign!!.stats, expectedStats)
                expectedTotalStats.apply {
                    shows = shows?.plus(expectedStats.shows) ?: expectedStats.shows
                    clicks = clicks?.plus(expectedStats.clicks) ?: expectedStats.clicks
                    cost = cost?.plus(expectedStats.cost) ?: expectedStats.cost
                }
            }
            assertStats(it, campaignsContext.totalStats, expectedTotalStats)
        }
    }

    private fun assertStats(soft: SoftAssertions, actualStats: GdEntityStats, expectedStats: GdEntityStats) {
        soft.assertThat(actualStats.shows).`as`("Shows").isEqualTo(expectedStats.shows)
        soft.assertThat(actualStats.clicks).`as`("Clicks").isEqualTo(expectedStats.clicks)
        soft.assertThat(actualStats.cost.toDouble()).`as`("Cost").isEqualTo(expectedStats.cost.toDouble())
    }
}
