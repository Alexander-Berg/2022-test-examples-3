package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcperfilter

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpcPerFilterStrategyAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
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
    fun shouldCreateNewStrategy() {
        val strategy = autobudgetAvgCpcPerFilter()
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun shouldReturnValidationError_ifFilterAvgBidIsNull() {
        val strategy = autobudgetAvgCpcPerFilter()
            .withFilterAvgBid(null)
        val validationResult = prepareAndApplyInvalid(listOf(strategy))
        assertThat(
            validationResult,
            hasDefectDefinitionWith(
                validationError(
                    path(index(0), field(AutobudgetAvgCpcPerFilter.FILTER_AVG_BID)),
                    CommonDefects.notNull()
                )
            )
        )
    }

}
