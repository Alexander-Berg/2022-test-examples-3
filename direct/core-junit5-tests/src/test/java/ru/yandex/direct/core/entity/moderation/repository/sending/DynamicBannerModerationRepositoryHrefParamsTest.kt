package ru.yandex.direct.core.entity.moderation.repository.sending

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.TestNewDynamicBanners
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@CoreTest
@ExtendWith(SpringExtension::class)
class DynamicBannerModerationRepositoryHrefParamsTest @Autowired constructor(
    private val steps: Steps,
    private val dynamicBannerModerationRepository: DynamicBannerModerationRepository,
    private val dslContextProvider: DslContextProvider
) {
    @Test
    fun `build banner href with campaign params`() {
        val campaignInfo = steps.dynamicCampaignSteps().createCampaign(
            TestDynamicCampaigns.fullDynamicCampaign()
                .withBannerHrefParams("utm_source=foo")
        )
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)
        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
            NewDynamicBannerInfo().withBanner(
                TestNewDynamicBanners.fullDynamicBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                    .withHref("https://yandex.ru")
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val bannerForModeration = dynamicBannerModerationRepository.loadObjectForModeration(
            listOf(bannerInfo.bannerId), dslContextProvider.ppc(bannerInfo.shard).configuration()
        )[0]

        Assertions.assertThat(bannerForModeration.href).isEqualTo("https://yandex.ru?utm_source=foo")
    }

    @Test
    fun `build banner href with group params`() {
        val campaignInfo = steps.dynamicCampaignSteps().createDefaultCampaign()
        val adGroupInfo = steps.adGroupSteps().createAdGroup(
            TestGroups.activeDynamicTextAdGroup(campaignInfo.campaignId)
                .withTrackingParams("utm_source=bar")
        )
        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
            NewDynamicBannerInfo().withBanner(
                TestNewDynamicBanners.fullDynamicBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                    .withHref("https://yandex.ru")
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val bannerForModeration = dynamicBannerModerationRepository.loadObjectForModeration(
            listOf(bannerInfo.bannerId), dslContextProvider.ppc(bannerInfo.shard).configuration()
        )[0]

        Assertions.assertThat(bannerForModeration.href).isEqualTo("https://yandex.ru?utm_source=bar")
    }
}
