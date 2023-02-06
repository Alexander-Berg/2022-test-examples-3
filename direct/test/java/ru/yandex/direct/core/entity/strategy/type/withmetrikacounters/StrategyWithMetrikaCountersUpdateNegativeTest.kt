package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMetrikaCountersUpdateNegativeTest : StrategyUpdateOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    private lateinit var smartCampaignInfo: SmartCampaignInfo

    private var id: Long = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        smartCampaignInfo = steps.smartCampaignSteps().createDefaultCampaign(clientInfo)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `update strategy with invalid metrika counters`() {
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf())

        val operation = createAddOperation(listOf(strategy), StrategyOperationOptions())
        val result = operation.prepareAndApply()
        id = result[0].result

        val mc = ModelChanges.build(
            id, StrategyWithMetrikaCounters::class.java,
            StrategyWithMetrikaCounters.METRIKA_COUNTERS, listOf(-1)
        )

        prepareAndApplyInvalid(listOf(mc))
    }

    //    @Test
    fun `update strategy with unavailable metrika counters`() {
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf())
            .withCids(listOf(id))

        val operation = createAddOperation(listOf(strategy), StrategyOperationOptions())
        val result = operation.prepareAndApply()
        id = result[0].result

        val mc = ModelChanges.build(
            id, AutobudgetMaxReachCustomPeriod::class.java,
            StrategyWithMetrikaCounters.METRIKA_COUNTERS, listOf(123)
        )

        prepareAndApplyInvalid(listOf(mc))
    }

    fun matcher(path: Path, defect: Defect<*>): Matcher<ValidationResult<Any, Defect<*>>> =
        Matchers.hasDefectDefinitionWith(
            Matchers.validationError(
                path,
                defect
            )
        )

}
