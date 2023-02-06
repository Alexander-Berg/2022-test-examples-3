package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.DailyBudget
import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSetting
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSettingsEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.PlacementType
import com.yandex.direct.api.v5.campaigns.PlacementTypesEnum
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.general.ArrayOfInteger
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.AttributionModelEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType.SEARCH_PAGE
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.feature.FeatureName
import java.math.BigDecimal
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateDynamicTextCampaignDelegateTest : UpdateCampaignsDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<DynamicCampaign>

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        campaignInfo = steps.dynamicCampaignSteps().createDefaultCampaign(clientInfo)
        // для CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.IS_ORDER_PHRASE_LENGTH_PRECEDENCE_ALLOWED, true)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New DynamicTextCampaign"
        val expectedStartDate = LocalDate.now().plusDays(10)
        val expectedEndDate = expectedStartDate.plusDays(20)
        val expectedNegativeKeywords = listOf("negative", "keyword")
        val expectedTrackingParams = "utm_param=value"

        val requestedDailyBudgetAmount: Long = 300888888
        val expectedDayBudget = BigDecimal.valueOf(300.89)

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

        val requestedPlacementTypes = listOf(
            PlacementTypesEnum.PRODUCT_GALLERY.disabled(),
            PlacementTypesEnum.SEARCH_RESULTS.enabled(),
        )
        val expectedPlacementTypes = setOf(SEARCH_PAGE)

        val updateItem = CampaignUpdateItem().apply {
            clientInfo = null
            timeZone = AMSTERDAM_TIMEZONE.timezone.id
            id = campaignInfo.id
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
            dynamicTextCampaign = DynamicTextCampaignUpdateItem().apply {
                counterIds = ArrayOfInteger().run {
                    items = listOf(COUNTER_ID.toInt())
                    OBJECT_FACTORY.createDynamicTextCampaignBaseCounterIds(this)
                }
                attributionModel = AttributionModelEnum.FC
                biddingStrategy = TestStrategies.SEARCH_HIGHEST_POSITION_NETWORK_OFF_UPDATE
                priorityGoals =
                    OBJECT_FACTORY.createDynamicTextCampaignUpdateItemPriorityGoals(PRIORITY_GOALS_UPDATE_SETTINGS)
                settings = listOf(
                    DynamicTextCampaignSettingsEnum.ADD_METRICA_TAG.enabled(),
                    DynamicTextCampaignSettingsEnum.ADD_OPENSTAT_TAG.enabled(),
                    DynamicTextCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_COMPANY_INFO.enabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_EXTENDED_AD_TITLE.enabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_SITE_MONITORING.enabled(),
                    DynamicTextCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                    DynamicTextCampaignSettingsEnum.CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED.enabled(),
                )
                placementTypes = requestedPlacementTypes
                trackingParams = OBJECT_FACTORY.createDynamicTextCampaignUpdateItemTrackingParams(expectedTrackingParams)
            }
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = DynamicCampaign().apply {
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            enableCompanyInfo = true
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            favoriteForUids = setOf(clientInfo.uid)
            hasAddMetrikaTagToUrl = true
            hasAddOpenstatTagToUrl = true
            hasExtendedGeoTargeting = true
            hasSiteMonitoring = true
            hasTitleSubstitution = true
            id = campaignInfo.id
            meaningfulGoals = EXPECTED_MEANINGFUL_GOAL
            metrikaCounters = listOf(COUNTER_ID)
            minusKeywords = expectedNegativeKeywords
            name = expectedName
            placementTypes = expectedPlacementTypes
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_OFF
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
            isOrderPhraseLengthPrecedenceEnabled = true
            bannerHrefParams = expectedTrackingParams
        }

        val actualCampaign = getUpdatedCampaign<DynamicCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("disabledDomains", "disabledSsp", "minusKeywords")
            .ignoringFieldsMatchingRegexes(".*originalTimeTarget")
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `disable all settings`() {
        // привяжем текущую кампанию к менеджеру
        campaignRepository.setManagerForAllClientCampaigns(clientInfo.shard, clientInfo.clientId!!, managerInfo.uid)

        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            dynamicTextCampaign = DynamicTextCampaignUpdateItem().apply {
                settings = listOf(
                    DynamicTextCampaignSettingsEnum.ADD_METRICA_TAG.disabled(),
                    DynamicTextCampaignSettingsEnum.ADD_OPENSTAT_TAG.disabled(),
                    DynamicTextCampaignSettingsEnum.ADD_TO_FAVORITES.disabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.disabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_COMPANY_INFO.disabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_EXTENDED_AD_TITLE.disabled(),
                    DynamicTextCampaignSettingsEnum.ENABLE_SITE_MONITORING.disabled(),
                    // пробуем отвязать от менеджера, но не отвяжется, потому что нельзя
                    DynamicTextCampaignSettingsEnum.REQUIRE_SERVICING.disabled(),
                )
            }
        }

        val expectedCampaign = DynamicCampaign().apply {
            id = campaignInfo.id
            enableCompanyInfo = false
            favoriteForUids = null
            hasAddMetrikaTagToUrl = false
            hasAddOpenstatTagToUrl = false
            hasExtendedGeoTargeting = false
            hasSiteMonitoring = false
            hasTitleSubstitution = false
            // нельзя отвязывать от менеджера, поэтому он остался
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<DynamicCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update search strategy to AVERAGE_CPC`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            dynamicTextCampaign = DynamicTextCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_AVERAGE_CPC_NETWORK_OFF_UPDATE
            }
        }

        val expectedCampaign = DynamicCampaign().apply {
            id = campaignInfo.id
            strategy = TestStrategies.EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_OFF
            enableCpcHold = false // так как стратегия автобюджетная
        }

        val actualCampaign = getUpdatedCampaign<DynamicCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    private fun DynamicTextCampaignSettingsEnum.enabled() =
        DynamicTextCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun DynamicTextCampaignSettingsEnum.disabled() =
        DynamicTextCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private fun PlacementTypesEnum.enabled() =
        PlacementType()
            .withType(this)
            .withValue(YesNoEnum.YES)

    private fun PlacementTypesEnum.disabled() =
        PlacementType()
            .withType(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {
        val SEARCH_HIGHEST_POSITION_NETWORK_OFF_UPDATE = DynamicTextCampaignStrategy().apply {
            search = DynamicTextCampaignSearchStrategy().apply {
                biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION

            }
            network = DynamicTextCampaignNetworkStrategy().apply {
                biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_OFF = DbStrategy().apply {
            autobudget = CampaignsAutobudget.NO
            platform = CampaignsPlatform.SEARCH
            strategy = null
            strategyData = StrategyData().apply {
                version = 1
                name = "default"
            }
            strategyName = StrategyName.DEFAULT_
        }

        val SEARCH_AVERAGE_CPC_NETWORK_OFF_UPDATE = DynamicTextCampaignStrategy().apply {
            search = DynamicTextCampaignSearchStrategy().apply {
                biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                averageCpc = StrategyAverageCpc().apply {
                    averageCpc = 1_995_000
                    weeklySpendLimit = OBJECT_FACTORY.createStrategyAverageCpcWeeklySpendLimit(3000_888_888)
                }

            }
            network = DynamicTextCampaignNetworkStrategy().apply {
                biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_OFF = DbStrategy().apply {
            autobudget = CampaignsAutobudget.YES
            platform = CampaignsPlatform.SEARCH
            strategy = null
            strategyData = StrategyData().apply {
                version = 1
                name = StrategyName.AUTOBUDGET_AVG_CLICK.name.lowercase()
                sum = "3000.89".toBigDecimal()
                avgBid = 2.toBigDecimal()
            }
            strategyName = StrategyName.AUTOBUDGET_AVG_CLICK
        }
    }
}
