package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.RandomNumberUtils

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMetrikaCountersAddPositiveTest : StrategyAddOperationTestBase() {
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Autowired
    private lateinit var strategyMetrikaCountersRepository: StrategyMetrikaCountersRepository

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
    fun `add strategy with empty metrika counters`() {
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf())

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!

        Assertions.assertThat(actualStrategy.metrikaCounters).isEmpty()
    }

    @Test
    fun `add strategy with one metrika counter`() {
        val metrikaCounterIds: List<Long> = listOf(1)
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(metrikaCounterIds)

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!

        Assertions.assertThat(actualStrategy.metrikaCounters).isEqualTo(metrikaCounterIds)
    }

    @Test
    fun `add strategy with one non-system metrika counter and one system for perf campaign`() {
        val nonSystemCounter = RandomNumberUtils.nextPositiveInteger().toLong()
        val systemCounter = RandomNumberUtils.nextPositiveInteger().toLong()
        val metrikaCounterIds: List<Long> = listOf(nonSystemCounter, systemCounter)
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(metrikaCounterIds)

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!

        Assertions.assertThat(actualStrategy.metrikaCounters).containsExactlyInAnyOrderElementsOf(metrikaCounterIds)
    }

    @Test
    fun `add strategy with available non-system metrika counters and system`() {
        val smartCampaignInfo = steps.smartCampaignSteps().createDefaultCampaign(clientInfo)
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = 1L
        val systemCounterId = 2L
        metrikaClient.addUserCounter(clientInfo.uid, counterId.toInt())
        metrikaClient.addUserCounter(
            clientInfo.uid, CounterInfoDirect()
                .withId(systemCounterId.toInt())
                .withCounterSource("sprav")
                .withName("name")
                .withEcommerce(false)
                .withSitePath("ya.ru")
        )
        metrikaClient.addCounterGoal(counterId.toInt(), goalId)
        val strategy = TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter()
            .withGoalId(goalId.toLong())
            .withCids(listOf(smartCampaignInfo.campaignId))
            .withMetrikaCounters(listOf(counterId))

        prepareAndApplyValid(listOf(strategy))
    }
}
