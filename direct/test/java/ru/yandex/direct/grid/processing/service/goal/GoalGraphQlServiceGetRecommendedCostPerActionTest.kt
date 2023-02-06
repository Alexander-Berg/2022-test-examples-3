package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.reset
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
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.schema.yt.Tables
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GoalGraphQlServiceGetRecommendedCostPerActionTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var ytSupport: YtDynamicSupport
    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private val counterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val queryCaptor = argumentCaptor<Select<*>>()

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var campaignId: Long? = null

    @Before
    fun before() {
        reset(ytSupport)
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!

        campaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id

        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId.toInt())
        metrikaClientStub.addUserCounter(clientInfo.uid, counterId.toInt())

        metrikaClientStub.addCounterGoal(unavailableCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(unavailableCounterId)
    }

    @Test
    fun getCpaForNewCampaign_unavailableGoalsNotEnabled() {
        mockYtSupport(goalId, unavailableGoalId)
        val cpaByGoalIds = ktGraphQLTestExecutor.getRecommendedGoalsCostPerActionForNewCampaign(
            clientInfo.uid, listOf(goalId, unavailableGoalId))

        assertWithUnavailableGoal(cpaByGoalIds)
    }

    @Test
    fun getCpaForNewCampaign_unavailableGoalsEnabled() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        mockYtSupport(goalId)
        val cpaByGoalIds = ktGraphQLTestExecutor.getRecommendedGoalsCostPerActionForNewCampaign(
            clientInfo.uid, listOf(goalId, unavailableGoalId))

        assertWithoutUnavailableGoal(cpaByGoalIds)
    }

    @Test
    fun getCpaForExistingCampaign_unavailableGoalsNotEnabled() {
        mockYtSupport(goalId, unavailableGoalId)
        val cpaByGoalIds = ktGraphQLTestExecutor.getRecommendedCampaignsGoalsCostPerAction(
            clientInfo.uid, campaignId!!, listOf(goalId, unavailableGoalId))

        assertWithUnavailableGoal(cpaByGoalIds)
    }

    @Test
    fun getCpaForExistingCampaign_unavailableGoalsEnabled() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        mockYtSupport(goalId)
        val cpaByGoalIds = ktGraphQLTestExecutor.getRecommendedCampaignsGoalsCostPerAction(
            clientInfo.uid, campaignId!!, listOf(goalId, unavailableGoalId))

        assertWithoutUnavailableGoal(cpaByGoalIds)
    }

    private fun mockYtSupport(vararg goalIds: Long) {
        // мокаем общую статистику по кампании
        val campaignRowSetBuilder = RowsetBuilder().add(RowBuilder()
            .withColValue("effectiveCampaignId", campaignId)
            .withColValue(GdiEntityStats.CLICKS.name(), 10)
            .withColValue(GdiEntityStats.SHOWS.name(), 11)
            .withColValue(GdiEntityStats.COST.name(), 12 * 1_000_000))
        whenever(ytSupport.selectRows(argWhere {
            !it.toString().contains(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID.name)
        })).thenReturn(campaignRowSetBuilder.build())

        // мокаем статистику по целям на кампании
        val goalsRowSetBuilder = RowsetBuilder()
        goalIds.forEach {
            goalsRowSetBuilder.add(RowBuilder()
                .withColValue("effectiveCampaignId", campaignId)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID, it)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM, 13)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR, 14)
            )
        }
        whenever(ytSupport.selectRows(argWhere {
            it.toString().contains(Tables.DIRECTGRIDGOALSSTAT_BS.GOAL_ID.name)
        })).thenReturn(goalsRowSetBuilder.build())
    }

    private fun assertWithUnavailableGoal(cpaByGoalIds: Map<Long, BigDecimal>) {
        verify(ytSupport, times(2)).selectRows(queryCaptor.capture())
        val query = queryCaptor.secondValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query contains available goal")
                .contains(goalId.toString())
            it.assertThat(query)
                .`as`("Query contains unavailable goal")
                .contains(unavailableGoalId.toString())
            it.assertThat(cpaByGoalIds[goalId])
                .`as`("Recommended cpa for available goal")
                .isPositive
            it.assertThat(cpaByGoalIds[unavailableGoalId])
                .`as`("Recommended cpa for unavailable goal")
                .isPositive
        }
    }

    private fun assertWithoutUnavailableGoal(cpaByGoalIds: Map<Long, BigDecimal>) {
        verify(ytSupport, times(2)).selectRows(queryCaptor.capture())
        val query = queryCaptor.secondValue.toString()
        SoftAssertions.assertSoftly {
            it.assertThat(query)
                .`as`("Query contains available goal")
                .contains(goalId.toString())
            it.assertThat(query)
                .`as`("Query does not contain unavailable goal")
                .doesNotContain(unavailableGoalId.toString())
            it.assertThat(cpaByGoalIds[goalId])
                .`as`("Recommended cpa for available goal")
                .isPositive
            it.assertThat(cpaByGoalIds[unavailableGoalId])
                .`as`("Recommended cpa for unavailable goal")
                .isNull()
        }
    }
}
