package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.DailyBudget
import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSetting
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSettingsEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefault
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.feature.FeatureName
import java.math.BigDecimal
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateMobileAppCampaignDelegateTest : UpdateCampaignsDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<MobileContentCampaign>

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        campaignInfo = steps.mobileContentCampaignSteps().createDefaultCampaign(clientInfo)

        // для CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.IS_ORDER_PHRASE_LENGTH_PRECEDENCE_ALLOWED, true)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ADVANCED_GEOTARGETING, true)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New MobileAppCampaign"
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
            mobileAppCampaign = MobileAppCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE
                settings = listOf(
                    MobileAppCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    MobileAppCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    MobileAppCampaignSettingsEnum.ENABLE_CURRENT_AREA_TARGETING.enabled(),
                    // нельзя одновременно ENABLE_CURRENT_AREA_TARGETING и ENABLE_REGULAR_AREA_TARGETING
                    MobileAppCampaignSettingsEnum.ENABLE_REGULAR_AREA_TARGETING.disabled(),
                    MobileAppCampaignSettingsEnum.MAINTAIN_NETWORK_CPC.enabled(),
                    MobileAppCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                    MobileAppCampaignSettingsEnum.CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED.enabled(),
                )
            }
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = MobileContentCampaign().apply {
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            contextLimit = 100
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            enableCpcHold = true
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            favoriteForUids = setOf(clientInfo.uid)
            hasExtendedGeoTargeting = true
            id = campaignInfo.id
            minusKeywords = expectedNegativeKeywords
            name = expectedName
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
            useCurrentRegion = true
            useRegularRegion = false
            isOrderPhraseLengthPrecedenceEnabled = true
        }

        val actualCampaign = getUpdatedCampaign<MobileContentCampaign>(updateItem)

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
            mobileAppCampaign = MobileAppCampaignUpdateItem().apply {
                settings = listOf(
                    MobileAppCampaignSettingsEnum.ADD_TO_FAVORITES.disabled(),
                    MobileAppCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.disabled(),
                    MobileAppCampaignSettingsEnum.MAINTAIN_NETWORK_CPC.disabled(),
                    // пробуем отвязать от менеджера, но не отвяжется, потому что нельзя
                    MobileAppCampaignSettingsEnum.REQUIRE_SERVICING.disabled(),
                )
            }
        }

        val expectedCampaign = MobileContentCampaign().apply {
            id = campaignInfo.id
            enableCpcHold = false
            favoriteForUids = null
            hasExtendedGeoTargeting = false
            // нельзя отвязывать от менеджера, поэтому он остался
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<MobileContentCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update search strategy to AVERAGE_CPC and network strategy to SERVING_OFF`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            mobileAppCampaign = MobileAppCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_AVERAGE_CPC_NETWORK_DEFAULT_UPDATE
            }
        }

        val expectedCampaign = MobileContentCampaign().apply {
            id = campaignInfo.id
            strategy = TestStrategies.EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_DEFAULT
            enableCpcHold = false // так как стратегия автобюджетная
        }

        val actualCampaign = getUpdatedCampaign<MobileContentCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    private fun MobileAppCampaignSettingsEnum.enabled() =
        MobileAppCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun MobileAppCampaignSettingsEnum.disabled() =
        MobileAppCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {
        val SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE = MobileAppCampaignStrategy().apply {
            search = MobileAppCampaignSearchStrategy().apply {
                biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
            }
            network = MobileAppCampaignNetworkStrategy().apply {
                biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF = DbStrategy().apply {
            autobudget = CampaignsAutobudget.NO
            platform = CampaignsPlatform.SEARCH
            strategy = null
            strategyData = StrategyData().apply {
                version = 1
                name = "default"
            }
            strategyName = StrategyName.DEFAULT_
        }

        val SEARCH_AVERAGE_CPC_NETWORK_DEFAULT_UPDATE = MobileAppCampaignStrategy().apply {
            search = MobileAppCampaignSearchStrategy().apply {
                biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                averageCpc = StrategyAverageCpc().apply {
                    averageCpc = 1_995_000
                    weeklySpendLimit = OBJECT_FACTORY.createStrategyAverageCpcWeeklySpendLimit(3000_888_888)
                }

            }
            network = MobileAppCampaignNetworkStrategy().apply {
                biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                networkDefault = StrategyNetworkDefault().apply {
                }
            }
        }

        val EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_DEFAULT = DbStrategy().apply {
            autobudget = CampaignsAutobudget.YES
            platform = CampaignsPlatform.BOTH
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
