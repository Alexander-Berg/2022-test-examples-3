package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.nhaarman.mockitokotlin2.mock
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignGetItem
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignGetItem
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignGetItem
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignGetItem
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignGetItem
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.api.v5.entity.campaigns.container.CampaignAnyFieldEnum
import ru.yandex.direct.common.util.PropertyFilter

@RunWith(JUnitParamsRunner::class)
class CampaignGetConverterFilterPropertiesTest {

    private val converter = CampaignsGetResponseConverter(
        propertyFilter = PropertyFilter(),
        translationService = mock(),
        campaignStatusCalculator = mock(),
    )

    fun filterOutSingleField() = listOf(
        listOf(
            "filter out all fields except Id",
            listOf(createTextCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.ID),
            listOf(CampaignGetItem().withId(ID)),
        ),

        listOf(
            "filter out all fields except Name",
            listOf(createTextCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.NAME),
            listOf(CampaignGetItem().withName(NAME)),
        ),

        listOf(
            "filter out all fields except CpmBannerCampaign.BiddingStrategy",
            listOf(createCpmBannerCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.CPM_BANNER_CAMPAIGN_BIDDING_STRATEGY),
            listOf(
                CampaignGetItem().apply {
                    cpmBannerCampaign = CpmBannerCampaignGetItem().apply {
                        biddingStrategy = CPM_BANNER_CAMPAIGN_BIDDING_STRATEGY
                    }
                }
            ),
        ),

        listOf(
            "filter out all fields except DynamicTextCampaign.BiddingStrategy",
            listOf(createDynamicTextCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.DYNAMIC_TEXT_CAMPAIGN_BIDDING_STRATEGY),
            listOf(
                CampaignGetItem().apply {
                    dynamicTextCampaign = DynamicTextCampaignGetItem().apply {
                        biddingStrategy = DYNAMIC_TEXT_CAMPAIGN_BIDDING_STRATEGY
                    }
                }
            ),
        ),

        listOf(
            "filter out all fields except MobileAppCampaign.BiddingStrategy",
            listOf(createMobileAppCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.MOBILE_APP_CAMPAIGN_BIDDING_STRATEGY),
            listOf(
                CampaignGetItem().apply {
                    mobileAppCampaign = MobileAppCampaignGetItem().apply {
                        biddingStrategy = MOBILE_APP_CAMPAIGN_BIDDING_STRATEGY
                    }
                }
            ),
        ),

        listOf(
            "filter out all fields except SmartCampaign.BiddingStrategy",
            listOf(createSmartCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.SMART_CAMPAIGN_BIDDING_STRATEGY),
            listOf(
                CampaignGetItem().apply {
                    smartCampaign = SmartCampaignGetItem().apply {
                        biddingStrategy = SMART_CAMPAIGN_BIDDING_STRATEGY
                    }
                }
            ),
        ),

        listOf(
            "filter out all fields except TextCampaign.BiddingStrategy",
            listOf(createTextCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.TEXT_CAMPAIGN_BIDDING_STRATEGY),
            listOf(
                CampaignGetItem().apply {
                    textCampaign = TextCampaignGetItem().apply {
                        biddingStrategy = TEXT_CAMPAIGN_BIDDING_STRATEGY
                    }
                }
            ),
        ),
    )

    fun filterOutMultipleFields() = listOf(
        listOf(
            "filter out all fields except Id and Name",
            listOf(createTextCampaignGetItem()),
            setOf(CampaignAnyFieldEnum.ID, CampaignAnyFieldEnum.NAME),
            listOf(CampaignGetItem().withId(ID).withName(NAME)),
        ),

        listOf(
            "filter out all fields except TextCampaign.BiddingStrategy and TextCampaign.Settings",
            listOf(createTextCampaignGetItem()),
            setOf(
                CampaignAnyFieldEnum.TEXT_CAMPAIGN_BIDDING_STRATEGY,
                CampaignAnyFieldEnum.TEXT_CAMPAIGN_SETTINGS,
            ),
            listOf(
                CampaignGetItem().apply {
                    textCampaign = TextCampaignGetItem().apply {
                        biddingStrategy = TEXT_CAMPAIGN_BIDDING_STRATEGY
                        settings = SETTINGS
                    }
                }
            ),
        ),
    )

