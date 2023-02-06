package ru.yandex.direct.core.entity.strategy.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.autobudget.restart.repository.PackageStrategyAutobudgetRestartRepository
import ru.yandex.direct.autobudget.restart.service.Reason
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PublicPackageStrategyAutobudgetRestartServiceTest : StrategyUpdateOperationTestBase() {

    @Autowired
    lateinit var restartService: PublicPackageStrategyAutobudgetRestartService

    @Autowired
    lateinit var strategyRestartRepository: PackageStrategyAutobudgetRestartRepository

    lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    // Слуйчай когда стратегия стала публичной и у нее нет записи о рестартах, поэтому наследуем рестарт у кампании.
    @Test
    fun `restart for public strategy with one campaign`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(true)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()
        assertThat(addResult).`is`(matchedBy(isFullySuccessful<Long>()))

        val updatedStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val restarts = restartService.recalculateAndSaveRestarts(getShard(), listOf(updatedStrategy))
        val savedRestart = strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(updatedStrategy.id))
        softly {
            assertThat(restarts).hasSize(1)
            assertThat(savedRestart).hasSize(1)
            val expectedRestartTime = savedRestart.first().times.restartTime
            assertThat(restarts.first().restartReason).isEqualTo(Reason.INIT.toString())
            assertThat(restarts.first().restartTime).isEqualTo(expectedRestartTime)
        }
    }

    @Test
    fun `restart for public strategy with two campaign`() {
        val campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign1.campaignId, campaign2.campaignId))
            .withIsPublic(true)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        assertThat(addResult).`is`(matchedBy(isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val restarts = restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))
        val savedRestart = strategyRestartRepository.getAutobudgetRestartData(getShard(), listOf(actualStrategy.id))
        val expectedRestartTime = savedRestart.first().times.restartTime

        softly {
            assertThat(restarts).hasSize(1)
            assertThat(savedRestart).hasSize(1)
            assertThat(restarts.first().restartReason).isEqualTo(Reason.INIT.toString())
            assertThat(restarts.first().restartTime).isEqualTo(expectedRestartTime)
        }
    }

    @Test
    fun `recalculate restarts`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withCids(listOf(campaign.campaignId))
            .withSum(BigDecimal.valueOf(5000))
            .withIsPublic(true)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        assertThat(addResult).`is`(matchedBy(isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val restarts = restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))

        val changes = ModelChanges(strategy.id, AutobudgetCrr::class.java)
            .process(BigDecimal.valueOf(10000), AutobudgetCrr.SUM)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val recalculated = restartService.recalculateAndSaveRestarts(getShard(), listOf(updatedStrategy))

        softly {
            assertThat(restarts).hasSize(1)
            assertThat(recalculated).hasSize(1)
            assertThat(restarts.first().restartReason).isEqualTo(Reason.INIT.toString())
            assertThat(recalculated.first().restartReason).isEqualTo(Reason.CHANGED_AUTOBUDGET_SUM.toString())
            assertThat(restarts.first().restartTime).isBeforeOrEqualTo(recalculated.first().restartTime)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fail on private strategy`() {

        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withIsPublic(false)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        assertThat(addResult).`is`(matchedBy(isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))
    }

    @Test
    fun `initial strategy without campaigns`() {
        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
            .withIsPublic(true)
        val addResult = createAddOperation(listOf(strategy)).prepareAndApply()

        assertThat(addResult).`is`(matchedBy(isFullySuccessful<Long>()))

        val actualStrategy = strategyTypedRepository.getTyped(getShard(), listOf(strategy.id))
            .mapNotNull { it as? CommonStrategy }
            .first()

        val restart = restartService.recalculateAndSaveRestarts(getShard(), listOf(actualStrategy))
            .first()
        assertThat(restart.restartReason).isEqualTo(Reason.INIT.toString())
    }
}
