package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachCustomPeriodStrategies
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithMetrikaCountersAddNegativeTest : StrategyAddOperationTestBase() {
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo

    private lateinit var smartCampaignInfo: SmartCampaignInfo

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
    fun `add strategy with invalid metrika counters`() {
        val strategy = TestAutobudgetMaxReachCustomPeriodStrategies.clientAutobudgetMaxReachCustomPeriodStrategy()
            .withMetrikaCounters(listOf(-1))

        val result = prepareAndApplyInvalid(listOf(strategy))
        assertThat(result).`is`(
            matchedBy(
                matcher(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(StrategyWithMetrikaCounters.METRIKA_COUNTERS),
                        PathHelper.index(0)
                    ),
                    CommonDefects.validId()
                )
            )
        )
    }

    @Test
    fun `add strategy with unavailable metrika counters`() {
        val strategy = autobudgetAvgCpaPerFilter()
            .withGoalId(randomPositiveLong())
            .withCids(listOf(smartCampaignInfo.campaignId))
            .withMetrikaCounters(listOf(1001))

        val result = prepareAndApplyInvalid(listOf(strategy))
        assertThat(result).`is`(
            matchedBy(
                matcher(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(StrategyWithMetrikaCounters.METRIKA_COUNTERS),
                        PathHelper.index(0)
                    ),
                    CampaignDefects.metrikaCounterIsUnavailable()
                )
            )
        )
    }

    fun matcher(path: Path, defect: Defect<*>): Matcher<ValidationResult<Any, Defect<*>>> =
        Matchers.hasDefectDefinitionWith(
            Matchers.validationError(
                path,
                defect
            )
        )
}
