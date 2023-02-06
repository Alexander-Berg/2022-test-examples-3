package ru.yandex.direct.core.entity.strategy.type.withavgcpaandweeklybudget

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpaAndWeeklyBudget
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgCpaAndWeeklyBudgetUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {

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
    fun `update to valid strategy`() {
        val strategy = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
            .withAvgCpa(currency.autobudgetAvgCpaWarning)
            .withSum(currency.autobudgetAvgCpaWarning)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpaAndWeeklyBudget::class.java)
            .process(currency.autobudgetAvgCpaWarning.minus(BigDecimal.TEN), StrategyWithAvgCpaAndWeeklyBudget.AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail to add invalid strategy`() {
        val strategy = TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa()
            .withAvgCpa(currency.minAutobudget.plus(BigDecimal.TEN))
            .withSum(currency.minAutobudget.plus(BigDecimal.TEN))
            .withBid(null)
            .withIsPayForConversionEnabled(false)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpaAndWeeklyBudget::class.java)
            .process(currency.minAutobudget, StrategyWithAvgCpaAndWeeklyBudget.SUM)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply()

        val expectedDefect = weekBudgetLessThan()
        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgCpaAndWeeklyBudget.AVG_CPA)),
                expectedDefect
            )
        )

        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(matcher)
    }
}
