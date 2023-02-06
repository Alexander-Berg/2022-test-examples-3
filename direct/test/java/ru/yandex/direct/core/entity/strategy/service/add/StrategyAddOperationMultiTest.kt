package ru.yandex.direct.core.entity.strategy.service.add

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpv.autobudgetAvgCpv
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpvCustomPeriodStrategy.clientAutobudgetAvgCpvCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsStrategy.clientAutobudgetMaxImpressionsStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetRoiStrategy.autobudgetRoi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekSumStrategy.autobudget
import ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy.clientCpmDefaultStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestPeriodFixBidStrategy.clientPeriodFixBidStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.RandomNumberUtils

@CoreTest
@RunWith(SpringRunner::class)
class StrategyAddOperationMultiTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard(): Int = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `add all strategies in one operation`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategies = listOf(
            clientDefaultManualStrategy(),
            autobudgetCrr().withGoalId(goalId.toLong()),
            autobudgetAvgClick(),
            autobudgetAvgCpaPerCamp().withGoalId(goalId.toLong()),
            autobudgetAvgCpaPerFilter().withGoalId(goalId.toLong()),
            autobudgetAvgCpa().withGoalId(goalId.toLong()),
            autobudgetAvgCpcPerFilter(),
            autobudgetAvgCpi(),
            autobudgetAvgCpv(),
            clientAutobudgetAvgCpvCustomPeriodStrategy(),
            clientAutobudgetMaxImpressionsCustomPeriodStrategy(),
            clientAutobudgetMaxImpressionsStrategy(),
            clientAutobudgetMaxReachCustomPeriodStrategy(),
            clientAutobudgetReachStrategy(),
            autobudgetRoi().withGoalId(goalId.toLong()),
            autobudgetWeekBundle(),
            autobudget().withGoalId(null),
            clientCpmDefaultStrategy(),
            clientPeriodFixBidStrategy()
        ).map {
            withMetrikaCounters(it, counterId.toLong())
            it
        }

        prepareAndApplyValid(strategies)
    }

    private fun withMetrikaCounters(
        strategy: BaseStrategy,
        metrikaCounter: Long
    ) {
        if (strategy is StrategyWithMetrikaCounters) {
            strategy.withMetrikaCounters(listOf(metrikaCounter))
        }
    }

}
