package ru.yandex.direct.core.entity.campaign.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.autobudget.restart.model.CampStrategyRestartData
import ru.yandex.direct.autobudget.restart.model.CampStrategyRestartResultSuccess
import ru.yandex.direct.autobudget.restart.repository.CampaignAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.PackageStrategyAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.RestartTimes
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.CampaignAutobudgetRestartUtils
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.result.MassResultMatcher

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignAutobudgetRestartServiceTest : StrategyUpdateOperationTestBase() {

    @Autowired
    lateinit var restartService: CampaignAutobudgetRestartService

    @Autowired
    lateinit var strategyRestartRepository: PackageStrategyAutobudgetRestartRepository

    @Autowired
    lateinit var campaignAutobudgetRestartRepository: CampaignAutobudgetRestartRepository

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var campaignModifyRepository: CampaignModifyRepository

    lateinit var clientInfo: ClientInfo

    private var counter: Int = 0
    private var goal: Int = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        steps.featureSteps().addClientFeature(
            clientInfo.clientId!!,
            FeatureName.PACKAGE_STRATEGIES_STAGE_TWO,
            true
        )
        counter = RandomNumberUtils.nextPositiveInteger()
        goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        steps.featureSteps().addClientFeature(
            clientInfo.clientId!!,
            FeatureName.AUTOBUDGET_RESTART_WITH_PACKAGE_STRATEGY_SUPPORT_ENABLED,
            true
        )
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `calculate restart for campaign without package strategy`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val restart = calculateRestarts(listOf(campaign.campaignId)).first()

        val expectedRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(getShard(), listOf(campaign.campaignId))
                .first()
        assertThat(restart.times()).isEqualTo(expectedRestart.times)
    }

    @Test
    fun `calculate restart for campaign with private package strategy`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(false)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restart = calculateRestarts(listOf(campaign.campaignId)).first()

        val strategyRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
        val expectedRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(getShard(), listOf(campaign.campaignId))
                .first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(strategyRestart).hasSize(1)
            assertThat(restart.times()).isEqualTo(expectedRestart.times)
            haveTheSameRestarts(strategy.id, campaign.campaignId)
        }
    }

    @Test
    fun `calculate restart for campaign with public package strategy`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restart = calculateRestarts(listOf(campaign.campaignId)).first()

        val campRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(getShard(), listOf(campaign.campaignId))
                .first()

        val expectedRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
                .first()
        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(restart.times()).isEqualTo(expectedRestart.times)
            assertThat(restart.times()).isEqualTo(expectedRestart.times)
            assertThat(campRestart.times).isEqualTo(expectedRestart.times)
            haveTheSameRestarts(strategy.id, campaign.campaignId)
        }
    }

    @Test
    fun `campaigns has common restarts`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId, campaign2.campaignId))
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val campRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(
                getShard(),
                listOf(campaign1.campaignId, campaign2.campaignId)
            )

        val expectedRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
                .first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(campRestart).hasSize(2)
            assertThat(restarts).hasSize(2)
            assertThat(restarts.map { it.times() }).containsOnly(expectedRestart.times)
            assertThat(campRestart.map { it.times }).containsOnly(expectedRestart.times)
            haveTheSameRestarts(strategy.id, campaign1.campaignId)
            haveTheSameRestarts(strategy.id, campaign2.campaignId)
        }
    }

    @Test
    fun `updates affects all campaigns`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId, campaign2.campaignId))
            .withSum(10000.toBigDecimal())
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val updateBudget = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(5000.toBigDecimal(), AutobudgetAvgCpa.SUM)
        prepareAndApplyValid(listOf(updateBudget))

        val restartsAfterUpdate = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val campRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(
                getShard(),
                listOf(campaign1.campaignId, campaign2.campaignId)
            )

        val expectedRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
                .first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(campRestart).hasSize(2)
            assertThat(restarts).hasSize(2)
            assertThat(restartsAfterUpdate.map { it.times() }).containsOnly(expectedRestart.times)
            restarts.forEach { restart ->
                assertThat(restart.restartTime).isBeforeOrEqualTo(expectedRestart.times.restartTime)
                assertThat(restart.softRestartTime).isBeforeOrEqualTo(expectedRestart.times.softRestartTime)
            }
            haveTheSameRestarts(strategy.id, campaign1.campaignId)
            haveTheSameRestarts(strategy.id, campaign2.campaignId)
        }
    }

    @Test
    fun `campaigns take restart from strategy`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId))
            .withSum(5000.toBigDecimal())
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val changes = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(listOf(campaign1.campaignId, campaign2.campaignId), AutobudgetAvgCpa.CIDS)
        prepareAndApplyValid(listOf(changes))

        val restartsAfterUpdate = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val expectedRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
                .first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(restarts).hasSize(2)
            assertThat(restartsAfterUpdate).hasSize(2)
            restarts.forEach { restart ->
                assertThat(restart.restartTime).isBeforeOrEqualTo(expectedRestart.times.restartTime)
                assertThat(restart.softRestartTime).isBeforeOrEqualTo(expectedRestart.times.softRestartTime)
            }
            assertThat(restartsAfterUpdate.map { it.times() }).containsOnly(expectedRestart.times)
            haveTheSameRestarts(strategy.id, campaign1.campaignId)
            haveTheSameRestarts(strategy.id, campaign2.campaignId)
        }
    }

    @Test
    fun `strategy transition to public do not triggers restart`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(false)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restart = calculateRestarts(listOf(campaign.campaignId)).first()

        val changes = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(true, AutobudgetAvgCpa.IS_PUBLIC)
        prepareAndApplyValid(listOf(changes))

        val restartAfterUpdate = calculateRestarts(listOf(campaign.campaignId)).first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(restart.times()).isEqualTo(restartAfterUpdate.times())
        }
    }

    @Test
    fun `unbinding of campaign do not trigger restart`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId, campaign2.campaignId))
            .withSum(10000.toBigDecimal())
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val actualCampaign1 = campaignTypedRepository.getSafely(
            getShard(),
            listOf(campaign1.campaignId),
            TextCampaign::class.java
        ).first()
        val changes = ModelChanges(campaign1.campaignId, TextCampaign::class.java)
            .process(0L, TextCampaign.STRATEGY_ID)
            .applyTo(actualCampaign1)

        campaignModifyRepository.updateCampaignsTable(getShard(), listOf(changes))

        val restartsAfterUpdate = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val expectedRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
                .first()

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(restarts).hasSize(2)
            assertThat(restartsAfterUpdate).hasSize(2)
            assertThat(restarts.map { it.times() }).containsOnly(expectedRestart.times)
            assertThat(restartsAfterUpdate.map { it.times() }).containsOnly(expectedRestart.times)
        }
    }

    @Test
    fun `campaign binded to another strategy`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy1 = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId))
            .withSum(10000.toBigDecimal())
            .withIsPublic(true)

        val strategy2 = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign2.campaignId))
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy1, strategy2)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))
            .associateBy { it.cid }

        val changes = ModelChanges(strategy2.id, AutobudgetCrr::class.java)
            .process(listOf(campaign1.campaignId, campaign2.campaignId), AutobudgetCrr.CIDS)

        prepareAndApplyValid(listOf(changes))

        val restartsAfterUpdate = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))
            .associateBy { it.cid }

        val actualRestart =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy1.id, strategy2.id))
                .associateBy { it.strategyId }

        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(restarts).hasSize(2)
            assertThat(restartsAfterUpdate).hasSize(2)
            assertThat(restarts[campaign1.campaignId]!!.times()).isEqualTo(actualRestart[strategy1.id]!!.times)
            assertThat(restarts[campaign2.campaignId]!!.times()).isEqualTo(actualRestart[strategy2.id]!!.times)
            assertThat(restartsAfterUpdate[campaign1.campaignId]!!.times()).isEqualTo(actualRestart[strategy2.id]!!.times)
            assertThat(restartsAfterUpdate[campaign2.campaignId]!!.times()).isEqualTo(actualRestart[strategy2.id]!!.times)
            haveTheSameRestarts(strategy2.id, campaign1.campaignId)
            haveTheSameRestarts(strategy2.id, campaign2.campaignId)
        }
    }

    @Test
    fun `strategy updates not affects campaigns if property disabled`() {
        steps.featureSteps().addClientFeature(
            clientInfo.clientId!!,
            FeatureName.AUTOBUDGET_RESTART_WITH_PACKAGE_STRATEGY_SUPPORT_ENABLED,
            false
        )
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId, campaign2.campaignId))
            .withSum(10000.toBigDecimal())
            .withIsPublic(true)

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        val restarts = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val updateBudget = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(5000.toBigDecimal(), AutobudgetAvgCpa.SUM)
        prepareAndApplyValid(listOf(updateBudget))

        val restartsAfterUpdate = calculateRestarts(listOf(campaign1.campaignId, campaign2.campaignId))

        val campRestart =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(
                getShard(),
                listOf(campaign1.campaignId, campaign2.campaignId)
            )

        val strategyRestarts =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategy.id))
        softly {
            assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))
            assertThat(campRestart).hasSize(2)
            assertThat(strategyRestarts).isEmpty()
            assertThat(restarts).hasSize(2)
            assertThat(restartsAfterUpdate.map { it.times() }).containsExactlyInAnyOrder(*(campRestart.map { it.times }
                .toTypedArray()))
        }
    }

    private fun haveTheSameRestarts(strategyId: Long, campaignId: Long) {
        val strategyRestartTimes =
            strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(strategyId)).first()
        val campaignTimes =
            campaignAutobudgetRestartRepository.getAutobudgetRestartData(getShard(), listOf(campaignId)).first()

        assertThat(campaignTimes.times).isEqualTo(strategyRestartTimes.times)
    }

    private fun calculateRestarts(cids: List<Long>): List<CampStrategyRestartResultSuccess> {
        val campaigns = campaignTypedRepository.getStrictlyFullyFilled(getShard(), cids, CommonCampaign::class.java)
        val data = campaigns.map {
            CampStrategyRestartData(
                it.id,
                CampaignAutobudgetRestartUtils.getStrategyDto(it, true, false)
            )
        }
        return restartService.calculateRestartsAndSave(data)
            .map { it.restartResult }
    }

    private fun CampStrategyRestartResultSuccess.times(): RestartTimes =
        RestartTimes(
            this.restartTime,
            this.softRestartTime,
            this.restartReason
        )
}
