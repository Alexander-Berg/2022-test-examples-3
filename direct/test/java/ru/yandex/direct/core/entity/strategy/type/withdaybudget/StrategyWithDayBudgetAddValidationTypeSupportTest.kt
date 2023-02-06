package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetOverridenByWallet
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.PathHelper
import java.math.BigDecimal

@CoreTest
@RunWith(SpringRunner::class)
internal class StrategyWithDayBudgetAddValidationTypeSupportTest : StrategyAddOperationTestBase() {
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
    fun `add valid strategy`() {
        val strategy = clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget)
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.STRETCHED)
            .withDayBudgetDailyChangeCount(1)

        updateWallet(currency.minDayBudget)

        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `fail to add strategy with not overriden show mode`() {
        updateWallet(currency.minDayBudget)

        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val strategy = clientDefaultManualStrategy()
            .withDayBudget(currency.minDayBudget.plus(BigDecimal.TEN))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)
            .withCids(listOf(campaign.campaignId))

        val vr = prepareAndApplyInvalid(listOf(strategy))

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithDayBudget.DAY_BUDGET)),
                dayBudgetOverridenByWallet()
            )
        )

        vr.check(matcher)
    }

    @Test
    fun `fail to add strategy with invalid day budget`() {
        val campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val strategy = clientDefaultManualStrategy()
            .withDayBudget(currency.maxDailyBudgetAmount.plus(BigDecimal.TEN))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.STRETCHED)
            .withDayBudgetDailyChangeCount(1)
            .withCids(listOf(campaign.campaignId))

        val vr = prepareAndApplyInvalid(listOf(strategy))

        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(StrategyWithDayBudget.DAY_BUDGET)),
                inInterval(currency.minDayBudget, currency.maxDailyBudgetAmount)
            )
        )

        vr.check(matcher)
    }
}
