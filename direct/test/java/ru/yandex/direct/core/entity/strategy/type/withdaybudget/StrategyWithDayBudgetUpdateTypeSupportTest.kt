package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
internal class StrategyWithDayBudgetUpdateTypeSupportTest : StrategyUpdateOperationTestBase() {

    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `update day budget change count`() {
        val strategy = clientDefaultManualStrategy()
            .withDayBudget(BigDecimal(10000L))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(BigDecimal(20000L), StrategyWithDayBudget.DAY_BUDGET)

        prepareAndApplyValid(listOf(changes))

        val actualStrategy = strategyTypedRepository.getSafely(
            getShard(),
            listOf(strategy.id),
            DefaultManualStrategy::class.java
        ).firstOrNull()

        assertThat(actualStrategy?.dayBudgetDailyChangeCount).isEqualTo(1)
    }

    @Test
    fun `update show mode on day budget reset`() {
        val strategy = clientDefaultManualStrategy()
            .withDayBudget(BigDecimal(10000L))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.STRETCHED)

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(BigDecimal.ZERO, StrategyWithDayBudget.DAY_BUDGET)

        prepareAndApplyValid(listOf(changes))

        val actualStrategy = strategyTypedRepository.getSafely(
            getShard(),
            listOf(strategy.id),
            DefaultManualStrategy::class.java
        ).firstOrNull()

        assertThat(actualStrategy?.dayBudgetDailyChangeCount).isEqualTo(0)
        assertThat(actualStrategy?.dayBudgetShowMode).isEqualTo(StrategyDayBudgetShowMode.DEFAULT_)
        assertThat(actualStrategy?.dayBudget?.toLong()).isEqualTo(0L)
    }

}
