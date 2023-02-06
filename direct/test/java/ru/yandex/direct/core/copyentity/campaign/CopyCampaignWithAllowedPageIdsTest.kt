package ru.yandex.direct.core.copyentity.campaign

import org.junit.Before
import org.junit.Test
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.feature.FeatureName

@CoreTest
class CopyCampaignWithAllowedPageIdsTest : BaseCopyCampaignTest() {

    val allowedSsp: List<String> = listOf("Smaato", "Google", "AdsNative", "Madgic")

    val allowedPageIds: List<Long> = listOf(
        139995, 231583, 259601, 259856, 259864, 260002, 260229, 260232,
        269694, 272256, 272769, 293635, 293686, 337887, 338031, 338036,
        486953, 539800, 553163, 555600, 587131, 648120, 683973, 700312,
        731413, 731419, 731422, 731426, 731543, 731546, 752673
    )

    val allowedDomains: List<String> = listOf("www.google.com", "www.yandex.ru", "www.lenta.ru")

    @Before
    fun before() {
        steps.sspPlatformsSteps().addSspPlatforms(allowedSsp)
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `campaign with allowed pages ids fields is not copied same client without feature`() {
        setAllowedPagesIdsFeature(listOf(client), false)

        val campaign = createCampaignWithAllowedPagesIdsFields()
        val copiedCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.allowedPageIds).isNull()
            assertThat(copiedCampaign.allowedDomains).isNull()
            assertThat(copiedCampaign.allowedSsp).isNull()
        }
    }

    @Test
    fun `campaign with allowed pages ids fields is copied same client with feature`() {
        setAllowedPagesIdsFeature(listOf(client), true)

        val campaign = createCampaignWithAllowedPagesIdsFields()
        val copiedCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.allowedPageIds).containsExactlyInAnyOrderElementsOf(allowedPageIds)
            assertThat(copiedCampaign.allowedDomains).containsExactlyInAnyOrderElementsOf(allowedDomains)
            assertThat(copiedCampaign.allowedSsp).containsExactlyInAnyOrderElementsOf(allowedSsp)
        }
    }

    @Test
    fun `campaign with allowed pages ids fields is not copied between clients without feature`() {
        setAllowedPagesIdsFeature(listOf(client, targetClient), false)

        val campaign = createCampaignWithAllowedPagesIdsFields()
        val copiedCampaign = copyValidCampaignBetweenClients(campaign = campaign, operatorUid = targetClient.uid)

        softly {
            assertThat(copiedCampaign.allowedPageIds).isNull()
            assertThat(copiedCampaign.allowedDomains).isNull()
            assertThat(copiedCampaign.allowedSsp).isNull()
        }
    }

    @Test
    fun `campaign with allowed pages ids is copied between clients with feature`() {
        setAllowedPagesIdsFeature(listOf(client, targetClient), true)

        val campaign = createCampaignWithAllowedPagesIdsFields()
        val copiedCampaign = copyValidCampaignBetweenClients(campaign = campaign, operatorUid = targetClient.uid)

        softly {
            assertThat(copiedCampaign.allowedPageIds).containsExactlyInAnyOrderElementsOf(allowedPageIds)
            assertThat(copiedCampaign.allowedDomains).containsExactlyInAnyOrderElementsOf(allowedDomains)
            assertThat(copiedCampaign.allowedSsp).containsExactlyInAnyOrderElementsOf(allowedSsp)
        }
    }

    private fun setAllowedPagesIdsFeature(clients: List<ClientInfo>, enabled: Boolean) =
        clients.forEach {
            steps.featureSteps()
                .addClientFeature(it.clientId, FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, enabled)
        }

    private fun createCampaignWithAllowedPagesIdsFields(): TextCampaignInfo = steps.textCampaignSteps().createCampaign(
        client,
        TestTextCampaigns.fullTextCampaign()
            .withAllowedDomains(allowedDomains)
            .withAllowedSsp(allowedSsp)
            .withAllowedPageIds(allowedPageIds)
    )

}
