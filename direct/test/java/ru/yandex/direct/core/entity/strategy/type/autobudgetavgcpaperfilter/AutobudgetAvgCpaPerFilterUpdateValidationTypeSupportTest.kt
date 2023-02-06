package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpaperfilter

import java.math.BigDecimal
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpaPerFilterUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
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
        stubGoals(counterId, goalId)

        val strategy = autobudgetAvgCpaPerFilter()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withFilterAvgCpa(currency.minAutobudgetAvgCpa)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpaPerFilter::class.java)
            .process(currency.minAutobudgetAvgCpa.plus(BigDecimal.TEN), AutobudgetAvgCpaPerFilter.FILTER_AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail on invalid FILTER_AVG_CPA`() {
        val goalId = RandomNumberUtils.nextPositiveInteger()
        val counterId = RandomNumberUtils.nextPositiveInteger()
        stubGoals(counterId, goalId)

        val strategy = autobudgetAvgCpaPerFilter()
            .withMetrikaCounters(listOf(counterId.toLong()))
            .withGoalId(goalId.toLong())
            .withFilterAvgCpa(currency.minAutobudgetAvgCpa)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpaPerFilter::class.java)
            .process(currency.minAutobudgetAvgCpa.minus(BigDecimal.TEN), AutobudgetAvgCpaPerFilter.FILTER_AVG_CPA)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val matcher: Matcher<ValidationResult<Any, Defect<Any>>> =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetAvgCpaPerFilter.FILTER_AVG_CPA)),
                    NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa)
                )
            )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(matcher)
    }


    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}

