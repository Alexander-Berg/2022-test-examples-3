package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.DailyBudget
import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaigns.RelevantKeywordsSetting
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefault
import com.yandex.direct.api.v5.campaigns.StrategyPayForConversionCrr
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSetting
import com.yandex.direct.api.v5.campaigns.TextCampaignSettingsEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignUpdateItem
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
import ru.yandex.direct.core.entity.campaign.model.BroadMatch
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.feature.FeatureName
import java.math.BigDecimal
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateTextCampaignDelegateTest : UpdateCampaignsDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<TextCampaign>
    private val FACTORY = com.yandex.direct.api.v5.campaigns.ObjectFactory()

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))
        campaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)

        // включаем UNIVERSAL_CAMPAIGNS_BETA_DISABLED, чтобы все цели метрики не были автоматически доступны
        // чтобы качественнее проверить валидацию целей
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)

        // для CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.IS_ORDER_PHRASE_LENGTH_PRECEDENCE_ALLOWED, true)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New TextCampaign"
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

        val expectedBudgetPercent = 99
        val exceptedOptimizeGoalId = 0L

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
            textCampaign = TextCampaignUpdateItem().apply {
                counterIds = ArrayOfInteger().run {
                    items = listOf(COUNTER_ID.toInt())
                    OBJECT_FACTORY.createCpmBannerCampaignBaseCounterIds(this)
                }
                relevantKeywords = null
                attributionModel = AttributionModelEnum.FC
                biddingStrategy = TestStrategies.SEARCH_HIGHEST_POSITION_NETWORK_DEFAULT_UPDATE
                relevantKeywords = RelevantKeywordsSetting().run {
                    budgetPercent = expectedBudgetPercent
                    optimizeGoalId = OBJECT_FACTORY.createRelevantKeywordsSettingOptimizeGoalId(exceptedOptimizeGoalId)
                    OBJECT_FACTORY.createTextCampaignBaseRelevantKeywords(this)
                }
                priorityGoals = OBJECT_FACTORY.createTextCampaignUpdateItemPriorityGoals(PRIORITY_GOALS_UPDATE_SETTINGS)
                settings = listOf(
                    TextCampaignSettingsEnum.ADD_METRICA_TAG.enabled(),
                    TextCampaignSettingsEnum.ADD_OPENSTAT_TAG.enabled(),
                    TextCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    TextCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    TextCampaignSettingsEnum.ENABLE_COMPANY_INFO.enabled(),
                    TextCampaignSettingsEnum.ENABLE_EXTENDED_AD_TITLE.enabled(),
                    TextCampaignSettingsEnum.ENABLE_SITE_MONITORING.enabled(),
                    TextCampaignSettingsEnum.EXCLUDE_PAUSED_COMPETING_ADS.enabled(),
                    TextCampaignSettingsEnum.MAINTAIN_NETWORK_CPC.enabled(),
                    TextCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                    TextCampaignSettingsEnum.CAMPAIGN_EXACT_PHRASE_MATCHING_ENABLED.enabled(),
                )
                trackingParams = OBJECT_FACTORY.createTextCampaignUpdateItemTrackingParams(expectedTrackingParams)
            }
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = TextCampaign().apply {
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            broadMatch = BroadMatch().apply {
                broadMatchFlag = true
                broadMatchLimit = expectedBudgetPercent
                broadMatchGoalId = exceptedOptimizeGoalId
            }
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            contextLimit = 10
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            disabledDomains = expectedDisabledDomains
            disabledSsp = expectedDisabledSsp
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            enableCompanyInfo = true
            enableCpcHold = true
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            excludePausedCompetingAds = true
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
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_DEFAULT
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
            isOrderPhraseLengthPrecedenceEnabled = true
            bannerHrefParams = expectedTrackingParams
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

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
            textCampaign = TextCampaignUpdateItem().apply {
                settings = listOf(
                    TextCampaignSettingsEnum.ADD_METRICA_TAG.disabled(),
                    TextCampaignSettingsEnum.ADD_OPENSTAT_TAG.disabled(),
                    TextCampaignSettingsEnum.ADD_TO_FAVORITES.disabled(),
                    TextCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.disabled(),
                    TextCampaignSettingsEnum.ENABLE_COMPANY_INFO.disabled(),
                    TextCampaignSettingsEnum.ENABLE_EXTENDED_AD_TITLE.disabled(),
                    TextCampaignSettingsEnum.ENABLE_SITE_MONITORING.disabled(),
                    TextCampaignSettingsEnum.EXCLUDE_PAUSED_COMPETING_ADS.disabled(),
                    TextCampaignSettingsEnum.MAINTAIN_NETWORK_CPC.disabled(),
                    // пробуем отвязать от менеджера, но не отвяжется, потому что нельзя
                    TextCampaignSettingsEnum.REQUIRE_SERVICING.disabled(),
                )
            }
        }

        val expectedCampaign = TextCampaign().apply {
            id = campaignInfo.id
            enableCompanyInfo = false
            enableCpcHold = false
            excludePausedCompetingAds = false
            favoriteForUids = null
            hasAddMetrikaTagToUrl = false
            hasAddOpenstatTagToUrl = false
            hasExtendedGeoTargeting = false
            hasSiteMonitoring = false
            hasTitleSubstitution = false
            // нельзя отвязывать от менеджера, поэтому он остался
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update search strategy to AVERAGE_CPC and network strategy to NETWORK_DEFAULT`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            textCampaign = TextCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_AVERAGE_CPC_NETWORK_DEFAULT_UPDATE
            }
        }

        val expectedCampaign = TextCampaign().apply {
            id = campaignInfo.id
            enableCpcHold = false
            strategy = TestStrategies.EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_DEFAULT
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update strategy setting goalId but not counterId`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            textCampaign = TextCampaignUpdateItem().apply {
                biddingStrategy = TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.PAY_FOR_CONVERSION_CRR
                        payForConversionCrr = StrategyPayForConversionCrr().apply {
                            crr = 100
                            goalId = VALID_GOAL_ID
                            weeklySpendLimit = FACTORY.createStrategyPayForConversionCrrWeeklySpendLimit(500000000L)
                        }
                    }
                    network = TextCampaignNetworkStrategy().apply {
                        biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                    }
                }
            }
        }

        val expectedCampaign = TextCampaign().apply {
            id = campaignInfo.id
            strategy = DbStrategy().apply {
                autobudget = CampaignsAutobudget.YES
                platform = CampaignsPlatform.SEARCH
                strategy = null
                strategyData = StrategyData().apply {
                    version = 1
                    name = StrategyName.AUTOBUDGET_CRR.name.lowercase()
                    sum = "500".toBigDecimal()
                    payForConversion = true
                    crr = 100L
                    goalId = VALID_GOAL_ID
                }
                strategyName = StrategyName.AUTOBUDGET_CRR
            }
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    private fun TextCampaignSettingsEnum.enabled() =
        TextCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun TextCampaignSettingsEnum.disabled() =
        TextCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {
        val SEARCH_HIGHEST_POSITION_NETWORK_DEFAULT_UPDATE = TextCampaignStrategy().apply {
            search = TextCampaignSearchStrategy().apply {
                biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION

            }
            network = TextCampaignNetworkStrategy().apply {
                biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                networkDefault = StrategyNetworkDefault().apply {
                    limitPercent = 10
                }
            }
        }

        val EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_DEFAULT = DbStrategy().apply {
            autobudget = CampaignsAutobudget.NO
            platform = CampaignsPlatform.BOTH
            strategy = null
            strategyData = StrategyData().apply {
                version = 1
                name = "default"
            }
            strategyName = StrategyName.DEFAULT_
        }

        val SEARCH_AVERAGE_CPC_NETWORK_DEFAULT_UPDATE = TextCampaignStrategy().apply {
            search = TextCampaignSearchStrategy().apply {
                biddingStrategyType = TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                averageCpc = StrategyAverageCpc().apply {
                    averageCpc = 1_995_000
                    weeklySpendLimit = OBJECT_FACTORY.createStrategyAverageCpcWeeklySpendLimit(3000_888_888)
                }

            }
            network = TextCampaignNetworkStrategy().apply {
                biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                networkDefault = StrategyNetworkDefault()
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
