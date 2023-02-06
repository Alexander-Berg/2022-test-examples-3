package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringRunner::class)
internal class StrategyWithDayBudgetUpdateValidationTypeSupportTest : StrategyUpdateOperationTestBase() {
    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    private lateinit var clientInfo: ClientInfo

    private var walletId: Long = 0L

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    private fun updateWallet(dayBudget: BigDecimal) {
        campaignRepository.updateDailyBudget(getShard(), walletId, dayBudget)
    }

    @Test
    fun `update to valid strategy`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)

        updateWallet(currency.maxDailyBudgetAmount)

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(currency.maxDailyBudgetAmount, StrategyWithDayBudget.DAY_BUDGET)

        prepareAndApplyValid(listOf(modelChanges))
    }

    @Test
    fun `fail validation on update to strategy with invalid day budget`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(currency.maxDailyBudgetAmount.plus(BigDecimal.ONE), StrategyWithDayBudget.DAY_BUDGET)

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithDayBudget.DAY_BUDGET)),
                inInterval(currency.minDayBudget, currency.maxDailyBudgetAmount)
            )
        )

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        vr.check(matcher)
    }

    @Test
    fun `warning validation on update to strategy with invalid day budget`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)

        updateWallet(currency.minDayBudget)

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(currency.maxDailyBudgetAmount, StrategyWithDayBudget.DAY_BUDGET)

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithDayBudget.DAY_BUDGET)),
                CampaignDefects.dayBudgetOverridenByWallet()
            )
        )

        val vr = prepareAndApplyValid(listOf(modelChanges))

        vr.check(matcher)
    }

    @Test
    fun `warning validation on update to strategy with invalid day budget show mode`() {
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)

        updateWallet(currency.minDayBudget)

        val addOperation = createAddOperation(listOf(strategy))

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, StrategyWithDayBudget::class.java)
            .process(StrategyDayBudgetShowMode.STRETCHED, StrategyWithDayBudget.DAY_BUDGET_SHOW_MODE)

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithDayBudget.DAY_BUDGET_SHOW_MODE)),
                CampaignDefects.dayBudgetShowModeOverridenByWallet()
            )
        )

        val vr = prepareAndApplyValid(listOf(modelChanges))

        vr.check(matcher)
    }

//    TODO добавить тест на DAY_BUDGET_CHANGE_COUNT
//     нужно сделать после добавления сервисного уровня ручных

}
