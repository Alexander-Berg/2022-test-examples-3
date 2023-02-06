package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.yandex.direct.api.v5.campaignsext.CampaignAssistant
import com.yandex.direct.api.v5.campaignsext.CampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.CampaignFundsEnum
import com.yandex.direct.api.v5.campaignsext.CampaignFundsParam
import com.yandex.direct.api.v5.campaignsext.CampaignGetItem
import com.yandex.direct.api.v5.campaignsext.CampaignStateGetEnum
import com.yandex.direct.api.v5.campaignsext.CampaignStatusPaymentEnum
import com.yandex.direct.api.v5.campaignsext.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaignsext.DailyBudget
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaignsext.EmailSettings
import com.yandex.direct.api.v5.campaignsext.FundsParam
import com.yandex.direct.api.v5.campaignsext.GetResponse
import com.yandex.direct.api.v5.campaignsext.Notification
import com.yandex.direct.api.v5.campaignsext.SmsEventsEnum
import com.yandex.direct.api.v5.campaignsext.SmsSettings
import com.yandex.direct.api.v5.campaignsext.TimeTargeting
import com.yandex.direct.api.v5.campaignsext.TimeTargetingOnPublicHolidays
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.CurrencyEnum
import com.yandex.direct.api.v5.general.Statistics
import com.yandex.direct.api.v5.general.StatusEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.api.v5.common.toArrayOfString
import ru.yandex.direct.api.v5.common.toYesNoEnum
import ru.yandex.direct.api.v5.entity.campaigns.StatusClarificationTranslations
import ru.yandex.direct.api.v5.entity.campaigns.converter.toApiDate
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.TranslationService
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.SmsFlag
import ru.yandex.direct.core.entity.time.model.TimeInterval
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo
import ru.yandex.direct.i18n.types.ConcatTranslatable
import ru.yandex.direct.libs.timetarget.TimeTarget
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong
import java.time.LocalDate
import java.util.EnumSet

@Api5Test
@RunWith(SpringJUnit4ClassRunner::class)
class GetCampaignsExtDelegateTest : GetCampaignsExtDelegateBaseTest() {

    @Autowired
    private lateinit var translationService: TranslationService

