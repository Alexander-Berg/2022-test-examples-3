package ru.yandex.direct.api.v5.entity.campaignsext.converter

import com.yandex.direct.api.v5.campaignsext.CampaignAddItem
import com.yandex.direct.api.v5.campaignsext.DailyBudget
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum
import com.yandex.direct.api.v5.campaignsext.EmailSettings
import com.yandex.direct.api.v5.campaignsext.Notification
import com.yandex.direct.api.v5.campaignsext.ObjectFactory
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsArray
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsItem
import com.yandex.direct.api.v5.campaignsext.RelevantKeywordsModeEnum
import com.yandex.direct.api.v5.campaignsext.RelevantKeywordsSettingAdd
import com.yandex.direct.api.v5.campaignsext.SmsEventsEnum
import com.yandex.direct.api.v5.campaignsext.SmsSettings
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCpaAdd
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCpcAdd
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCrrAdd
import com.yandex.direct.api.v5.campaignsext.StrategyAverageRoiAdd
import com.yandex.direct.api.v5.campaignsext.StrategyMaximumClicksAdd
import com.yandex.direct.api.v5.campaignsext.StrategyMaximumConversionRateAdd
import com.yandex.direct.api.v5.campaignsext.StrategyNetworkDefaultAdd
import com.yandex.direct.api.v5.campaignsext.StrategyPayForConversionAdd
import com.yandex.direct.api.v5.campaignsext.StrategyPayForConversionCrrAdd
import com.yandex.direct.api.v5.campaignsext.StrategyWeeklyClickPackageAdd
import com.yandex.direct.api.v5.campaignsext.TextCampaignAddItem
import com.yandex.direct.api.v5.campaignsext.TextCampaignNetworkStrategyAdd
import com.yandex.direct.api.v5.campaignsext.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.TextCampaignSearchStrategyAdd
import com.yandex.direct.api.v5.campaignsext.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.TextCampaignSetting
import com.yandex.direct.api.v5.campaignsext.TextCampaignSettingsEnum
import com.yandex.direct.api.v5.campaignsext.TextCampaignStrategyAdd
import com.yandex.direct.api.v5.campaignsext.TimeTargetingAdd
import com.yandex.direct.api.v5.general.ArrayOfInteger
import com.yandex.direct.api.v5.general.ArrayOfString
import com.yandex.direct.api.v5.general.AttributionModelEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import java.time.ZoneId
import javax.xml.bind.JAXBElement
import kotlin.random.Random

@Api5Test
@RunWith(SpringRunner::class)
class CampaignsExtToCampaignsConversionsTest {

    @Autowired
    private lateinit var campaignsExtToCampaignsConverter: CampaignExtToCampaignConverter

    @Test
    fun `CampaignAddItem without campaigns is converted correctly`() {
        val addItem = CampaignAddItem().apply {
            clientInfo = "client info"
            notification = Notification().apply {
                smsSettings = SmsSettings().apply {
                    events = listOf(SmsEventsEnum.FINISHED)
                    timeFrom = "1970-01-01"
                    timeTo = "2012-12-21"
                }
                emailSettings = EmailSettings().apply {
                    email = "email@ya.ru"
                    checkPositionInterval = 100
                    warningBalance = 1000
                    sendAccountNews = YesNoEnum.YES
                    sendWarnings = YesNoEnum.YES
                }
            }
            timeZone = ZoneId.of("Europe/Amsterdam").id
            name = "Test name"
            startDate = "1970-01-01"
            dailyBudget = DailyBudget().apply {
                amount = 10
                mode = DailyBudgetModeEnum.STANDARD
            }
            endDate = "2012-12-21"
            negativeKeywords = arrayOfString("negative", "keywords")
            blockedIps = arrayOfString("127.0.0.1")
            excludedSites = arrayOfString("google.com")
            timeTargeting = TimeTargetingAdd().apply {
                schedule = arrayOfString(*TIME_TARGETING_SCHEDULE.toTypedArray())
                considerWorkingWeekends = YesNoEnum.YES
            }
        }

        assertEqualsFieldByField(addItem, campaignsExtToCampaignsConverter.convertAddItem(addItem))
    }

