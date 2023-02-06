package ru.yandex.direct.core.testing.data.campaign

import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo
import ru.yandex.direct.core.testing.steps.campaign.model0.BroadmatchFlag
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusPostModerate
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextLimitType
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object TestOldCampaigns {
    @JvmStatic
    fun manualCampaignWithoutType(clientId: ClientId?, uid: Long?): Campaign {
        val balanceInfo = TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)
        val contextSettings = TestCampaigns.defaultSearchBasedContextSettings()
        return Campaign()
            .withClientId(clientId?.asLong())
            .withWalletId(TestCampaigns.EMPTY_WALLET_ID)
            .withUid(uid)
            .withName("test campaign " + RandomStringUtils.randomNumeric(5))
            .withStatusEmpty(false)
            .withArchived(false)
            .withBalanceInfo(balanceInfo)
            .withStrategy(TestCampaigns.manualStrategy())
            .withContextSettings(contextSettings)
            .withStatusModerate(StatusModerate.YES)
            .withAgencyId(0L)
            .withOrderId(0L)
            .withTimezoneId(TestCampaigns.DEFAULT_TIMEZONE_ID)
            .withArchived(false)
            .withExcludePausedCompetingAds(false)
            .withEnableCompanyInfo(true)
            .withStatusShow(true)
            .withStatusActive(false)
            .withStatusBsSynced(StatusBsSynced.NO)
            .withLastChange(LocalDateTime.now())
            .withStatusPostModerate(StatusPostModerate.NEW)
            .withBroadmatchFlag(BroadmatchFlag.NO)
            .withStatusMetricaControl(false)
            .withStartTime(LocalDate.now())
            .withEmail("test@yandex.ru")
    }

    @JvmStatic
    fun emptyBalanceInfo(currencyCode: CurrencyCode): BalanceInfo {
        return BalanceInfo()
            .withCurrency(currencyCode)
            .withProductId(1L)
            .withCurrencyConverted(false)
            .withWalletCid(0L)
            .withSum(BigDecimal.ZERO)
            .withSumBalance(BigDecimal.ZERO)
            .withSumSpent(BigDecimal.ZERO)
            .withSumLast(BigDecimal.ZERO)
            .withSumToPay(BigDecimal.ZERO)
            .withSumUnits(0L)
            .withSumSpentUnits(0L)
            .withBalanceTid(0L)
            .withStatusNoPay(false)
            .withPaidByCertificate(false)
    }

    @JvmStatic
    fun defaultSearchBasedContextSettings(): ContextSettings {
        return ContextSettings()
            .withLimitType(ContextLimitType.MANUAL)
            .withLimit(50)
            .withPriceCoeff(80)
            .withEnableCpcHold(true)
    }
}
