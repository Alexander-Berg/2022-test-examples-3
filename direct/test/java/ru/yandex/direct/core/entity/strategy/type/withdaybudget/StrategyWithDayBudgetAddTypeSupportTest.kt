package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo

@CoreTest
@RunWith(SpringRunner::class)
internal class StrategyWithDayBudgetAddTypeSupportTest : StrategyAddOperationTestBase() {

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
    fun `add strategy with zero change count`() {
        val strategyWithDayBudget = clientDefaultManualStrategy()

        prepareAndApplyValid(listOf(strategyWithDayBudget))

        val actualStrategy = strategyTypedRepository.getSafely(
            getShard(),
            listOf(strategyWithDayBudget.id),
            DefaultManualStrategy::class.java
        ).firstOrNull()

        assertThat(actualStrategy?.dayBudgetDailyChangeCount).isEqualTo(0L)
    }
}
