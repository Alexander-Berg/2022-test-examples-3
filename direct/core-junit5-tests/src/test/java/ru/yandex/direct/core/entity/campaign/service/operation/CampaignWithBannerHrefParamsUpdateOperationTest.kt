package ru.yandex.direct.core.entity.campaign.service.operation

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.model.ButtonAction
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBannerHrefParams
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewDynamicBanners
import ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.info.SitelinkSetInfo
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.randomPositiveInt

@CoreTest
@ExtendWith(SpringExtension::class)
class CampaignWithBannerHrefParamsUpdateOperationTest @Autowired constructor(
    private val steps: Steps,
    private val metrikaClientStub: MetrikaClientStub,
    private val campaignOperationService: CampaignOperationService,
    private val campaignTypedRepository: CampaignTypedRepository,
    private val adGroupRepository: AdGroupRepository,
    private val bannerTypedRepository: BannerTypedRepository
) {
    @Test
    fun `reset bs synced status on text campaign`() {
        val campaignInfo = steps.textCampaignSteps().createCampaign(
            TestTextCampaigns.fullTextCampaign()
                .withStatusBsSynced(CampaignStatusBsSynced.YES)
        )

        val result = updateCampaignHrefParams(campaignInfo.campaignId, campaignInfo.uid, campaignInfo.clientId)

        val campaign = campaignTypedRepository
            .getStrictly(campaignInfo.shard, listOf(campaignInfo.campaignId), TextCampaign::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(campaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
        }
    }

    @Test
    fun `reset bs synced status on dynamic campaign`() {
        val campaignInfo = steps.dynamicCampaignSteps().createCampaign(
            TestDynamicCampaigns.fullDynamicCampaign()
                .withStatusBsSynced(CampaignStatusBsSynced.YES)
        )

        val result = updateCampaignHrefParams(campaignInfo.campaignId, campaignInfo.uid, campaignInfo.clientId)

        val campaign = campaignTypedRepository
            .getStrictly(campaignInfo.shard, listOf(campaignInfo.campaignId), DynamicCampaign::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(campaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
        }
    }

    @Test
    fun `reset bs synced status on smart campaign`() {
        val campaignInfo = defaultSmartCampaign()

        val result = updateCampaignHrefParams(campaignInfo.campaignId, campaignInfo.uid, campaignInfo.clientId)

        val campaign = campaignTypedRepository
            .getStrictly(campaignInfo.shard, listOf(campaignInfo.campaignId), SmartCampaign::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(campaign.statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
        }
    }

    @Test
    fun `not reset moderation status on new text banner`() {
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(
                TestNewTextBanners.fullTextBanner()
                    .withStatusModerate(BannerStatusModerate.NEW)
            )
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), TextBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.NEW)
        }
    }

    @Test
    fun `not reset moderation status on active text banner if no href`() {
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(
                TestNewTextBanners.fullTextBanner()
                    .withHref(null)
                    .withStatusModerate(BannerStatusModerate.YES)
            )
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), TextBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
        }
    }

    @Test
    fun `reset banner statuses on active text banner`() {
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(defaultModeratedTextBanner())
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), TextBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.READY)
            it.assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.NO)
            it.assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
        }
    }

    @Test
    fun `reset sitelink moderation status on active text banner`() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val sitelinkSetInfo = steps.sitelinkSetSteps().createSitelinkSet(
            SitelinkSetInfo()
                .withSitelinkSet(defaultSitelinkSet())
                .withClientInfo(clientInfo)
        )
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo()
                .withBanner(
                    defaultModeratedTextBanner()
                        .withSitelinksSetId(sitelinkSetInfo.sitelinkSetId)
                        .withStatusSitelinksModerate(BannerStatusSitelinksModerate.YES)
                )
                .withClientInfo(clientInfo)
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), TextBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusSitelinksModerate).isEqualTo(BannerStatusSitelinksModerate.READY)
        }
    }

    @Test
    fun `reset button moderation status on active text banner`() {
        val bannerInfo = steps.textBannerSteps().createBanner(
            NewTextBannerInfo().withBanner(
                defaultModeratedTextBanner()
                    .withButtonAction(ButtonAction.BUY)
                    .withButtonCaption("Buy")
                    .withButtonHref("https://yandex.ru")
                    .withButtonStatusModerate(BannerButtonStatusModerate.YES)
            )
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), TextBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.buttonStatusModerate).isEqualTo(BannerButtonStatusModerate.READY)
        }
    }

    @Test
    fun `reset sitelink moderation status on active dynamic banner`() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val sitelinkSetInfo = steps.sitelinkSetSteps().createSitelinkSet(
            SitelinkSetInfo()
                .withSitelinkSet(defaultSitelinkSet())
                .withClientInfo(clientInfo)
        )
        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
            NewDynamicBannerInfo()
                .withBanner(
                    TestNewDynamicBanners.fullDynamicBanner()
                        .withHref(null)
                        .withSitelinksSetId(sitelinkSetInfo.sitelinkSetId)
                        .withStatusSitelinksModerate(BannerStatusSitelinksModerate.YES)
                )
                .withClientInfo(clientInfo)
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), DynamicBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusSitelinksModerate).isEqualTo(BannerStatusSitelinksModerate.READY)
        }
    }

    @Test
    fun `reset bs synced status on active dynamic banner`() {
        val bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(
            NewDynamicBannerInfo().withBanner(
                TestNewDynamicBanners.fullDynamicBanner()
                    .withStatusBsSynced(StatusBsSynced.YES)
            )
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), DynamicBanner::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
        }
    }

    @Test
    fun `reset bs synced status on active dynamic group`() {
        val adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup()

        val result = updateCampaignHrefParams(adGroupInfo.campaignId, adGroupInfo.uid, adGroupInfo.clientId)

        val adGroup = adGroupRepository
            .getAdGroups(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(adGroup.statusBsSynced).isEqualTo(StatusBsSynced.NO)
        }
    }

    @Test
    fun `reset bs synced status on active smart banner`() {
        val campaignInfo = defaultSmartCampaign()
        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaignInfo)
        val bannerInfo = steps.performanceMainBannerSteps().createPerformanceMainBanner(
            NewPerformanceMainBannerInfo().withBanner(
                TestNewPerformanceMainBanners.fullPerformanceMainBanner()
                    .withStatusBsSynced(StatusBsSynced.YES)
            ).withAdGroupInfo(adGroupInfo).withCampaignInfo(campaignInfo)
        )

        val result = updateCampaignHrefParams(bannerInfo.campaignId, bannerInfo.uid, bannerInfo.clientId)

        val banner = bannerTypedRepository
            .getStrictly(bannerInfo.shard, listOf(bannerInfo.bannerId), PerformanceBannerMain::class.java)[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
        }
    }

    @Test
    fun `reset bs synced status on active smart group`() {
        val campaignInfo = defaultSmartCampaign()
        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaignInfo)

        val result = updateCampaignHrefParams(adGroupInfo.campaignId, adGroupInfo.uid, adGroupInfo.clientId)

        val adGroup = adGroupRepository
            .getAdGroups(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[0]

        SoftAssertions.assertSoftly {
            it.assertThat(result.validationResult.hasAnyErrors()).isFalse
            it.assertThat(adGroup.statusBsSynced).isEqualTo(StatusBsSynced.NO)
        }
    }

    private fun updateCampaignHrefParams(cid: Long, uid: Long, clientId: ClientId): MassResult<Long> {
        val modelChanges = ModelChanges(cid, CampaignWithBannerHrefParams::class.java)
            .process("utm_source=foo", CampaignWithBannerHrefParams.BANNER_HREF_PARAMS)
        return campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(modelChanges), uid, UidAndClientId.of(uid, clientId), CampaignOptions()
        ).apply()
    }

    private fun defaultSmartCampaign(): SmartCampaignInfo {
        val counterId = randomPositiveInt()
        val campaignInfo = steps.smartCampaignSteps().createCampaign(
            TestSmartCampaigns.fullSmartCampaign()
                .withMetrikaCounters(listOf(counterId.toLong()))
                .withStatusBsSynced(CampaignStatusBsSynced.YES)
        )
        metrikaClientStub.addUserCounter(campaignInfo.uid, counterId)
        return campaignInfo
    }

    private fun defaultModeratedTextBanner() = TestNewTextBanners.fullTextBanner()
        .withStatusModerate(BannerStatusModerate.YES)
        .withStatusPostModerate(BannerStatusPostModerate.YES)
        .withStatusBsSynced(StatusBsSynced.YES)

    private fun defaultSitelinkSet() = SitelinkSet()
        .withSitelinks(listOf(Sitelink().withTitle("yandex").withHref("https://yandex.ru")))
}
