package ru.yandex.direct.core.entity.strategy.type.withweeklybudget

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithWeeklyBudget
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithWeeklyBudgetAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
    private lateinit var user: ClientInfo

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    override fun getShard() = user.shard

    override fun getClientId() = user.clientId!!

    override fun getOperatorUid(): Long = user.uid

    @Test
    fun shouldCreateStrategy() {
        val counter = RandomNumberUtils.nextPositiveInteger()
        val goal = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counter, goal)
        val strategy = autobudgetCrr()
            .withGoalId(goal.toLong())
            .withMetrikaCounters(listOf(counter.toLong()))
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun shouldReturnValidationError_ifValueIsNull() {
        val currencyMinPrice = CurrencyRub.getInstance().minAutobudget
        val strategy = autobudgetCrr()
            .withSum(currencyMinPrice.subtract(BigDecimal.ONE))
        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithWeeklyBudget.SUM)),
                    NumberDefects.greaterThanOrEqualTo(currencyMinPrice)
                )
            )
        )
    }
}

