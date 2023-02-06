package ru.yandex.direct.core.entity.strategy.container

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService
import ru.yandex.direct.core.entity.metrika.service.commongoals.CommonCountersService
import ru.yandex.direct.core.entity.metrika.service.strategygoals.StrategyGoalsService
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCampaignIds
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.rbac.RbacService

@CoreTest
@RunWith(SpringRunner::class)
class StrategyUpdateOperationContainerServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClient: MetrikaClient

    @Autowired
    private lateinit var rbacService: RbacService

    @Autowired
    private lateinit var clientService: ClientService

    @Autowired
    private lateinit var walletService: WalletService

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var commonCountersService: CommonCountersService

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private val campaignGoalsService: CampaignGoalsService = mock()
    private val strategyGoalsService: StrategyGoalsService = StrategyGoalsService(campaignGoalsService)

    private lateinit var service: StrategyUpdateOperationContainerService

    private lateinit var user: UserInfo

    private val strategyId = 1L

    @Before
    fun init() {
        service = StrategyUpdateOperationContainerService(
            clientService,
            campaignTypedRepository,
            strategyGoalsService,
            featureService,
            rbacService,
            walletService,
            campaignRepository,
            metrikaClient,
            commonCountersService,
            strategyTypedRepository,
            ppcPropertiesSupport
        )

        user = steps.userSteps().createDefaultUser()
    }

    @Test
    fun `campaignsMap should contain previous cids even after update cids list`() {
        val testCid = steps.textCampaignSteps().createDefaultCampaign(user.clientInfo!!).id
        val strategy = autobudgetAvgCpa().withId(strategyId).withCids(listOf(testCid))

        val modelChanges = ModelChanges(strategyId, AutobudgetAvgCpa::class.java)
            .process(emptyList(), StrategyWithCampaignIds.CIDS)

        val container =
            StrategyUpdateOperationContainer(user.shard, user.clientId, user.uid, user.uid, StrategyOperationOptions())
        service.fillContainers(container, listOf(strategy), listOf(modelChanges))

        Assertions.assertThat(container.typedCampaignsMap).hasSize(1)
        Assertions.assertThat(container.typedCampaignsMap).containsKey(strategyId)
        Assertions.assertThat(container.typedCampaignsMap[strategyId]!![0].id).isEqualTo(testCid)
    }

    @Test
    fun `campaignsMap should contain new cids and not contain duplicates`() {
        val testCid = steps.textCampaignSteps().createDefaultCampaign(user.clientInfo!!).id
        val testCidToAdd = steps.textCampaignSteps().createDefaultCampaign(user.clientInfo!!).id

        val strategy = autobudgetAvgCpa().withId(strategyId).withCids(listOf(testCid))

        val modelChanges = ModelChanges(strategyId, AutobudgetAvgCpa::class.java)
            .process(listOf(testCid, testCid, testCidToAdd), StrategyWithCampaignIds.CIDS)

        val container =
            StrategyUpdateOperationContainer(user.shard, user.clientId, user.uid, user.uid, StrategyOperationOptions())
        service.fillContainers(container, listOf(strategy), listOf(modelChanges))

        Assertions.assertThat(container.typedCampaignsMap).hasSize(1)
        Assertions.assertThat(container.typedCampaignsMap).containsKey(strategyId)
        Assertions.assertThat(container.typedCampaignsMap[strategyId]!!.map { it.id })
            .isEqualTo(listOf(testCid, testCidToAdd))
    }

    @Test
    fun `strategyGoalsMap should contain goals of the removed strategies`() {
        val testCid = steps.textCampaignSteps().createDefaultCampaign(user.clientInfo!!).id
        val strategy = autobudgetAvgCpa().withId(strategyId).withCids(listOf(testCid))

        val testCampaignGoals = setOf(
            TestFullGoals.defaultGoalByType(GoalType.GOAL),
            TestFullGoals.defaultGoalByType(GoalType.GOAL)
        )
        val goalsMap = mapOf(testCid to testCampaignGoals)
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), ArgumentMatchers.anyMap(), anyOrNull())

        val modelChanges = ModelChanges(strategyId, AutobudgetAvgCpa::class.java)
            .process(emptyList(), StrategyWithCampaignIds.CIDS)

        val container =
            StrategyUpdateOperationContainer(user.shard, user.clientId, user.uid, user.uid, StrategyOperationOptions())
        service.fillContainers(container, listOf(strategy), listOf(modelChanges))

        Assertions.assertThat(container.strategyGoalsMap).hasSize(1)
        Assertions.assertThat(container.strategyGoalsMap).containsKey(strategyId)
        Assertions.assertThat(container.strategyGoalsMap[strategyId]!!).isEqualTo(testCampaignGoals)
    }

    @Test
    fun `strategyGoalsMap should contain goals of the added strategies`() {
        val testCidToAdd = steps.textCampaignSteps().createDefaultCampaign(user.clientInfo!!).id
        val strategy = autobudgetAvgCpa().withId(strategyId)

        val modelChanges = ModelChanges(strategyId, AutobudgetAvgCpa::class.java)
            .process(listOf(testCidToAdd), StrategyWithCampaignIds.CIDS)

        val testCampaignGoals = setOf(
            TestFullGoals.defaultGoalByType(GoalType.GOAL),
            TestFullGoals.defaultGoalByType(GoalType.GOAL)
        )
        val goalsMap = mapOf(testCidToAdd to testCampaignGoals)
        doReturn(goalsMap)
            .whenever(campaignGoalsService)
            .getAvailableGoalsForCampaignId(any(), any(), ArgumentMatchers.anyMap(), anyOrNull())

        val container =
            StrategyUpdateOperationContainer(user.shard, user.clientId, user.uid, user.uid, StrategyOperationOptions())
        service.fillContainers(container, listOf(strategy), listOf(modelChanges))

        Assertions.assertThat(container.strategyGoalsMap).hasSize(1)
        Assertions.assertThat(container.strategyGoalsMap).containsKey(strategyId)
        Assertions.assertThat(container.strategyGoalsMap[strategyId]!!).isEqualTo(testCampaignGoals)
    }
}
