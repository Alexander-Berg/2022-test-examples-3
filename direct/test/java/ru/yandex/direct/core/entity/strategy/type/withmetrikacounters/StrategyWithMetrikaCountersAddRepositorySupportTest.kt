package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounterSource
import ru.yandex.direct.core.entity.metrikacounter.model.StrategyMetrikaCounter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMetrikaCountersAddRepositorySupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    @Autowired
    private lateinit var strategyMetrikaCountersRepository: StrategyMetrikaCountersRepository


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

        assertThat(actualStrategy.metrikaCounters).isEmpty()
    }

    @Test
    fun `add strategy with 3 metrika counters -- ecom, users, system`() {
        val counter1 = RandomNumberUtils.nextPositiveInteger()
        val counter2 = counter1 + 1
        val counter3 = counter2 + 1
        metrikaClient.addUserCounters(
            clientInfo.uid, listOf(
                CounterInfoDirect()
                    .withId(counter1)
                    .withEcommerce(true),
                CounterInfoDirect()
                    .withId(counter2)
                    .withCounterSource("sprav")
                    .withEcommerce(false),
                CounterInfoDirect()
                    .withId(counter3)
                    .withEcommerce(false),
            )
        )

        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf(counter1.toLong(), counter2.toLong(), counter3.toLong()))

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!
        assertThat(actualStrategy.metrikaCounters)
            .containsExactlyInAnyOrder(counter1.toLong(), counter2.toLong(), counter3.toLong())

        val actualMetrikaCounters = strategyMetrikaCountersRepository.getMetrikaCounters(getShard(), listOf(id)).get(id)
        val expectedMetrikaCounters = listOf(
            StrategyMetrikaCounter()
                .withId(counter1.toLong())
                .withStrategyId(id)
                .withHasEcommerce(true)
                .withSource(MetrikaCounterSource.UNKNOWN)
                .withIsDeleted(false),
            StrategyMetrikaCounter()
                .withId(counter2.toLong())
                .withStrategyId(id)
                .withHasEcommerce(false)
                .withSource(MetrikaCounterSource.SPRAV)
                .withIsDeleted(false),
            StrategyMetrikaCounter()
                .withId(counter3.toLong())
                .withStrategyId(id)
                .withHasEcommerce(false)
                .withSource(MetrikaCounterSource.UNKNOWN)
                .withIsDeleted(false)
        )
        assertThat(actualMetrikaCounters).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedMetrikaCounters)
            )
        )
    }

    @Test
    fun `add strategy with 1 unavailable metrika counter`() {
        val counter1 = RandomNumberUtils.nextPositiveInteger()

        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf(counter1.toLong()))

        prepareAndApplyValid(listOf(strategy))

        val id = strategy.id
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetMaxReachCustomPeriod::class.java
        )[id]!!
        assertThat(actualStrategy.metrikaCounters)
            .containsExactlyInAnyOrder(counter1.toLong())

        val actualMetrikaCounters = strategyMetrikaCountersRepository.getMetrikaCounters(getShard(), listOf(id)).get(id)
        val expectedMetrikaCounters = listOf(
            StrategyMetrikaCounter()
                .withId(counter1.toLong())
                .withStrategyId(id)
                .withHasEcommerce(null)
                .withSource(MetrikaCounterSource.UNKNOWN)
                .withIsDeleted(false),
        )
        assertThat(actualMetrikaCounters).containsExactlyInAnyOrderElementsOf(expectedMetrikaCounters)
    }
}
