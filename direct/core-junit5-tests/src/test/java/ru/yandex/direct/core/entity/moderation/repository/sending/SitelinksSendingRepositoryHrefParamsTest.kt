package ru.yandex.direct.core.entity.moderation.repository.sending

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.TestNewDynamicBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.info.SitelinkSetInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@CoreTest
@ExtendWith(SpringExtension::class)
class SitelinksSendingRepositoryHrefParamsTest @Autowired constructor(
    private val steps: Steps,
    private val sitelinksSendingRepository: SitelinksSendingRepository,
    private val dslContextProvider: DslContextProvider
) {
    @Test
    fun `build sitelink href with campaign params`() {
        val campaignInfo = steps.textCampaignSteps().createCampaign(
            TestTextCampaigns.fullTextCampaign()
                .withBannerHrefParams("utm_source=foo")
        )
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)
        val sitelinkSetInfo = steps.sitelinkSetSteps().createSitelinkSet(
            SitelinkSetInfo().withSitelinkSet(
                SitelinkSet().withSitelinks(
                    listOf(Sitelink()
                        .withTitle("yandex")
                        .withHref("https://yandex.ru")
                    )
                )
            ).withClientInfo(campaignInfo.clientInfo)
        )
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(
                TestNewTextBanners.fullTextBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                    .withSitelinksSetId(sitelinkSetInfo.sitelinkSetId)
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val sitelinkForModeration = sitelinksSendingRepository.loadObjectForModeration(
            listOf(bannerInfo.bannerId), dslContextProvider.ppc(bannerInfo.shard).configuration()
        )[0].sitelinks[0]

        Assertions.assertThat(sitelinkForModeration.href).isEqualTo("https://yandex.ru?utm_source=foo")
    }

    @Test
    fun `build sitelink href with group params`() {
        val campaignInfo = steps.dynamicCampaignSteps().createDefaultCampaign()
        val adGroupInfo = steps.adGroupSteps().createAdGroup(
            TestGroups.activeDynamicTextAdGroup(campaignInfo.campaignId)
                .withTrackingParams("utm_source=bar")
        )
        val sitelinkSetInfo = steps.sitelinkSetSteps().createSitelinkSet(
            SitelinkSetInfo().withSitelinkSet(
                SitelinkSet().withSitelinks(
                    listOf(Sitelink()
                        .withTitle("yandex")
                        .withHref("https://yandex.ru")
                    )
                )
            ).withClientInfo(campaignInfo.clientInfo)
        )
        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
            NewDynamicBannerInfo().withBanner(
                TestNewDynamicBanners.fullDynamicBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                    .withSitelinksSetId(sitelinkSetInfo.sitelinkSetId)
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val sitelinkForModeration = sitelinksSendingRepository.loadObjectForModeration(
            listOf(bannerInfo.bannerId), dslContextProvider.ppc(bannerInfo.shard).configuration()
        )[0].sitelinks[0]

        Assertions.assertThat(sitelinkForModeration.href).isEqualTo("https://yandex.ru?utm_source=bar")
    }
}