    @Test
    fun `TextCampaignAddItem is converted correctly`() {
        val addItem = TextCampaignAddItem().apply {
            biddingStrategy = TextCampaignStrategyAdd().apply {
                search = TextCampaignSearchStrategyAdd().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                    wbMaximumClicks = StrategyMaximumClicksAdd().apply {
                        weeklySpendLimit = 123
                        bidCeiling = 123
                    }
                    wbMaximumConversionRate = StrategyMaximumConversionRateAdd().apply {
                        weeklySpendLimit = 123
                        bidCeiling = 123
                    }
                    averageCpc = StrategyAverageCpcAdd().apply {
                        averageCpc = 123
                        weeklySpendLimit = 123
                    }
                    averageCpa = null
                    payForConversion = StrategyPayForConversionAdd().apply {
                        cpa = 123
                        goalId = 123
                        weeklySpendLimit = 123
                    }
                    weeklyClickPackage = StrategyWeeklyClickPackageAdd().apply {
                        clicksPerWeek = 123
                        averageCpc = 123
                        bidCeiling = 123
                    }
                    averageRoi = StrategyAverageRoiAdd().apply {
                        reserveReturn = 123
                        roiCoef = 123
                        goalId = 123
                        weeklySpendLimit = 123
                        bidCeiling = 123
                        profitability = 123
                    }
                    averageCrr = null
                    payForConversionCrr = null
                }
                network = TextCampaignNetworkStrategyAdd().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                    networkDefault = StrategyNetworkDefaultAdd().apply {
                        limitPercent = 50
                    }
                    wbMaximumClicks = null
                    wbMaximumConversionRate = null
                    averageCpc = null
                    averageCpa = StrategyAverageCpaAdd().apply {
                        averageCpa = 123
                        goalId = 123
                        weeklySpendLimit = 123
                        bidCeiling = 123
                    }
                    payForConversion = null
                    weeklyClickPackage = null
                    averageRoi = null
                    averageCrr = StrategyAverageCrrAdd().apply {
                        crr = 123
                        goalId = 123
                        weeklySpendLimit = 123
                    }
                    payForConversionCrr = StrategyPayForConversionCrrAdd().apply {
                        crr = 123
                        goalId = 123
                        weeklySpendLimit = 123
                    }
                }
            }
            settings = listOf(
                TextCampaignSetting().apply {
                    option = TextCampaignSettingsEnum.ADD_METRICA_TAG
                    value = YesNoEnum.YES
                },
                TextCampaignSetting().apply {
                    option = TextCampaignSettingsEnum.ADD_TO_FAVORITES
                    value = YesNoEnum.NO
                }
            )
            counterIds = ArrayOfInteger().withItems(1, 2, 3)
            relevantKeywords = RelevantKeywordsSettingAdd().apply {
                budgetPercent = 50
                mode = RelevantKeywordsModeEnum.MAXIMUM
                optimizeGoalId = FACTORY.createRelevantKeywordsSettingAddOptimizeGoalId(12)
            }
            priorityGoals = PriorityGoalsArray().apply {
                items = listOf(
                    PriorityGoalsItem().apply {
                        goalId = 1234
                        value = 123
                    }
                )
            }
            attributionModel = AttributionModelEnum.FC
            strategyId = 4567
        }

        val campaignsAddItem = campaignsExtToCampaignsConverter.convertAddItem(addItem)
        assertEqualsFieldByField(addItem, campaignsAddItem)
    }

    @Test
    fun `nil JAXBElement is converted correctly`() {
        val addItem = TextCampaignAddItem().apply {
            relevantKeywords = RelevantKeywordsSettingAdd().apply {
                budgetPercent = 50
                mode = RelevantKeywordsModeEnum.MAXIMUM
                optimizeGoalId = FACTORY.createRelevantKeywordsSettingAddOptimizeGoalId(null)
            }
        }

        val campaignsAddItem = campaignsExtToCampaignsConverter.convertAddItem(addItem)
        assertThat(campaignsAddItem)
            .extracting("relevantKeywords.optimizeGoalId.nil")
            .isEqualTo(true)
    }

    companion object {
        private val TIME_TARGETING_SCHEDULE = List(7) {
            List(24) { Random.nextInt(0, 100) }
                .joinToString(separator = ",")
        }

        private val FACTORY = ObjectFactory()

        private fun arrayOfString(vararg items: String) = ArrayOfString().withItems(*items)

        private fun assertEqualsFieldByField(original: Any, converted: Any) {
            val enumComparator = compareBy(Enum<*>::name)
            val jaxbElementComparator: Comparator<JAXBElement<*>> = Comparator { a, b ->
                when {
                    a?.value != b?.value -> 1
                    else -> 0
                }
            }
            assertThat(converted)
                .usingRecursiveComparison()
                .withComparatorForType(nullsFirst(enumComparator), Enum::class.java)
                .withComparatorForType(nullsFirst(jaxbElementComparator), JAXBElement::class.java)
                .isEqualTo(original)
        }
    }
}
