package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.yandex.direct.api.v5.campaignsext.CampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignSearchStrategy
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignSetting
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignSettingsEnum
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignStrategy
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.DailyBudget
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaignsext.FrequencyCapSetting
import com.yandex.direct.api.v5.campaignsext.StrategyWbAverageCpv
import com.yandex.direct.api.v5.campaignsext.TimeTargeting
import com.yandex.direct.api.v5.campaignsext.TimeTargetingOnPublicHolidays
import com.yandex.direct.api.v5.general.ArrayOfInteger
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.VideoTargetEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.entity.campaigns.delegate.UpdateCampaignsExtDelegateBaseTest
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.libs.timetarget.TimeTarget
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateCpmBannerCampaignExtDelegateTest : UpdateCampaignsExtDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<CpmBannerCampaign>

    @Before
    fun before() {
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS)
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        campaignInfo = steps.cpmBannerCampaignSteps().createDefaultCampaign(clientInfo)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New CpmBannerCampaign"
        val expectedStartDate = LocalDate.now().plusDays(10)
        val expectedEndDate = expectedStartDate.plusDays(20)

        val requestedDailyBudgetAmount: Long = 300888888
        val expectedDayBudget = "300.89".toBigDecimal()

        val expectedTimeTarget = TimeTarget.parseRawString(
            "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUV" +
                "WX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRST" +
                "UVWX7ABCDEFGHIJKLMNOPQRSTUVWX8JKLMNOPQRSTUVW;p:o"
        )

        val requestedExcludedSites = listOf(
            SSP_PLATFORM_1.uppercase(),
            SSP_PLATFORM_2,
            "google.ru 1",
            "google.com"
        )
        val expectedDisabledSsp =
            listOf(SSP_PLATFORM_1, SSP_PLATFORM_2)
        val expectedDisabledDomains =
            listOf(SSP_PLATFORM_2, "google.ru", "google.com")

        val requestedVideoTarget = VideoTargetEnum.CLICKS
        val expectedEshowSettings = EshowsSettings().apply {
            videoType = EshowsVideoType.LONG_CLICKS
        }

        val expectedImpressions = 20
        val expectedPeriodDays = 10

        val updateItem = CampaignUpdateItem().apply {
            clientInfo = null
            notification = null
            timeZone = AMSTERDAM_TIMEZONE.timezone.id
            id = campaignInfo.id
            name = expectedName
            startDate = expectedStartDate.format(DATETIME_FORMATTER)
            dailyBudget = DailyBudget().run {
                amount = requestedDailyBudgetAmount
                mode = DailyBudgetModeEnum.STANDARD
                OBJECT_FACTORY.createCampaignUpdateItemDailyBudget(this)
            }
            negativeKeywords = null
            // не можем протестировать blockedIps, т.к. в network-config.allow-all.json все ip объявляются внутренними,
            // а ядро не разрешает блокировать внутренние ip
            blockedIps = null
            excludedSites = ArrayOfString().run {
                items = requestedExcludedSites
                OBJECT_FACTORY.createCampaignUpdateItemExcludedSites(this)
            }
            endDate = OBJECT_FACTORY.createCampaignUpdateItemEndDate(expectedEndDate.format(DATETIME_FORMATTER))
            cpmBannerCampaign = CpmBannerCampaignUpdateItem().apply {
                counterIds = ArrayOfInteger().run {
                    items = listOf(COUNTER_ID.toInt())
                    OBJECT_FACTORY.createCpmBannerCampaignBaseCounterIds(this)
                }
                frequencyCap = null
                biddingStrategy = TestStrategies.SEARCH_OFF_NETWORK_MANUAL_CPM_UPDATE
                frequencyCap = FrequencyCapSetting().run {
                    impressions = expectedImpressions
                    periodDays = expectedPeriodDays
                    OBJECT_FACTORY.createCpmBannerCampaignBaseFrequencyCap(this)
                }
                settings = listOf(
                    CpmBannerCampaignSettingsEnum.ADD_METRICA_TAG.enabled(),
                    CpmBannerCampaignSettingsEnum.ADD_OPENSTAT_TAG.enabled(),
                    CpmBannerCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    CpmBannerCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    CpmBannerCampaignSettingsEnum.ENABLE_SITE_MONITORING.enabled(),
                    CpmBannerCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                )
                videoTarget = requestedVideoTarget
            }
            timeTargeting = TimeTargeting().apply {
                holidaysSchedule = OBJECT_FACTORY.createTimeTargetingHolidaysSchedule(
                    TimeTargetingOnPublicHolidays().apply {
                        bidPercent = 100
                        startHour = 9
                        endHour = 23
                        suspendOnHolidays = YesNoEnum.NO
                    }
                )
                considerWorkingWeekends = YesNoEnum.NO
            }
            notification = NOTIFICATION_UPDATE.apply {
                emailSettings.apply {
                    checkPositionInterval = null
                    sendWarnings = null
                }
            }
        }

        val expectedCampaign = CpmBannerCampaign().apply {
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            email = EXPECTED_EMAIL
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            eshowsSettings = expectedEshowSettings
            favoriteForUids = setOf(clientInfo.uid)
            hasAddMetrikaTagToUrl = true
            hasAddOpenstatTagToUrl = true
            hasExtendedGeoTargeting = true
            hasSiteMonitoring = true
            id = campaignInfo.id
            impressionRateCount = expectedImpressions
            impressionRateIntervalDays = expectedPeriodDays
            metrikaCounters = listOf(COUNTER_ID)
            name = expectedName
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_OFF_NETWORK_MANUAL_CPM
            timeTarget = expectedTimeTarget
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<CpmBannerCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .ignoringCollectionOrderInFields("disabledDomains", "disabledSsp")
            .ignoringFieldsMatchingRegexes(".*originalTimeTarget")
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `disable all settings`() {
        // привяжем текущую кампанию к менеджеру
        campaignRepository.setManagerForAllClientCampaigns(clientInfo.shard, clientInfo.clientId!!, managerInfo.uid)

        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            cpmBannerCampaign = CpmBannerCampaignUpdateItem().apply {
                settings = listOf(
                    CpmBannerCampaignSettingsEnum.ADD_METRICA_TAG.disabled(),
                    CpmBannerCampaignSettingsEnum.ADD_OPENSTAT_TAG.disabled(),
                    CpmBannerCampaignSettingsEnum.ADD_TO_FAVORITES.disabled(),
                    CpmBannerCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.disabled(),
                    CpmBannerCampaignSettingsEnum.ENABLE_SITE_MONITORING.disabled(),
                    // пробуем отвязать от менеджера, но не отвяжется, потому что нельзя
                    CpmBannerCampaignSettingsEnum.REQUIRE_SERVICING.disabled(),
                )
            }
        }

        val expectedCampaign = CpmBannerCampaign().apply {
            id = campaignInfo.id
            favoriteForUids = null
            hasAddMetrikaTagToUrl = false
            hasAddOpenstatTagToUrl = false
            hasExtendedGeoTargeting = false
            hasSiteMonitoring = false
            // нельзя отвязывать от менеджера, поэтому он остался
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<CpmBannerCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update search strategy to SERVING_OFF and network strategy to WB_AVERAGE_CPV`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            cpmBannerCampaign = CpmBannerCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_OFF_NETWORK_WB_AVERAGE_CPV_UPDATE
            }
        }

        val actualCampaign = getUpdatedCampaign<CpmBannerCampaign>(updateItem)

        assertThat(actualCampaign.strategy)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(TestStrategies.EXPECTED_SEARCH_OFF_NETWORK_WB_AVERAGE_CPV)
    }

    private fun CpmBannerCampaignSettingsEnum.enabled() =
        CpmBannerCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun CpmBannerCampaignSettingsEnum.disabled() =
        CpmBannerCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {
        val SEARCH_OFF_NETWORK_MANUAL_CPM_UPDATE = CpmBannerCampaignStrategy().apply {
            search = CpmBannerCampaignSearchStrategy().apply {
                biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
            }
            network = CpmBannerCampaignNetworkStrategy().apply {
                biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.MANUAL_CPM
            }
        }

        val EXPECTED_SEARCH_OFF_NETWORK_MANUAL_CPM = DbStrategy().apply {
            autobudget = CampaignsAutobudget.NO
            platform = CampaignsPlatform.CONTEXT
            strategy = CampOptionsStrategy.DIFFERENT_PLACES
            strategyData = StrategyData().apply {
                version = 1
                name = StrategyName.CPM_DEFAULT.name.lowercase()
            }
            strategyName = StrategyName.CPM_DEFAULT
        }

        val SEARCH_OFF_NETWORK_WB_AVERAGE_CPV_UPDATE = CpmBannerCampaignStrategy().apply {
            search = CpmBannerCampaignSearchStrategy().apply {
                biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
            }
            network = CpmBannerCampaignNetworkStrategy().apply {
                biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.WB_AVERAGE_CPV
                wbAverageCpv = StrategyWbAverageCpv().apply {
                    averageCpv = 1_995_000
                    spendLimit = 3000_888_888
                }
            }
        }

        val EXPECTED_SEARCH_OFF_NETWORK_WB_AVERAGE_CPV = DbStrategy().apply {
            autobudget = CampaignsAutobudget.YES
            platform = CampaignsPlatform.CONTEXT
            strategy = CampOptionsStrategy.DIFFERENT_PLACES
            strategyData = StrategyData().apply {
                version = 1
                name = StrategyName.AUTOBUDGET_AVG_CPV.name.lowercase()
                sum = "3000.89".toBigDecimal()
                avgCpv = 2.toBigDecimal()
            }
            strategyName = StrategyName.AUTOBUDGET_AVG_CPV
        }
    }
}
