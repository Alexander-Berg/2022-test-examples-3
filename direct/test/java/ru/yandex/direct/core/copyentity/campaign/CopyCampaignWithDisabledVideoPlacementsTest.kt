package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns

@CoreTest
class CopyCampaignWithDisabledVideoPlacementsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyCpmCampaignWithDisabledVideoPlacements() {
        val disabledVideoPlacements = listOf(
            "zen.yandex.ru", "sport.yandex.ru", "otzovik.com", "dom-knig.com", "rusprofile.ru",
            "russianfood.com", "mk.ru", "cosmo.ru", "ficbook.net", "drive2.ru", "maps.yandex.ru",
            "auto.ru", "fishki.net", "litmir.me", "pogoda.yandex.ru", "glavbukh.ru", "forumhouse.ru",
            "smart-lab.ru", "zachestnyibiznes.ru", "realty.yandex.ru", "anekdot.ru", "topwar.ru",
            "newsru.com"
        )
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withDisabledVideoPlacements(disabledVideoPlacements)
        )
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.disabledVideoPlacements).containsExactlyInAnyOrderElementsOf(disabledVideoPlacements)
    }

}
