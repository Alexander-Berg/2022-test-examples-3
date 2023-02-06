package ru.yandex.direct.core.entity.strategy.type.withlastbidderrestarttime

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.strategy.container.StrategyRepositoryContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE
import ru.yandex.direct.core.entity.strategy.model.StrategyName.AUTOBUDGET_AVG_CPI
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.entity.strategy.model.StrategyWithLastBidderRestartTime
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy.autobudget
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithLastBidderRestartTimeUpdateTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: ClientInfo

    private val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private lateinit var strategyRepositoryContainer: StrategyRepositoryContainer

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
        strategyRepositoryContainer = StrategyRepositoryContainer(getShard(), getClientId(), emptyMap(), false)
    }

    override fun getShard() = user.shard

    override fun getClientId(): ClientId = user.clientId!!

    override fun getOperatorUid(): Long = user.uid

    @Test
    fun `not modify lastBidderRestartTime after update, when updated strategy without support of learning status`() {
        val strategy = autobudget()
            .withGoalId(null)
            .withLastBidderRestartTime(now.minusDays(1))
            .withAttributionModel(LAST_YANDEX_DIRECT_CLICK)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy)
        val oldStrategyLastBidderRestartTime = strategy.lastBidderRestartTime

        val changes = ModelChanges(strategy.id, StrategyWithLastBidderRestartTime::class.java)
            .process(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE, StrategyWithLastBidderRestartTime.ATTRIBUTION_MODEL)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetWeekSum::class.java
        )[strategy.id]!!

        Assert.assertEquals(
            updatedStrategy.lastBidderRestartTime.truncatedTo(ChronoUnit.SECONDS),
            oldStrategyLastBidderRestartTime.truncatedTo(ChronoUnit.SECONDS)
        )
    }

    @Test
    fun `lastBidderRestartTime should change after update, when updated strategy with learning status and goalId not null and strategy restarting`() {
        val strategy = autobudgetAvgCpa()
            .withGoalId(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withAttributionModel(LAST_YANDEX_DIRECT_CLICK)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy)

        val changes = ModelChanges(strategy.id, StrategyWithLastBidderRestartTime::class.java)
            .process(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE, StrategyWithLastBidderRestartTime.ATTRIBUTION_MODEL)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpa::class.java
        )[strategy.id]!!

        Assert.assertThat(updatedStrategy.lastBidderRestartTime, LocalDateTimeMatcher.approximatelyNow())
    }

    @Test
    fun `lastBidderRestartTime should change after update, when updated strategy with learning status and attribution model changed`() {
        val strategy = autobudgetAvgCpi()
            .withAttributionModel(LAST_YANDEX_DIRECT_CLICK)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy)

        val changes = ModelChanges(strategy.id, StrategyWithLastBidderRestartTime::class.java)
            .process(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE, StrategyWithLastBidderRestartTime.ATTRIBUTION_MODEL)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpi::class.java
        )[strategy.id]!!

        Assert.assertThat(updatedStrategy.lastBidderRestartTime, LocalDateTimeMatcher.approximatelyNow())
    }

    @Test
    fun `lastBidderRestartTime should change after update, when updated strategy with learning status and goalId not null and goal id changed`() {
        val goalId1 = RandomNumberUtils.nextPositiveInteger()
        val goalId2 = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId1, goalId2))

        val strategy = autobudgetCrr()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId1.toLong())

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy)

        val changes = ModelChanges(strategy.id, StrategyWithLastBidderRestartTime::class.java)
            .process(goalId2.toLong(), StrategyWithConversion.GOAL_ID)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetCrr::class.java
        )[strategy.id]!!

        Assert.assertThat(updatedStrategy.lastBidderRestartTime, LocalDateTimeMatcher.approximatelyNow())
    }

    @Test
    fun `lastBidderRestartTime should change after update, when updated strategy with learning status and old strategy without support of learning status`() {
        val strategy = autobudget()
            .withGoalId(null)
            .withLastBidderRestartTime(now.minusDays(1))

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy)

        val changes = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(AUTOBUDGET_AVG_CPI, CommonStrategy.TYPE)
            .process(BigDecimal.TEN, AutobudgetAvgCpi.AVG_CPI)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpi::class.java
        )[strategy.id]!!

        Assert.assertThat(updatedStrategy.lastBidderRestartTime, LocalDateTimeMatcher.approximatelyNow())
    }

    private fun updateLastBidderRestartTimeToOneBeforeNowStrictly(strategy: StrategyWithLastBidderRestartTime) {
        val prepareChanges = ModelChanges(strategy.id, StrategyWithLastBidderRestartTime::class.java)
            .process(
                strategy.lastBidderRestartTime.minusMinutes(3),
                StrategyWithLastBidderRestartTime.LAST_BIDDER_RESTART_TIME
            )
            .applyTo(strategy)

        strategyModifyRepository.update(
            ppcDslContextProvider.ppc(getShard()),
            strategyRepositoryContainer,
            listOf(prepareChanges)
        )
    }
}
