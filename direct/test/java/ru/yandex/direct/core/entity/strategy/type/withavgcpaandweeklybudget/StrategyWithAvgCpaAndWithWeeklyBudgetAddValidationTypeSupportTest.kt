package ru.yandex.direct.core.entity.strategy.type.withavgcpaandweeklybudget

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpaAndWeeklyBudget
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.index

@CoreTest
@RunWith(SpringRunner::class)
internal class StrategyWithAvgCpaAndWeeklyBudgetAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var clientInfo: ClientInfo

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `add valid strategy`() {
        val strategy = autobudgetAvgCpa()
            .withAvgCpa(currency.autobudgetAvgCpaWarning)
            .withSum(currency.autobudgetAvgCpaWarning)

        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `fail to add invalid strategy`() {
        val strategy = autobudgetAvgCpa()
            .withAvgCpa(currency.minAutobudgetAvgCpa.plus(BigDecimal.TEN))
            .withSum(currency.minAutobudgetAvgCpa)

        val result = prepareAndApplyInvalid(listOf(strategy))
        val expectedDefect = weekBudgetLessThan()
        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(index(0), PathHelper.field(StrategyWithAvgCpaAndWeeklyBudget.AVG_CPA)),
                expectedDefect
            )
        )

        result.check(matcher)
    }
}