    @Test
    fun `get all campaigns common fields`() {
        val campaignInfo = createContentPromotionCampaign {
            checkPositionIntervalEvent = CampaignWarnPlaceInterval._15
            clicks = randomPositiveInt().toLong()
            dayBudget = "500.00".toBigDecimal()
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledSsp = listOf(SSP_PLATFORM_1)
            disabledDomains = listOf(SSP_PLATFORM_1, SSP_PLATFORM_2)
            endDate = LocalDate.now().plusYears(1)
            minusKeywords = listOf("слово")
            name = "Test ContentPromotionCampaign #1"
            shows = randomPositiveLong()
            smsFlags = EnumSet.of(SmsFlag.CAMP_FINISHED_SMS)
            smsTime = TimeInterval().apply {
                startHour = 1
                startMinute = 2
                endHour = 3
                endMinute = 4
            }
            strategy = TestCampaignsStrategy.defaultAutobudgetRoiStrategy(0L, false)
            sum = 300.toBigDecimal()
            sumSpent = 200.toBigDecimal()
            timeTarget = TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
        }
        val campaign = campaignInfo.typedCampaign

        val actualResponse = doGetRequest {
            selectionCriteria = CampaignsSelectionCriteria().apply {
                ids = listOf(campaign.id)
            }
            fieldNames = listOf(
                // закомментированные значения ещё не поддержаны
                CampaignFieldEnum.BLOCKED_IPS,
                CampaignFieldEnum.CLIENT_INFO,
                CampaignFieldEnum.CURRENCY,
                CampaignFieldEnum.DAILY_BUDGET,
                CampaignFieldEnum.END_DATE,
                CampaignFieldEnum.EXCLUDED_SITES,
                CampaignFieldEnum.FUNDS,
                CampaignFieldEnum.ID,
                CampaignFieldEnum.NAME,
                CampaignFieldEnum.NEGATIVE_KEYWORDS,
                CampaignFieldEnum.NOTIFICATION,
                CampaignFieldEnum.REPRESENTED_BY,
                CampaignFieldEnum.SOURCE_ID,
                CampaignFieldEnum.START_DATE,
                CampaignFieldEnum.STATE,
                CampaignFieldEnum.STATISTICS,
                CampaignFieldEnum.STATUS,
                CampaignFieldEnum.STATUS_CLARIFICATION,
                CampaignFieldEnum.STATUS_PAYMENT,
                CampaignFieldEnum.TIME_TARGETING,
                CampaignFieldEnum.TIME_ZONE,
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    // значения с null ещё не поддержаны
                    blockedIps = FACTORY.createCampaignGetItemBlockedIps(null)
                    clientInfo = campaign.fio
                    currency = CurrencyEnum.RUB
                    dailyBudget = DailyBudget().run {
                        amount = 500_000_000
                        mode = DailyBudgetModeEnum.STANDARD
                        FACTORY.createCampaignGetItemDailyBudget(this)
                    }
                    endDate = FACTORY.createCampaignGetItemEndDate(campaign.endDate?.toApiDate())
                    excludedSites = FACTORY.createCampaignGetItemExcludedSites(
                        listOf(SSP_PLATFORM_2, SSP_PLATFORM_1).toArrayOfString()
                    )
                    funds = FundsParam().apply {
                        mode = CampaignFundsEnum.CAMPAIGN_FUNDS
                        campaignFunds = CampaignFundsParam().apply {
                            sum = 300_000_000
                            balance = 83_333_333
                            balanceBonus = 0
                            sumAvailableForTransfer = 0
                        }
                    }
                    id = campaign.id
                    name = campaign.name
                    negativeKeywords = FACTORY.createCampaignGetItemNegativeKeywords(
                        listOf("слово").toArrayOfString()
                    )
                    notification = Notification().apply {
                        smsSettings = SmsSettings().apply {
                            events = listOf(SmsEventsEnum.FINISHED)
                            timeFrom = "01:02"
                            timeTo = "03:04"
                        }
                        emailSettings = EmailSettings().apply {
                            email = campaign.email
                            checkPositionInterval = 15
                            warningBalance = campaign.warningBalance
                            sendAccountNews = campaign.enableSendAccountNews.toYesNoEnum()
                            sendWarnings = campaign.enableCheckPositionEvent.toYesNoEnum()
                        }
                    }
                    representedBy = CampaignAssistant().apply {
                        manager = FACTORY.createCampaignAssistantManager(null)
                        agency = FACTORY.createCampaignAssistantAgency(null)
                    }
                    smartCampaign = null
                    sourceId = FACTORY.createCampaignGetItemSourceId(null)
                    startDate = campaign.startDate.toApiDate()
                    state = CampaignStateGetEnum.ON
                    statistics = Statistics().apply {
                        clicks = campaign.clicks
                        impressions = campaign.shows
                    }
                    status = StatusEnum.ACCEPTED
                    statusClarification = translationService.translate(
                        ConcatTranslatable(
                            ". ",
                            listOf(
                                StatusClarificationTranslations.INSTANCE.noAds(),
                                StatusClarificationTranslations.endDate(campaign.endDate),
                            )
                        )
                    )
                    statusPayment = CampaignStatusPaymentEnum.ALLOWED
                    timeTargeting = EXPECTED_TIME_TARGETING
                    timeZone = AMSTERDAM_TIMEZONE.timezone.id
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(""".*\.name\.*""", """.*\.scope\.*""") // поля из JAXBElement
            .isEqualTo(expectedResponse)
    }

    private fun createContentPromotionCampaign(block: ContentPromotionCampaign.() -> Unit): ContentPromotionCampaignInfo {
        val typedCampaign = TestContentPromotionCampaigns.fullContentPromotionCampaign().apply(block)
        return steps.contentPromotionCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }

    private companion object TestData {
        val TIME_TARGET: TimeTarget = TimeTarget.parseRawString(
            "1ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX2ABCDEFGHIJ" +
                "bKcLdMeNOfPgQhRqSrTsUtVuWpX3ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX4ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtV" +
                "uWpX5ABCDEFGHIJKLMNOPQRSTUVWX6AbBcCdDfEFGHIJKLbMcNdOfPcQdReSjTiUhVgWfX7AbBcCdDfEFGHIJKLbMcNdOfPcQdR" +
                "eSjTiUhVgWfX8JfKfLfMfNfOfPfQfRfSfTfUfVfWf;p:o"
        )

        private val TIME_TARGETING_SCHEDULE = listOf(
            "1,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "2,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "3,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "4,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "5,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100,100",
            "6,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100",
            "7,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100"
        )

        val EXPECTED_TIME_TARGETING = TimeTargeting().apply {
            schedule = ArrayOfString().withItems(TIME_TARGETING_SCHEDULE)
            holidaysSchedule = FACTORY.createTimeTargetingHolidaysSchedule(
                TimeTargetingOnPublicHolidays().apply {
                    bidPercent = 50
                    startHour = 9
                    endHour = 23
                    suspendOnHolidays = YesNoEnum.NO
                }
            )
            considerWorkingWeekends = YesNoEnum.NO
        }
    }
}
