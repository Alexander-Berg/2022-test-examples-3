package ru.yandex.direct.core.entity.strategy.service

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.autobudget.restart.repository.CampaignAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.repository.PackageStrategyAutobudgetRestartRepository
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.result.MassResultMatcher

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PrivatePackageStrategyAutobudgetRestartServiceTest : StrategyUpdateOperationTestBase() {

    @Autowired
    lateinit var restartService: PrivatePackageStrategyAutobudgetRestartService

    @Autowired
    lateinit var strategyRestartRepository: PackageStrategyAutobudgetRestartRepository

    @Autowired
    lateinit var campCampaignAutobudgetRestartRepository: CampaignAutobudgetRestartRepository

    lateinit var clientInfo: ClientInfo

    private var counter: Int = 0
    private var goal: Int = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        counter = RandomNumberUtils.nextPositiveInteger()
        goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `recalculate camp restarts on private package strategy`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val strategy = TestAutobudgetCrrStrategy.autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withIsPublic(false)
            .withCids(listOf(campaign.campaignId))

        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        Assertions.assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val restart = restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))
            .first()

        val strategyRestarts = strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(actualStrategy.id))
        val campRestarts =
            campCampaignAutobudgetRestartRepository.getAutobudgetRestartData(getShard(), listOf(campaign.campaignId))

        softly {
            assertThat(strategyRestarts).hasSize(1)
            assertThat(campRestarts).hasSize(1)
            assertThat(restart.restartReason).isEqualTo(campRestarts.first().times.restartReason)
            assertThat(restart.restartTime).isEqualTo(campRestarts.first().times.restartTime)
            assertThat(restart.softRestartTime).isEqualTo(campRestarts.first().times.softRestartTime)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail on public strategy`() {
        val strategy = TestAutobudgetCrrStrategy.autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withIsPublic(true)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        Assertions.assertThat(addResult).`is`(Conditions.matchedBy(MassResultMatcher.isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))
    }
}
