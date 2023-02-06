package ru.yandex.direct.core.entity.strategy.type.withavgcpa

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpa
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithAvgCpaUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo
    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId))

        val strategy = autobudgetAvgCpa()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withAvgCpa(currency.minAutobudgetAvgCpa)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpa::class.java)
            .process(currency.minAutobudgetAvgCpa.plus(10L.toBigDecimal()), StrategyWithAvgCpa.AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail on invalid avg_cpa`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId))

        val strategy = autobudgetAvgCpa()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withAvgCpa(currency.minAutobudgetAvgCpa)
            .withSum(null)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithAvgCpa::class.java)
            .process(currency.minAutobudgetAvgCpa.minus(10L.toBigDecimal()), StrategyWithAvgCpa.AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val marcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgCpa.AVG_CPA)),
                greaterThanOrEqualTo(currency.minAutobudgetAvgCpa)
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(marcher)
    }

    @Test
    fun `sum field update triggers AvgCpaOrAvgCpiValidator`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, listOf(goalId))

        val strategy = autobudgetAvgCpa()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withAvgCpa(currency.minAutobudget.plus(BigDecimal.TEN))
            .withIsPayForConversionEnabled(false)
            .withSum(currency.minAutobudget.plus(BigDecimal.TEN))

        val addOperation = createAddOperation(listOf(strategy))
        val addResult = addOperation.prepareAndApply()

        addResult.check(MassResultMatcher.isFullySuccessful())

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpa::class.java)
            .process(currency.minAutobudgetAvgCpa.minus(BigDecimal.TEN), AutobudgetAvgCpa.SUM)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val marcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithAvgCpa.AVG_CPA)),
                weekBudgetLessThan()
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(marcher)
    }

    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}
