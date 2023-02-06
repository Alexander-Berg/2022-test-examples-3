package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.container.StrategyRepositoryContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpvCustomPeriodStrategy.clientAutobudgetAvgCpvCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpvCustomPeriodUpdateTypeSupportTest : StrategyUpdateOperationTestBase() {
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
    fun `increase budget change count on budget update`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withBudget(BigDecimal.valueOf(10000L))
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusWeeks(1))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(BigDecimal.valueOf(20000L), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(2)
    }

    @Test
    fun `increase budget change count on finish update`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusWeeks(1))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(LocalDate.now().plusDays(3), StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(2)
    }

    // Счетчик и время обновления не изменяются при изменении только поля  AutoProlongation
    @Test
    fun `not increase budget change count on AutoProlongation update`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())
            .withAutoProlongation(true)
            .withFinish(LocalDate.now().plusWeeks(1))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val actualBeforeUpdate = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(false, AutobudgetAvgCpvCustomPeriod.AUTO_PROLONGATION)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(1)
        Assertions.assertThat(updatedStrategy.lastUpdateTime).isEqualTo(actualBeforeUpdate.lastUpdateTime)
    }

    // Ожидаем сброса счетчика изменений при обновлении поля START
    @Test
    fun `reset daily change counter on start update`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(LocalDate.now().plusDays(1), AutobudgetAvgCpvCustomPeriod.START)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(1L)
    }

    @Test
    fun `reset daily change counter on budget update if last changes was not today`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())
            .withBudget(BigDecimal.valueOf(10000L))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val container = StrategyRepositoryContainer(getShard(), getClientId(), emptyMap(), false)

        val prepareChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(strategy.lastUpdateTime.minusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.LAST_UPDATE_TIME)
            .applyTo(strategy)

        strategyModifyRepository.update(
            ppcDslContextProvider.ppc(getShard()),
            container,
            listOf(prepareChanges)
        )

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(BigDecimal.valueOf(20000L), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(1L)
    }

    @Test
    fun `reset daily change counter on finish update if last changes was not today`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusWeeks(1))

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val container = StrategyRepositoryContainer(getShard(), getClientId(), emptyMap(), false)

        val prepareChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(strategy.lastUpdateTime.minusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.LAST_UPDATE_TIME)
            .applyTo(strategy)

        strategyModifyRepository.update(
            ppcDslContextProvider.ppc(getShard()),
            container,
            listOf(prepareChanges)
        )

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(LocalDate.now().plusDays(3), StrategyWithCustomPeriodBudgetAndCustomBid.FINISH)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(1L)
    }

    @Test
    fun `not update change count if only start update`() {
        val strategy = clientAutobudgetAvgCpvCustomPeriodStrategy()
            .withStart(LocalDate.now())

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val changes = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(strategy.start.plusDays(1), StrategyWithCustomPeriodBudgetAndCustomBid.START)

        prepareAndApplyValid(listOf(changes))

        val updatedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            AutobudgetAvgCpvCustomPeriod::class.java
        )[strategy.id]!!

        Assertions.assertThat(updatedStrategy.dailyChangeCount).isEqualTo(1L)
    }

}
