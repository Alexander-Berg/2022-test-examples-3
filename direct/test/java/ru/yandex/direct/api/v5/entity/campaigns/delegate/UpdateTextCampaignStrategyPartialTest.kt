package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignUpdateItem
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicks
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignUpdateItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import java.math.BigDecimal

@Api5Test
@RunWith(Parameterized::class)
class UpdateTextCampaignStrategyPartialTest(
    private val testName: String,
    private val initialStrategy: DbStrategy,
    private val updateStrategy: TextCampaignStrategy,
    private val expectedStrategy: DbStrategy
) : UpdateCampaignsDelegateBaseTest() {

    @Rule
    @JvmField
    val methodRule = SpringMethodRule()

    companion object {

        private val FACTORY = com.yandex.direct.api.v5.campaigns.ObjectFactory()
        private val oldSum = BigDecimal.valueOf(300)
        private val oldBid = BigDecimal.valueOf(7)
        private val newBid = BigDecimal.valueOf(8)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "BidCeiling от пользователя, WeeklySpendLimit из базы",

                DbStrategy().apply {
                    autobudget = CampaignsAutobudget.YES
                    platform = CampaignsPlatform.SEARCH
                    strategy = null
                    strategyData = StrategyData().apply {
                        version = 1
                        name = StrategyName.AUTOBUDGET.name.lowercase()
                        sum = oldSum
                        bid = oldBid
                    }
                    strategyName = StrategyName.AUTOBUDGET
                },

                TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                        wbMaximumClicks = StrategyMaximumClicks().apply {
                            bidCeiling = FACTORY.createStrategyWeeklyBudgetBaseBidCeiling(convertToMicros(newBid))
                        }
                    }
                },

                DbStrategy().apply {
                    autobudget = CampaignsAutobudget.YES
                    platform = CampaignsPlatform.SEARCH
                    strategy = null
                    strategyData = StrategyData().apply {
                        version = 1
                        name = StrategyName.AUTOBUDGET.name.lowercase()
                        sum = oldSum
                        bid = newBid
                    }
                    strategyName = StrategyName.AUTOBUDGET
                }
            ),

            arrayOf(
                "BidCeiling от пользователя = null, WeeklySpendLimit из базы",

                DbStrategy().apply {
                    autobudget = CampaignsAutobudget.YES
                    platform = CampaignsPlatform.SEARCH
                    strategy = null
                    strategyData = StrategyData().apply {
                        version = 1
                        name = StrategyName.AUTOBUDGET.name.lowercase()
                        sum = oldSum
                        bid = oldBid
                    }
                    strategyName = StrategyName.AUTOBUDGET
                },

                TextCampaignStrategy().apply {
                    search = TextCampaignSearchStrategy().apply {
                        biddingStrategyType = TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                        wbMaximumClicks = StrategyMaximumClicks().apply {
                            bidCeiling = FACTORY.createStrategyWeeklyBudgetBaseBidCeiling(null)
                        }
                    }
                },

                DbStrategy().apply {
                    autobudget = CampaignsAutobudget.YES
                    platform = CampaignsPlatform.SEARCH
                    strategy = null
                    strategyData = StrategyData().apply {
                        version = 1
                        name = StrategyName.AUTOBUDGET.name.lowercase()
                        sum = oldSum
                    }
                    strategyName = StrategyName.AUTOBUDGET
                }
            ),


        )
    }

    @Test
    fun updateStrategyInTextCampaign() {
        val campaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).apply {
            strategy = initialStrategy
        }

        val textCampaignInfo = TextCampaignInfo()
            .withTypedCampaign(campaign)
            .withClientInfo(clientInfo) as TextCampaignInfo

        val campaignInfo = steps.textCampaignSteps().createCampaign(textCampaignInfo)

        val updateItem = CampaignUpdateItem().apply {
            id = campaignInfo.id
            textCampaign = TextCampaignUpdateItem().apply {
                biddingStrategy = updateStrategy
            }
        }

        val expectedCampaign = TextCampaign().apply {
            id = campaignInfo.id
            strategy = expectedStrategy
        }

        val actualCampaign = getUpdatedCampaign<TextCampaign>(updateItem)

        assertThat(actualCampaign)
            .usingRecursiveComparison()
            .ignoringExpectedNullFields()
            .isEqualTo(expectedCampaign)
    }
}
