package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.feature.FeatureName

@CoreTest
class CopyCampaignWithDisallowedPageIdsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps()
            .addClientFeature(client.clientId, FeatureName.SET_CAMPAIGN_DISALLOWED_PAGE_IDS, true)
    }

    @Test
    fun copyCpmCampaignWithDisallowedPageIds() {
        val disallowedPageIds: List<Long> = listOf(
            139995, 231583, 259601, 259856, 259864, 260002, 260229, 260232,
            269694, 272256, 272769, 293635, 293686, 337887, 338031, 338036,
            486953, 539800, 553163, 555600, 587131, 648120, 683973, 700312,
            731413, 731419, 731422, 731426, 731543, 731546, 752673
        )
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withDisallowedPageIds(disallowedPageIds)
        )
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.disallowedPageIds).containsExactlyInAnyOrderElementsOf(disallowedPageIds)
    }

}
