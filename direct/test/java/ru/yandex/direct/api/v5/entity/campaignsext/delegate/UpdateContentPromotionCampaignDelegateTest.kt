package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.yandex.direct.api.v5.campaignsext.CampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSetting
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSettingsEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignUpdateItem
import com.yandex.direct.api.v5.campaignsext.DailyBudget
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCpc
import com.yandex.direct.api.v5.general.ArrayOfInteger
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
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.feature.FeatureName
import java.math.BigDecimal
import java.time.LocalDate

@Api5Test
@RunWith(SpringRunner::class)
class UpdateContentPromotionCampaignDelegateTest : UpdateCampaignsExtDelegateBaseTest() {

    private lateinit var campaignInfo: CampaignInfo<ContentPromotionCampaign>

    @Before
    fun before() {
        campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo)

        // включаем UNIVERSAL_CAMPAIGNS_BETA_DISABLED, чтобы все цели метрики не были автоматически доступны
        // чтобы качественнее проверить валидацию целей
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ADVANCED_GEOTARGETING, true)
    }

    @Test
    fun `update all available fields`() {
        val expectedName = "New ContentPromotionCampaign"
        val expectedStartDate = LocalDate.now().plusDays(10)
        val expectedEndDate = expectedStartDate.plusDays(20)
        val expectedNegativeKeywords = listOf("negative", "keyword")

        val requestedDailyBudgetAmount: Long = 300888888
        val expectedDayBudget = BigDecimal.valueOf(300.89)

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
            endDate = OBJECT_FACTORY.createCampaignUpdateItemEndDate(expectedEndDate.format(DATETIME_FORMATTER))
            contentPromotionCampaign = ContentPromotionCampaignUpdateItem().apply {
                counterIds = ArrayOfInteger().run {
                    items = listOf(COUNTER_ID.toInt())
                    OBJECT_FACTORY.createContentPromotionCampaignBaseCounterIds(this)
                }
                attributionModel = AttributionModelEnum.FC
                biddingStrategy = TestStrategies.SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE
                settings = listOf(
                    ContentPromotionCampaignSettingsEnum.ADD_TO_FAVORITES.enabled(),
                    ContentPromotionCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.enabled(),
                    ContentPromotionCampaignSettingsEnum.ENABLE_CURRENT_AREA_TARGETING.enabled(),
                    // нельзя одновременно ENABLE_CURRENT_AREA_TARGETING и ENABLE_REGULAR_AREA_TARGETING
                    ContentPromotionCampaignSettingsEnum.ENABLE_REGULAR_AREA_TARGETING.disabled(),
                    ContentPromotionCampaignSettingsEnum.REQUIRE_SERVICING.enabled(),
                )
            }
            timeTargeting = TIME_TARGETING_UPDATE
            notification = NOTIFICATION_UPDATE
        }

        val expectedCampaign = ContentPromotionCampaign().apply {
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            checkPositionIntervalEvent = EXPECTED_CHECK_POSITION_INTERVAL_EVENT
            dayBudget = expectedDayBudget
            dayBudgetShowMode = DayBudgetShowMode.DEFAULT_
            email = EXPECTED_EMAIL
            enableCheckPositionEvent = EXPECTED_SEND_WARN
            enableCompanyInfo = true
            enableCpcHold = false
            enableSendAccountNews = EXPECTED_SEND_NEWS
            endDate = expectedEndDate
            favoriteForUids = setOf(clientInfo.uid)
            hasExtendedGeoTargeting = true
            hasTitleSubstitution = true
            id = campaignInfo.id
            metrikaCounters = listOf(COUNTER_ID)
            minusKeywords = expectedNegativeKeywords
            name = expectedName
            smsFlags = EXPECTED_SMS_EVENTS
            smsTime = EXPECTED_SMS_INTERVAL
            startDate = expectedStartDate
            strategy = TestStrategies.EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE
            timeTarget = EXPECTED_TIME_TARGET
            timeZoneId = AMSTERDAM_TIMEZONE.timezoneId
            warningBalance = EXPECTED_WARNING_BALANCE
            useCurrentRegion = true
            useRegularRegion = false
            // привязалась к менеджеру т.к. передали REQUIRE_SERVICING (до этого не была привязана)
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<ContentPromotionCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringCollectionOrderInFields("minusKeywords")
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
            contentPromotionCampaign = ContentPromotionCampaignUpdateItem().apply {
                settings = listOf(
                    ContentPromotionCampaignSettingsEnum.ADD_TO_FAVORITES.disabled(),
                    ContentPromotionCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING.disabled(),
                    ContentPromotionCampaignSettingsEnum.ENABLE_CURRENT_AREA_TARGETING.disabled(),
                    // нельзя одновременно отключать ENABLE_CURRENT_AREA_TARGETING и ENABLE_REGULAR_AREA_TARGETING
                    // поэтому ENABLE_REGULAR_AREA_TARGETING включим
                    ContentPromotionCampaignSettingsEnum.ENABLE_REGULAR_AREA_TARGETING.enabled(),
                    // пробуем отвязать от менеджера, но не отвяжется, потому что нельзя
                    ContentPromotionCampaignSettingsEnum.REQUIRE_SERVICING.disabled(),
                )
            }
        }

        val expectedCampaign = ContentPromotionCampaign().apply {
            id = campaignInfo.id
            favoriteForUids = null
            hasExtendedGeoTargeting = false
            useCurrentRegion = false
            useRegularRegion = true
            // нельзя отвязывать от менеджера, поэтому он остался
            managerUid = managerInfo.uid
        }

        val actualCampaign = getUpdatedCampaign<ContentPromotionCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    @Test
    fun `update search strategy to AVERAGE_CPC and network strategy to SERVING_OFF`() {
        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            contentPromotionCampaign = ContentPromotionCampaignUpdateItem().apply {
                biddingStrategy = TestStrategies.SEARCH_AVERAGE_CPC_NETWORK_SERVING_OFF_UPDATE
            }
        }

        val expectedCampaign = ContentPromotionCampaign().apply {
            id = campaignInfo.id
            enableCpcHold = false
            strategy = TestStrategies.EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_SERVING_OFF
        }

        val actualCampaign = getUpdatedCampaign<ContentPromotionCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }

    private fun ContentPromotionCampaignSettingsEnum.enabled() =
        ContentPromotionCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.YES)

    private fun ContentPromotionCampaignSettingsEnum.disabled() =
        ContentPromotionCampaignSetting()
            .withOption(this)
            .withValue(YesNoEnum.NO)

    private object TestStrategies {

         val SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE = ContentPromotionCampaignStrategy().apply {
            search = ContentPromotionCampaignSearchStrategy().apply {
                biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
            }
            network = ContentPromotionCampaignNetworkStrategy().apply {
                biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }

        }

        val EXPECTED_SEARCH_HIGHEST_POSITION_NETWORK_SERVING_OFF_UPDATE = DbStrategy().apply {
            autobudget = CampaignsAutobudget.NO
            platform = CampaignsPlatform.SEARCH
            strategy = null
            strategyData = StrategyData().apply {
                version = 1
                name = "default"
            }
            strategyName = StrategyName.DEFAULT_
        }

        val SEARCH_AVERAGE_CPC_NETWORK_SERVING_OFF_UPDATE = ContentPromotionCampaignStrategy().apply {
            search = ContentPromotionCampaignSearchStrategy().apply {
                biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                averageCpc = StrategyAverageCpc().apply {
                    averageCpc = 1_995_000
                    weeklySpendLimit = OBJECT_FACTORY.createStrategyAverageCpcWeeklySpendLimit(3000_888_888)
                }

            }
            network = ContentPromotionCampaignNetworkStrategy().apply {
                biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val EXPECTED_SEARCH_AVERAGE_CPC_NETWORK_SERVING_OFF = DbStrategy().apply {
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
