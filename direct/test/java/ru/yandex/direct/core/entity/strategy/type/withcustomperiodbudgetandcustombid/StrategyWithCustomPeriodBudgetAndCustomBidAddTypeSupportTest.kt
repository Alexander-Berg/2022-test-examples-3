package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithCustomPeriodBudgetAndCustomBidAddTypeSupportTest : StrategyAddOperationTestBase() {
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
    fun `add strategy with with custom period and custom bid period and save lastUpdateTime and dailyChangeCount correctly`() {
        val strategy = clientAutobudgetMaxReachCustomPeriodStrategy()

        val now = now()
        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!
        assertThat(actualStrategy.dailyChangeCount).isEqualTo(1)
        assertThat(actualStrategy.lastUpdateTime).isAfterOrEqualTo(now.minusSeconds(1))
    }

}
