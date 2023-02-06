package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCustomPeriodBudgetAndCustomBid
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxImpressionsCustomPeriodStrategy.clientAutobudgetMaxImpressionsCustomPeriodStrategy
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithCustomPeriodBudgetAndCustomBidUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: UserInfo

    @Before
    fun init() {
        user = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `add and update strategy successfully`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy()

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(LocalDate.now(), StrategyWithCustomPeriodBudgetAndCustomBid.START)

        val updateOperation = createUpdateOperation(listOf(modelChanges))
        val result = updateOperation.prepareAndApply()

        assertThat(
            result,
            MassResultMatcher.isFullySuccessful()
        )
    }

    @Test
    fun `fail to update with invalid BUDGET`() {
        val strategy = clientAutobudgetMaxImpressionsCustomPeriodStrategy()
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusDays(1))
            .withBudget(BigDecimal(1000))
            .withLastUpdateTime(LocalDateTime.now())


        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithCustomPeriodBudgetAndCustomBid::class.java)
            .process(BigDecimal(-1), StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(StrategyWithCustomPeriodBudgetAndCustomBid.BUDGET)
                    ),
                    MoneyDefects.invalidValueCpmNotLessThan(
                        Money.valueOf(BigDecimal.valueOf(0), CurrencyCode.RUB)
                    )
                )
            )
        )
    }

    override fun getShard() = user.shard

    override fun getClientId() = user.clientId

    override fun getOperatorUid() = user.uid
}
