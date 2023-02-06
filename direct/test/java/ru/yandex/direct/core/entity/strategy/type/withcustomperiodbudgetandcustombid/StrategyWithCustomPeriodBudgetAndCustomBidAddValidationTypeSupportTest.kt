package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.math.BigDecimal
import java.time.LocalDateTime.now
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithCustomPeriodBudgetAndCustomBidAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    val now = now()

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `add strategy with custom period and custom bid period and save start, finish and budget`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy()
        strategy.start = now.toLocalDate()
        strategy.finish = now.plusDays(7).toLocalDate()
        strategy.budget = BigDecimal(2400)
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `return validation error, when add strategy with custom period and custom bid period with null budget`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy()
        strategy.start = now.toLocalDate()
        strategy.finish = now.plusDays(7).toLocalDate()
        strategy.budget = null
        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
                    ),
                    CommonDefects.notNull()
                )
            )
        )
    }

}
