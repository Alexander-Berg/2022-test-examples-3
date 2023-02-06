package ru.yandex.direct.grid.processing.service.campaign

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.RandomNumberUtils

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignGoalsMassGetGoalsForUpdateCampaignsGraphQLServiceTest {
    @Autowired
    private lateinit var steps: Steps
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

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!

        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId.toInt())
        metrikaClientStub.addUserCounter(clientInfo.uid, counterId.toInt())

        metrikaClientStub.addCounterGoal(unavailableCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(unavailableCounterId)
    }

    @After
    fun after() {
        reset(metrikaGoalsService)
    }

    @Test
    fun testCampaignWithoutCounters() {
        val campaignInfo = createCampaign()

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId)
    }

    @Test
    fun testCampaignWithAvailableCounter() {
        val campaignInfo = createCampaign(listOf(counterId))

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId)
    }

    @Test
    fun testCampaignWithUnavailableCounter_featureIsOn() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val campaignInfo = createCampaign(listOf(unavailableCounterId))

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId, unavailableGoalId)
    }

    @Test
    fun testCampaignWithUnavailableCounter_featureIsOff() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false)
        val campaignInfo = createCampaign(listOf(unavailableCounterId))

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId)
    }

    @Test
    fun testCampaignWithAvailableAndUnavailableCounter_featureIsOn() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val campaignInfo = createCampaign(listOf(counterId, unavailableCounterId))

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId, unavailableGoalId)
    }

    @Test
    fun testCampaignWithAvailableAndUnavailableCounter_featureIsOff() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false)
        val campaignInfo = createCampaign(listOf(counterId, unavailableCounterId))

        val payload = ktGraphQLTestExecutor.getGoalsForUpdateCampaigns(campaignInfo.id)
        val actualGoalIds = payload.rowset.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrder(goalId)
    }

    private fun createCampaign(counterIds: List<Long>? = null): TextCampaignInfo {
        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
        textCampaign.apply {
            counterIds?.let { metrikaCounters = it }
        }
        return steps.textCampaignSteps().createCampaign(clientInfo, textCampaign)
    }
}
