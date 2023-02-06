package ru.yandex.direct.core.entity.strategy.type.withconversion

import java.time.LocalDateTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy.autobudget
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.test.utils.checkNull

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithConversionAddOperationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `set lastBidderRestartTime to now on conversion strategy addition`() {
        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
        val now = LocalDateTime.now()
        strategy.lastBidderRestartTime.checkNull()
        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy =
            strategyTypedRepository.getIdToModelSafely(getShard(), listOf(id), AutobudgetCrr::class.java)[id]!!
        actualStrategy.lastBidderRestartTime.checkNotNull()
        (actualStrategy.lastBidderRestartTime >= now.minusSeconds(1)).checkEquals(true)
    }

    @Test
    fun `do not set lastBidderRestartTime if goal is null`() {
        val strategy = autobudget().withGoalId(null)
        strategy.lastBidderRestartTime.checkNull()
        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy =
            strategyTypedRepository.getIdToModelSafely(getShard(), listOf(id), AutobudgetWeekSum::class.java)[id]!!
        actualStrategy.lastBidderRestartTime.checkNull()
    }

    @Test
    fun `set lastBidderRestartTime to now on conversion avg_cpi addition even if goal is null`() {
        val strategy = autobudgetAvgCpi().withGoalId(null)
        val now = LocalDateTime.now()
        strategy.lastBidderRestartTime.checkNull()
        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy =
            strategyTypedRepository.getIdToModelSafely(getShard(), listOf(id), AutobudgetAvgCpi::class.java)[id]!!
        actualStrategy.lastBidderRestartTime.checkNotNull()
        (actualStrategy.lastBidderRestartTime >= now.minusSeconds(1)).checkEquals(true)
    }
}
