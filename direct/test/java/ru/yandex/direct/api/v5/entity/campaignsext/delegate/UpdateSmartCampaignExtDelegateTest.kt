package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.yandex.direct.api.v5.campaignsext.CampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.SmartCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaignsext.SmartCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.SmartCampaignSearchStrategy
import com.yandex.direct.api.v5.campaignsext.SmartCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.SmartCampaignSetting
import com.yandex.direct.api.v5.campaignsext.SmartCampaignSettingsEnum
import com.yandex.direct.api.v5.campaignsext.SmartCampaignStrategy
import com.yandex.direct.api.v5.campaignsext.SmartCampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCpaPerCampaign
import com.yandex.direct.api.v5.campaignsext.StrategyNetworkDefault
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.AttributionModelEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.entity.campaigns.delegate.UpdateCampaignsExtDelegateBaseTest
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.feature.FeatureName
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateSmartCampaignExtDelegateTest : UpdateCampaignsExtDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<SmartCampaign>

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ADVANCED_GEOTARGETING, true)
        campaignInfo = steps.smartCampaignSteps().createDefaultCampaign(clientInfo)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New SmartCampaign"
        val expectedStartDate = LocalDate.now().plusDays(10)
        val expectedEndDate = expectedStartDate.plusDays(20)
        val expectedNegativeKeywords = listOf("negative", "keyword")
        val expectedTrackingParams = "utm_param=value"

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

        val updateItem = CampaignUpdateItem().apply {
            clientInfo = null
            notification = null
            timeZone = AMSTERDAM_TIMEZONE.timezone.id
            id = campaignInfo.id
            name = expectedName
            startDate = expectedStartDate.format(DATETIME_FORMATTER)
            dailyBudget = null // кажется, в смарт-кампаниях задание этой опции всегда возвращает ошибку
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
            smartCampaign = SmartCampaignUpdateItem().apply {
                counterId = COUNTER_ID
                attributionModel = AttributionModelEnum.FC
                biddingStrategy = TestStrategies.SEARCH_AVERAGE_CPA_PER_CAMPAIGN_NETWORK_DEFAULT_UPDATE
                priorityGoals =
                    OBJECT_FACTORY.createSmartCampaignUpdateItemPriorityGoals(PRIORITY_GOALS_UPDATE_SETTINGS)
                settings = listOf(
                    SmartCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    SmartCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    SmartCampaignSettingsEnum.ENABLE_CURRENT_AREA_TARGETING.enabled(),
                    // нельзя одновременно ENABLE_CURRENT_AREA_TARGETING и ENABLE_REGULAR_AREA_TARGETING
                    SmartCampaignSettingsEnum.ENABLE_REGULAR_AREA_TARGETING.disabled(),
                    SmartCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                )
                trackingParams = OBJECT_FACTORY.createSmartCampaignGetItemTrackingParams(expectedTrackingParams)
            }
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = SmartCampaign().apply {
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            enableCpcHold = false
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            favoriteForUids = setOf(clientInfo.uid)
            id = campaignInfo.id
            meaningfulGoals = EXPECTED_MEANINGFUL_GOAL
            metrikaCounters = listOf(COUNTER_ID)
            minusKeywords = expectedNegativeKeywords
            name = expectedName
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_AVERAGE_CPA_PER_CAMPAIGN_NETWORK_DEFAULT
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            hasExtendedGeoTargeting = true
            useCurrentRegion = true
            useRegularRegion = false
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
            bannerHrefParams = expectedTrackingParams
        }

        val actualCampaign = getUpdatedCampaign<SmartCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("disabledDomains", "disabledSsp", "minusKeywords")
            .ignoringExpectedNullFields()
            .ignoringFieldsMatchingRegexes(".*originalTimeTarget")
            .isEqualTo(expectedCampaign)
    }

    private fun SmartCampaignSettingsEnum.enabled() =
        SmartCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun SmartCampaignSettingsEnum.disabled() =
        SmartCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {
        val SEARCH_AVERAGE_CPA_PER_CAMPAIGN_NETWORK_DEFAULT_UPDATE = SmartCampaignStrategy().apply {
            search = SmartCampaignSearchStrategy().apply {
                biddingStrategyType = SmartCampaignSearchStrategyTypeEnum.AVERAGE_CPA_PER_CAMPAIGN
                averageCpaPerCampaign = StrategyAverageCpaPerCampaign().apply {
                    averageCpa = 1_995_000
                    goalId = VALID_GOAL_ID
                    weeklySpendLimit = OBJECT_FACTORY.createStrategyAverageCrrWeeklySpendLimit(3000_888_888)
                    bidCeiling = OBJECT_FACTORY.createStrategyAverageCpaPerCampaignBidCeiling(2_995_000)
                }
            }
            network = SmartCampaignNetworkStrategy().apply {
                biddingStrategyType = SmartCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                networkDefault = StrategyNetworkDefault().apply {
                }
            }
        }

        val EXPECTED_SEARCH_AVERAGE_CPA_PER_CAMPAIGN_NETWORK_DEFAULT = DbStrategy().apply {
            autobudget = CampaignsAutobudget.YES
            platform = CampaignsPlatform.BOTH
            strategy = CampOptionsStrategy.AUTOBUDGET_AVG_CPA_PER_CAMP
            strategyData = StrategyData().apply {
                version = 1
                name = StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP.name.lowercase()
                avgCpa = 2.toBigDecimal()
                goalId = VALID_GOAL_ID
                payForConversion = false
                bid = 3.toBigDecimal()
                sum = "3000.89".toBigDecimal()
            }
            strategyName = StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP
        }
    }
}
