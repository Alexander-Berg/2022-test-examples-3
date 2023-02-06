package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefault
import com.yandex.direct.api.v5.campaigns.StrategyWeeklyClickPackage
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.UpdateRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.entity.ApiValidationException
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategyWithoutDayBudget
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode
import java.math.BigDecimal
import java.math.RoundingMode

@Api5Test
@RunWith(SpringRunner::class)
class UpdateTextCampaignApiValidationTest : UpdateCampaignsDelegateBaseTest() {

    private var campaignId: Long = -1
    private val FACTORY = com.yandex.direct.api.v5.campaigns.ObjectFactory()

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))

        campaignId = steps.campaignSteps()
            .createCampaign(
                TextCampaignInfo()
                    .withTypedCampaign(fullTextCampaign())
                    .withCampaign(
                        TestCampaigns.activeTextCampaign(null, null).apply {
                            strategy = manualStrategyWithoutDayBudget().apply {
                                dayBudget = DayBudget().apply {
                                    dayBudget = BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN)
                                    showMode = DayBudgetShowMode.STRETCHED
                                    dailyChangeCount = 0L
                                    stopNotificationSent = false
                                }
                            }
                        }
                    )
                    .withClientInfo(clientInfo)
            )
            .campaignId
    }

    @Test(expected = ApiValidationException::class)
    fun `UpdateRequest with too many campaigns is invalid`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withName("New cool name")

        val request = UpdateRequest()
            .withCampaigns(List(11) { updateItem })

        genericApiService.doAction(delegate, request)
    }

    @Test
    fun `CampaignUpdateItem with two campaigns is invalid`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem())
            .withDynamicTextCampaign(DynamicTextCampaignUpdateItem())
        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5009)
    }

    @Test
    fun `CampaignUpdateItem with LimitPercent not dividing ten`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem().apply {
                biddingStrategy = TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                    }
                    network = TextCampaignNetworkStrategy().apply {
                        biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                        networkDefault = StrategyNetworkDefault().apply {
                            limitPercent = 39
                        }
                    }
                }
            })

        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5005)
    }

    @Test
    fun `CampaignUpdateItem with LimitPercent not in range validation error`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem().apply {
                biddingStrategy = TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                    }
                    network = TextCampaignNetworkStrategy().apply {
                        biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                        networkDefault = StrategyNetworkDefault().apply {
                            limitPercent = 210
                        }
                    }
                }
            })

        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5005)
    }

    @Test
    fun `SearchType is WeeklyClickPackage error`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem().apply {
                biddingStrategy = TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.WEEKLY_CLICK_PACKAGE
                        weeklyClickPackage = StrategyWeeklyClickPackage().apply {
                            clicksPerWeek = 123
                            averageCpc = FACTORY.createStrategyWeeklyClickPackageAverageCpc(1_234_567L)
                        }
                    }
                    network = TextCampaignNetworkStrategy().apply {
                        biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                    }
                }
            })

        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5006)
    }

    @Test
    fun `NetworkType is WeeklyClickPackage error`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem().apply {
                biddingStrategy = TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.SERVING_OFF
                    }
                    network = TextCampaignNetworkStrategy().apply {
                        biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.WEEKLY_CLICK_PACKAGE
                        weeklyClickPackage = StrategyWeeklyClickPackage().apply {
                            clicksPerWeek = 123
                            averageCpc = FACTORY.createStrategyWeeklyClickPackageAverageCpc(1_234_567L)
                        }
                    }
                }
            })

        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5006)
    }

    @Test
    fun `TrackingParams are invalid error`() {
        val updateItem = CampaignUpdateItem()
            .withId(campaignId)
            .withTextCampaign(TextCampaignUpdateItem().apply {
                trackingParams = OBJECT_FACTORY.createTextCampaignUpdateItemTrackingParams("точно неверное значение")
            })
        val request = UpdateRequest()
            .withCampaigns(updateItem)

        val response = genericApiService.doAction(delegate, request)
        assertThat(response.updateResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(5005)
    }
}
