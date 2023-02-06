package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpi

import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.strategy.AutobudgetAvgCpiInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal

@CoreTest
@RunWith(SpringRunner::class)
class AutobudgetAvgCpiUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo
    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetAvgCpi()
            .withAvgCpi(currency.minAutobudgetAvgCpa)

        steps.autobudgetAvgCpiSteps().createStrategy(AutobudgetAvgCpiInfo(user.clientInfo!!, strategy))

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(currency.minAutobudgetAvgCpa.plus(BigDecimal.TEN), AutobudgetAvgCpi.AVG_CPI)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())
    }

    @Test
    fun `fail on invalid AVG_CPI`() {
        val strategy = autobudgetAvgCpi()
            .withAvgCpi(currency.minAutobudgetAvgCpa)

        steps.autobudgetAvgCpiSteps().createStrategy(AutobudgetAvgCpiInfo(user.clientInfo!!, strategy))

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(currency.minAutobudgetAvgCpa.minus(BigDecimal.TEN), AutobudgetAvgCpi.AVG_CPI)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val matcher: Matcher<ValidationResult<Any, Defect<Any>>> =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetAvgCpi.AVG_CPI)),
                    NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa)
                )
            )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(matcher)
    }

    @Test
    fun `pay for conversion field update triggers AvgCpaOrAvgCpiValidator`() {

        val strategy = autobudgetAvgCpi()
            .withAvgCpi(currency.autobudgetPayForConversionAvgCpaWarning.plus(BigDecimal.TEN))
            .withIsPayForConversionEnabled(false)
            .withSum(null)

        val addOperation = createAddOperation(listOf(strategy))
        val addResult = addOperation.prepareAndApply()

        addResult.check(MassResultMatcher.isFullySuccessful())

        val modelChanges = ModelChanges(strategy.id, AutobudgetAvgCpi::class.java)
            .process(true, AutobudgetAvgCpi.IS_PAY_FOR_CONVERSION_ENABLED)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        val marcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(AutobudgetAvgCpi.AVG_CPI)),
                NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning)
            )
        )
        (result.validationResult as ValidationResult<Any, Defect<Any>>).check(marcher)
    }

    override fun getShard(): Int = user.shard

    override fun getClientId(): ClientId = user.clientId

    override fun getOperatorUid(): Long = user.uid
}


