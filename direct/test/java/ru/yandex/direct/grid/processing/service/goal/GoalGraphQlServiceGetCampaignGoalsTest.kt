package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.RandomNumberUtils

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GoalGraphQlServiceGetCampaignGoalsTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    @Autowired
    private lateinit var metrikaCampaignRepository: MetrikaCampaignRepository
    @Autowired
    private lateinit var retargetingGoalsPpcDictRepository: RetargetingGoalsPpcDictRepository
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private val counterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val notAllowedByMetrikaCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val notAllowedByMetrikaGoalId = RandomNumberUtils.nextPositiveInteger().toLong()

    private lateinit var operator: User
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = UserHelper.getUser(clientInfo.client!!)
        TestAuthHelper.setDirectAuthentication(operator)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(operator)

        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId.toInt())
        metrikaClientStub.addUserCounter(clientInfo.uid, counterId.toInt())

        metrikaClientStub.addCounterGoal(unavailableCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(unavailableCounterId)

        metrikaClientStub.addCounterGoal(notAllowedByMetrikaCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(notAllowedByMetrikaGoalId, false)
    }

    @After
    fun after() {
        reset(metrikaGoalsService)
    }

    /**
     * Получаем все цели со всех кампаний
     */
    @Test
    fun testGetByAllCampaigns_allGoalsAllowed() {
        val campaign1 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, campaign1).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = null,
            expectedGoalIdToCounterId = mapOf(
                goalId to counterId,
                unavailableGoalId to unavailableCounterId,
                notAllowedByMetrikaGoalId to null,
            ),
        )
    }

    /**
     * Получаем все цели с переданных кампаний
     */
    @Test
    fun testGetBySpecificCampaigns_allGoalsAllowed() {
        val campaign1 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, campaign1).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = setOf(cid1, cid2),
            expectedGoalIdToCounterId = mapOf(
                goalId to counterId,
                unavailableGoalId to unavailableCounterId,
                notAllowedByMetrikaGoalId to null,
            ),
        )
    }

    /**
     * Получаем доступную цель, а также недоступную цель с кампании, для которой включен флажок Метрики
     */
    @Test
    fun testGetByAllCampaigns_availableAndAllowedByMetrikaGoals_campaignWithCounters() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val campaign1 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId, notAllowedByMetrikaCounterId)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, campaign1).id
        val campaign2 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId, notAllowedByMetrikaCounterId)
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, campaign2).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = null,
            expectedGoalIdToCounterId = mapOf(
                goalId to counterId,
                unavailableGoalId to unavailableCounterId,
            ),
        )
    }

    /**
     * Получаем доступную цель, но недоступную цель, для которой включен флажок Метрики - не получаем,
     * т.к. счетчик не на кампании
     */
    @Test
    fun testGetByAllCampaigns_availableAndAllowedByMetrikaGoals_campaignWithoutCounters() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = null,
            expectedGoalIdToCounterId = mapOf(goalId to counterId),
        )
    }

    /**
     * Получаем доступную цель, а также недоступную цель с кампании, для которой включен флажок Метрики
     */
    @Test
    fun testGetBySpecificCampaigns_availableAndAllowedByMetrikaGoals_campaignWithCounters() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val campaign1 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId, notAllowedByMetrikaCounterId)
        }
        val cid1 = steps.textCampaignSteps().createCampaign(clientInfo, campaign1).id
        val campaign2 = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            metrikaCounters = listOf(unavailableCounterId, notAllowedByMetrikaCounterId)
        }
        val cid2 = steps.textCampaignSteps().createCampaign(clientInfo, campaign2).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = setOf(cid1, cid2),
            expectedGoalIdToCounterId = mapOf(
                goalId to counterId,
                unavailableGoalId to unavailableCounterId,
            ),
        )
    }

    /**
     * Получаем доступную цель, но недоступную цель, для которой включен флажок Метрики - не получаем,
     * т.к. счетчик не на кампании
     */
    @Test
    fun testGetBySpecificCampaigns_availableAndAllowedByMetrikaGoals_campaignWithoutCounters() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)

        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = setOf(cid1, cid2),
            expectedGoalIdToCounterId = mapOf(goalId to counterId),
        )
    }

    /**
     * Получаем только доступные цели со всех кампаний
     */
    @Test
    fun testGetByAllCampaigns_onlyAvailableGoals() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false)

        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = null,
            expectedGoalIdToCounterId = mapOf(goalId to counterId),
        )
    }

    /**
     * Получаем только доступные цели с переданных кампаний
     */
    @Test
    fun testGetBySpecificCampaigns_onlyAvailableGoals() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED, true)
        steps.featureSteps().addClientFeature(clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, false)
        val cid1 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cid2 = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        addCampaignGoalsToDb(setOf(cid1, cid2), setOf(goalId, unavailableGoalId, notAllowedByMetrikaGoalId))

        getAndAssertCampaignGoals(
            campaignIds = setOf(cid1, cid2),
            expectedGoalIdToCounterId = mapOf(goalId to counterId),
        )
    }

    private fun addCampaignGoalsToDb(campaignIds: Set<Long>, goalIds: Set<Long>) {
        campaignIds.forEach {
            metrikaCampaignRepository.addGoalIds(clientInfo.shard, it, goalIds)
        }
        retargetingGoalsPpcDictRepository.addMetrikaGoalsToPpcDict(
            goalIds.map {
                Goal()
                    .withId(it)
                    .withType(GoalType.GOAL)
                    .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION) as Goal
            }.toSet()
        )
    }

    private fun getAndAssertCampaignGoals(
        campaignIds: Set<Long>?,
        expectedGoalIdToCounterId: Map<Long, Long?>,
    ) {
        val actualGoalIdToCounterId = ktGraphQLTestExecutor.getCampaignGoals(clientInfo.uid, campaignIds)
            .associateBy({ it.id }, { it.counterId })
        assertThat(actualGoalIdToCounterId)
            .containsExactlyInAnyOrderEntriesOf(expectedGoalIdToCounterId.mapValues { it.value?.toInt() })
    }
}