    fun filterOutSingleFieldInMultipleCampaigns() = listOf(
        listOf(
            "filter out all fields except Id in multiple campaigns",
            listOf(
                createCpmBannerCampaignGetItem(),
                createDynamicTextCampaignGetItem(),
                createMobileAppCampaignGetItem(),
                createSmartCampaignGetItem(),
                createTextCampaignGetItem(),
            ),
            setOf(CampaignAnyFieldEnum.ID),
            List(5) { CampaignGetItem().withId(ID) }
        ),
    )

    @Test
    @TestCaseName("{0}")
    @Parameters(
        method = "filterOutSingleField,filterOutMultipleFields,filterOutSingleFieldInMultipleCampaigns",
    )
    fun `fields are filtered correctly`(
        @Suppress("UNUSED_PARAMETER") description: String,
        items: List<CampaignGetItem>,
        requestedFields: Set<CampaignAnyFieldEnum>,
        expectedItems: List<CampaignGetItem>,
    ) {
        converter.filterProperties(items, requestedFields)
        assertThat(items)
            .usingRecursiveComparison()
            .isEqualTo(expectedItems)
    }

    companion object TestData {
        val ID = 1L
        val NAME = "name"
        val SETTINGS = emptyList<Nothing>()

        val CPM_BANNER_CAMPAIGN_BIDDING_STRATEGY = CpmBannerCampaignStrategy().apply {
            search = CpmBannerCampaignSearchStrategy().apply {
                biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
            }
            network = CpmBannerCampaignNetworkStrategy().apply {
                biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.MANUAL_CPM
            }
        }

        val DYNAMIC_TEXT_CAMPAIGN_BIDDING_STRATEGY = DynamicTextCampaignStrategy().apply {
            search = DynamicTextCampaignSearchStrategy().apply {
                biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
            }
            network = DynamicTextCampaignNetworkStrategy().apply {
                biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val MOBILE_APP_CAMPAIGN_BIDDING_STRATEGY = MobileAppCampaignStrategy().apply {
            search = MobileAppCampaignSearchStrategy().apply {
                biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
            }
            network = MobileAppCampaignNetworkStrategy().apply {
                biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val SMART_CAMPAIGN_BIDDING_STRATEGY = SmartCampaignStrategy().apply {
            search = SmartCampaignSearchStrategy().apply {
                biddingStrategyType = SmartCampaignSearchStrategyTypeEnum.AVERAGE_CPC_PER_CAMPAIGN
            }
            network = SmartCampaignNetworkStrategy().apply {
                biddingStrategyType = SmartCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        val TEXT_CAMPAIGN_BIDDING_STRATEGY = TextCampaignStrategy().apply {
            search = TextCampaignSearchStrategy().apply {
                biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
            }
            network = TextCampaignNetworkStrategy().apply {
                biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
            }
        }

        private fun createCampaignGetItem() = CampaignGetItem().apply {
            id = ID
            name = NAME
        }

        fun createCpmBannerCampaignGetItem() = createCampaignGetItem().apply {
            cpmBannerCampaign = CpmBannerCampaignGetItem().apply {
                biddingStrategy = CPM_BANNER_CAMPAIGN_BIDDING_STRATEGY
                settings = SETTINGS
            }
        }

        fun createDynamicTextCampaignGetItem() = createCampaignGetItem().apply {
            dynamicTextCampaign = DynamicTextCampaignGetItem().apply {
                biddingStrategy = DYNAMIC_TEXT_CAMPAIGN_BIDDING_STRATEGY
                settings = SETTINGS
            }
        }

        fun createMobileAppCampaignGetItem() = createCampaignGetItem().apply {
            mobileAppCampaign = MobileAppCampaignGetItem().apply {
                biddingStrategy = MOBILE_APP_CAMPAIGN_BIDDING_STRATEGY
                settings = SETTINGS
            }
        }

        fun createSmartCampaignGetItem() = createCampaignGetItem().apply {
            smartCampaign = SmartCampaignGetItem().apply {
                biddingStrategy = SMART_CAMPAIGN_BIDDING_STRATEGY
                settings = SETTINGS
            }
        }

        fun createTextCampaignGetItem() = createCampaignGetItem().apply {
            textCampaign = TextCampaignGetItem().apply {
                biddingStrategy = TEXT_CAMPAIGN_BIDDING_STRATEGY
                settings = SETTINGS
            }
        }
    }
}
