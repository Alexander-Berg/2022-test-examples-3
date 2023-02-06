package ru.yandex.direct.core.entity.campaign

import org.junit.Assert
import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignSource

class AvailableCampaignSourcesTest {
    @Test
    fun universalCampaigns() {
        for (source in CampaignSource.values()) {
            val result = AvailableCampaignSources.isSupportedInAPI5(source)

            if (result && !NOT_UNIVERSAL_CAMPAIGNS.contains(source)) {
                Assert.fail("CampaignSource $source is universal campaign?")
            }
        }
    }

    @Test
    fun supportedInAPI5Campaigns() {
        for (source in CampaignSource.values()) {
            val result = AvailableCampaignSources.isSupportedInAPI5(source)

            if (result && !SUPPORTED_IN_API5.contains(source)) {
                Assert.fail("CampaignSource $source supported in API5?")
            }
        }
    }

    companion object {
        private val SUPPORTED_IN_API5 = setOf(
            CampaignSource.API,
            CampaignSource.DC,
            CampaignSource.DIRECT,
            CampaignSource.EDA,
            CampaignSource.GEO,
            CampaignSource.USLUGI,
            CampaignSource.XLS)

        private val NOT_UNIVERSAL_CAMPAIGNS = setOf(
            CampaignSource.API,
            CampaignSource.DC,
            CampaignSource.DIRECT,
            CampaignSource.EDA,
            CampaignSource.GEO,
            CampaignSource.USLUGI,
            CampaignSource.XLS,
            CampaignSource.ZEN)
    }
}
