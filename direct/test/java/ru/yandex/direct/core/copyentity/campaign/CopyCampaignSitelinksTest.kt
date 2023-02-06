package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.info.NewTextBannerInfo

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignSitelinksTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    /**
     * Набор сайтлинков не должен копироваться при копировании внутри клиента.
     * В частности, при копировании не должно быть варнингов про дублирущиеся наборы сайтлинков
     */
    @Test
    fun testCopyCampaignSameSitelinkSet() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(client)
        val sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(client)
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withBanner(fullTextBanner()
                .withSitelinksSetId(sitelinkSet.sitelinkSetId))
            .withCampaignInfo(campaign))

        val copyOperation = sameClientCampaignCopyOperation(campaign)
        val campaignId = copyValidCampaigns(copyOperation).first()

        val copiedBanner = bannerTypedRepository.getBannersByCampaignIdsAndClass(
            client.shard,
            listOf(campaignId),
            TextBanner::class.java
        ).first()

        assertThat(copiedBanner.sitelinksSetId).isEqualTo(sitelinkSet.sitelinkSetId)
    }

}
