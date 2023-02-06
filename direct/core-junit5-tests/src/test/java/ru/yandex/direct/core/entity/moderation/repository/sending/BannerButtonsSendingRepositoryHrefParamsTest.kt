package ru.yandex.direct.core.entity.moderation.repository.sending

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate
import ru.yandex.direct.core.entity.banner.model.ButtonAction
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.adgroup.TestAdGroups
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@CoreTest
@ExtendWith(SpringExtension::class)
class BannerButtonsSendingRepositoryHrefParamsTest @Autowired constructor(
    private val steps: Steps,
    private val bannerButtonsSendingRepository: BannerButtonsSendingRepository,
    private val dslContextProvider: DslContextProvider
) {
    @Test
    fun `build button href with campaign params`() {
        val campaignInfo = steps.textCampaignSteps().createCampaign(
            TestTextCampaigns.fullTextCampaign()
                .withBannerHrefParams("utm_source=foo")
        )
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(
                TestNewTextBanners.fullTextBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                    .withButtonHref("https://yandex.ru")
                    .withButtonAction(ButtonAction.BUY)
                    .withButtonCaption("Buy")
                    .withButtonStatusModerate(BannerButtonStatusModerate.READY)
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val bannerForModeration = bannerButtonsSendingRepository.loadObjectForModeration(
            listOf(bannerInfo.bannerId), dslContextProvider.ppc(bannerInfo.shard).configuration()
        )[0]

        Assertions.assertThat(bannerForModeration.href).isEqualTo("https://yandex.ru?utm_source=foo")
    }
}
