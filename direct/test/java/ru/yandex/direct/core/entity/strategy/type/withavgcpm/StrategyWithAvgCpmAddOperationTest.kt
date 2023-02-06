package ru.yandex.direct.core.entity.strategy.type.withavgcpm

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpm
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsStrategy.clientAutobudgetMaxImpressionsStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgCpmAddOperationTest : StrategyAddOperationTestBase() {
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
    fun shouldCreateStrategy() {
        val strategy = clientAutobudgetMaxImpressionsStrategy()
        strategy.avgCpm = BigDecimal.ONE
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun shouldReturnValidationError_ifAvgCpmIsNull() {
        val strategy = clientAutobudgetMaxImpressionsStrategy()
        strategy.avgCpm = null
        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgCpm.AVG_CPM)),
                    CommonDefects.notNull()
                )
            )
        )
    }
}
