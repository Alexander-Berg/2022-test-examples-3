package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.DailyBudget
import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum
import com.yandex.direct.api.v5.general.ArrayOfString
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.CampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class UpdateCampaignsDelegateTest : UpdateCampaignsDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        steps.featureSteps().setCurrentClient(clientInfo.clientId)
        campaignInfo = steps.textCampaignSteps()
            .createDefaultCampaign(clientInfo)
        steps.textCampaignSteps().createDefaultCampaign()
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New Campaign"
        val expectedStartDate = LocalDate.now().plusDays(10)
        val expectedEndDate = expectedStartDate.plusDays(20)
        val expectedNegativeKeywords = listOf("negative", "keyword")

        val requestedDailyBudgetAmount: Long = 300888888
        val expectedDayBudget = BigDecimal.valueOf(300.89)

        val requestedExcludedSites = listOf(
            SSP_PLATFORM_1.uppercase(),
            SSP_PLATFORM_2,
            "google.ru 1",
            "google.com"
        )
        val expectedDisabledSsp = listOf(SSP_PLATFORM_1, SSP_PLATFORM_2)
        val expectedDisabledDomains = listOf(SSP_PLATFORM_2, "google.ru", "google.com")

        val updateItem = CampaignUpdateItem().apply {
            clientInfo = null
            notification = null
            timeZone = AMSTERDAM_TIMEZONE.timezone.id
            id = campaignInfo.campaignId
            name = expectedName
            startDate = expectedStartDate.format(DATETIME_FORMATTER)
            dailyBudget = DailyBudget().run {
                amount = requestedDailyBudgetAmount
                mode = DailyBudgetModeEnum.STANDARD
                OBJECT_FACTORY.createCampaignUpdateItemDailyBudget(this)
            }
            negativeKeywords = ArrayOfString().run {
                items = expectedNegativeKeywords
                OBJECT_FACTORY.createCampaignUpdateItemNegativeKeywords(this)
            }
            // не можем протестировать blockedIps, т.к. в network-config.allow-all.json все ip объявляются внутренними,
            // а ядро не разрешает блокировать внутренние ip
            blockedIps = null
            excludedSites = ArrayOfString().run {
                items = requestedExcludedSites
                OBJECT_FACTORY.createCampaignUpdateItemExcludedSites(this)
            }
            endDate = OBJECT_FACTORY.createCampaignUpdateItemEndDate(expectedEndDate.format(DATETIME_FORMATTER))
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = TestTextCampaigns.fullTextCampaign().apply {
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            endDate = expectedEndDate
            id = campaignInfo.campaignId
            orderId = campaignInfo.orderId
            minusKeywords = expectedNegativeKeywords
            name = expectedName
            startDate = expectedStartDate
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            enableSendAccountNews = EXPECTED_SEND_NEWS
            smsTime = EXPECTED_SMS_INTERVAL
            smsFlags = EXPECTED_SMS_EVENTS
            dayBudgetDailyChangeCount = 1
            metrikaCounters = null
            statusBsSynced = CampaignStatusBsSynced.NO
            contextLimit = 0 // см CampaignWithNetworkSettingsAddOperationSupport.onPreValidated
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .withComparatorForType(BigDecimal::compareTo, BigDecimal::class.java)
            .ignoringFields(
                TextCampaign.AUTOBUDGET_FORECAST_DATE.name(),
                TextCampaign.LAST_CHANGE.name(),
                TextCampaign.DAY_BUDGET_LAST_CHANGE.name(),
                TextCampaign.STRATEGY_ID.name()
            )
            .ignoringCollectionOrderInFields(
                TextCampaign.DISABLED_DOMAINS.name(),
                TextCampaign.DISABLED_SSP.name(),
                TextCampaign.MINUS_KEYWORDS.name()
            )
            .ignoringExpectedNullFields()
            .ignoringFieldsMatchingRegexes(".*originalTimeTarget")
            .isEqualTo(expectedCampaign)
    }
}
