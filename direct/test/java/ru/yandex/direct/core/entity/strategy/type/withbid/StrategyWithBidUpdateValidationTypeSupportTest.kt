package ru.yandex.direct.core.entity.strategy.type.withbid

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.strategy.model.StrategyWithBid
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetWeekBundleStrategy.autobudgetWeekBundle
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
class StrategyWithBidUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    private lateinit var user: ClientInfo

    @Before
    fun setUp() {
        user = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(user.clientId, user.uid)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = autobudgetWeekBundle()
            .withBid(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithBid::class.java)
            .process(BigDecimal.valueOf(2), StrategyWithBid.BID)

        val result = prepareAndApplyValid(listOf(modelChanges))
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    @Test
    fun `fail on invalid BID`() {
        val currencyMaxPrice = CurrencyRub.getInstance().maxAutobudgetBid
        val strategy = autobudgetWeekBundle()
            .withBid(BigDecimal.ONE)

        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithBid::class.java)
            .process(currencyMaxPrice.plus(BigDecimal.ONE), StrategyWithBid.BID)

        val result = prepareAndApplyInvalid(listOf(modelChanges))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithBid.BID)),
                    NumberDefects.lessThanOrEqualTo(currencyMaxPrice)
                )
            )
        )
    }

    override fun getShard() = user.shard

    override fun getClientId(): ClientId = user.clientId!!

    override fun getOperatorUid(): Long = user.uid

}

