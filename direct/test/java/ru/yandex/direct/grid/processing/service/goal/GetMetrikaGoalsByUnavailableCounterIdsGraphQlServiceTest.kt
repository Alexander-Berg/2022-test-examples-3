package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType.ACTION
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper

private const val USER_COUNTER_ID = 111L
private const val USER_GOAL_ID = 1111L
private const val UNAVAILABLE_COUNTER_ID = 112L
private const val UNAVAILABLE_GOAL_ID = 1121L
private const val NOT_ALLOWED_BY_METRIKA_COUNTER_ID = 113L
private const val NOT_ALLOWED_BY_METRIKA_GOAL_ID = 1131L
private const val UNAVAILABLE_ECOMMERCE_COUNTER_ID = 114L

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GetMetrikaGoalsByUnavailableCounterIdsGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var metrikaCampaignRepository: MetrikaCampaignRepository

    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository

    @Autowired
    private lateinit var retargetingGoalsPpcDictRepository: RetargetingGoalsPpcDictRepository

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val user = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        clientId = clientInfo.clientId!!
        steps.featureSteps().addClientFeature(
            clientId,
            FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true
        )
        steps.featureSteps().addClientFeature(
            clientId,
            FeatureName.CAMPAIGN_MODIFICATION_GOALS_VALIDATION_TIGHTENING, true
        )
        steps.featureSteps().addClientFeature(
            clientId,
            FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true
        )

        metrikaClient.addUserCounter(clientInfo.uid, USER_COUNTER_ID.toInt())
        metrikaClient.addCounterGoal(USER_COUNTER_ID.toInt(), USER_GOAL_ID.toInt())

        metrikaClient.addUnavailableCounter(UNAVAILABLE_COUNTER_ID)
        metrikaClient.addCounterGoal(UNAVAILABLE_COUNTER_ID.toInt(), UNAVAILABLE_GOAL_ID.toInt())

        metrikaClient.addUnavailableCounter(NOT_ALLOWED_BY_METRIKA_COUNTER_ID, false)
        metrikaClient.addCounterGoal(NOT_ALLOWED_BY_METRIKA_COUNTER_ID.toInt(), NOT_ALLOWED_BY_METRIKA_GOAL_ID.toInt())

        metrikaClient.addUnavailableEcommerceCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID)
    }

    @After
    fun after() {
        Mockito.reset(metrikaCounterByDomainRepository, metrikaGoalsService)
        metrikaClient.clearUnavailableCounters()
    }

    @Test
    fun getMetrikaGoalsByCounter_unavailableCounterAllowed() {
        val payload = ktGraphQLTestExecutor.getMetrikaGoalsByCounter(USER_COUNTER_ID, UNAVAILABLE_COUNTER_ID)
        val actualGoalIds = payload.goals.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrderElementsOf(listOf(USER_GOAL_ID, UNAVAILABLE_GOAL_ID))
    }

    @Test
    fun getMetrikaGoalsByCounter_unavailableCounterNotAllowedByMetrikaFlag() {
        val payload = ktGraphQLTestExecutor.getMetrikaGoalsByCounter(
            USER_COUNTER_ID, UNAVAILABLE_COUNTER_ID, NOT_ALLOWED_BY_METRIKA_COUNTER_ID
        )
        val actualGoalIds = payload.goals.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrderElementsOf(listOf(USER_GOAL_ID, UNAVAILABLE_GOAL_ID))
    }

    @Test
    fun getMetrikaGoalsByCounter_campaignUnavailableCounterNotAllowedByMetrikaFlag() {
        val campaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        metrikaCampaignRepository.addGoalIds(
            clientInfo.shard, campaignId, setOf(UNAVAILABLE_GOAL_ID, NOT_ALLOWED_BY_METRIKA_GOAL_ID)
        )
        retargetingGoalsPpcDictRepository.addMetrikaGoalsToPpcDict(
            setOf(UNAVAILABLE_GOAL_ID, NOT_ALLOWED_BY_METRIKA_GOAL_ID).map {
                Goal().withId(it).withType(GoalType.GOAL).withMetrikaCounterGoalType(ACTION) as Goal
            }.toSet()
        )

        val payload = ktGraphQLTestExecutor.getMetrikaGoalsByCounter(
            USER_COUNTER_ID, UNAVAILABLE_COUNTER_ID, NOT_ALLOWED_BY_METRIKA_COUNTER_ID, campaignId = campaignId
        )
        val actualGoalIds = payload.goals.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrderElementsOf(listOf(USER_GOAL_ID, UNAVAILABLE_GOAL_ID))
    }

    @Test
    fun getMetrikaGoalsByCounter_unavailableEcommerceCounter() {
        val payload = ktGraphQLTestExecutor.getMetrikaGoalsByCounter(
            USER_COUNTER_ID, UNAVAILABLE_COUNTER_ID, UNAVAILABLE_ECOMMERCE_COUNTER_ID
        )
        val actualGoalIds = payload.goals.map { it.id }
        assertThat(actualGoalIds).containsExactlyInAnyOrderElementsOf(listOf(
            USER_GOAL_ID, UNAVAILABLE_GOAL_ID, ecommerceGoalId(UNAVAILABLE_ECOMMERCE_COUNTER_ID)
        ))
    }
}
