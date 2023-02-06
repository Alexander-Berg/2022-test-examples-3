package ru.yandex.direct.core.testing.data.campaign

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.EnumSet
import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.common.util.RepositoryUtils
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCurrencyConverted
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.SmsFlag
import ru.yandex.direct.core.entity.time.model.TimeInterval
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestCampaigns {
    const val DEFAULT_CONTEXT_PRICE_COEF = 2

    const val DEFAULT_TIMEZONE_ID = 130L // Moscow timezone
    const val EMPTY_WALLET_ID = 0L
    const val PRODUCT_ID = 1L

    fun <C : CommonCampaign> fillCommonClientFieldsForActiveCampaign(campaign: C) {
        campaign.name = "test campaign " + RandomStringUtils.randomNumeric(5)
        campaign.startDate = LocalDate.now()
        campaign.email = "test@yandex.ru"
    }

    fun fillCommonClientFields(campaign: CommonCampaign) {
        campaign.startDate = LocalDate.now()
        campaign.endDate = LocalDate.now()
        campaign.email = "1@1.ru"

        campaign.warningBalance = 49
        campaign.enableSendAccountNews = false
        campaign.timeZoneId = TestCampaigns.DEFAULT_TIMEZONE_ID
        campaign.smsFlags = EnumSet.of(SmsFlag.MODERATE_RESULT_SMS, SmsFlag.CAMP_FINISHED_SMS)
        campaign.smsTime = TimeInterval().withEndHour(1).withEndMinute(15)
            .withStartHour(1).withStartMinute(30)
        campaign.isSkadNetworkEnabled = false
        campaign.isRecommendationsManagementEnabled = false
        campaign.isPriceRecommendationsManagementEnabled = false
    }

    fun fillCommonSystemFieldsForActiveCampaign(campaign: CommonCampaign) {
        if (campaign.fio == null) {
            campaign.fio = "FIO"
        }
        campaign.statusEmpty = false
        campaign.statusArchived = false
        campaign.statusModerate = CampaignStatusModerate.YES
        campaign.statusPostModerate = CampaignStatusPostmoderate.ACCEPTED
        campaign.statusShow = true
        campaign.statusActive = true
        campaign.statusBsSynced = CampaignStatusBsSynced.YES
        campaign.autobudgetForecastDate = LocalDateTime.now().minusDays(1)
        campaign.timeZoneId = DEFAULT_TIMEZONE_ID
        campaign.lastChange = RepositoryUtils.NOW_PLACEHOLDER

        campaign.currency = CurrencyCode.RUB
        campaign.orderId = RandomNumberUtils.nextPositiveLong()
        campaign.walletId = EMPTY_WALLET_ID
        campaign.agencyId = 0L
        campaign.sum = BigDecimal.ZERO
        campaign.sumSpent = BigDecimal.ZERO
        campaign.sumLast = BigDecimal.ZERO
        campaign.sumToPay = BigDecimal.ZERO
        campaign.paidByCertificate = false
        campaign.isServiceRequested = false
        campaign.isSkadNetworkEnabled = false

        if (campaign is CampaignWithPackageStrategy) {
            campaign.strategyId = 0
        }

        if (campaign is CampaignWithCurrencyConverted) {
            campaign.currencyConverted = false
        }
    }

    fun fillCommonSystemFieldsForDraftCampaign(campaign: CommonCampaign) {
        campaign
            .withStatusActive(false)
            .withStatusShow(false)
            .withStatusModerate(CampaignStatusModerate.NEW)
            .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
            .withStatusBsSynced(CampaignStatusBsSynced.YES)
    }
}
