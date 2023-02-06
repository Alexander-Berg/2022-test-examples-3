package ru.yandex.direct.core.copyentity.campaign

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.CopyValidationResultUtils
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository
import ru.yandex.direct.core.entity.banner.service.validation.BannerConstants
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@CoreTest
class CopyCampaignWithTooManyBannersTest : BaseCopyCampaignTest() {
    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Autowired
    private lateinit var bannerModifyRepository: BannerModifyRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    lateinit var campaign: CampaignInfo

    lateinit var firstGroupBanner: NewTextBannerInfo

    lateinit var secondGroupBanners: List<NewTextBannerInfo>

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()

        campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns.fullTextCampaign()
        )

        firstGroupBanner = steps.textBannerSteps().createBanner(
            NewTextBannerInfo()
                .withClientInfo(client)
                .withCampaignInfo(campaign)
                .withBanner(
                    TestNewTextBanners.fullTextBanner()
                )
        )

        val secondAdGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)

        secondGroupBanners = createBanners(secondAdGroup, BannerConstants.MAX_BANNERS_IN_ADGROUP + 1)
    }

    @Test
    fun `copy campaign with small adgroup and adgroup with too many banners`() {
        val result: CopyResult<*> = sameClientCampaignCopyOperation(campaign).copy()

        val newBannerId =
            result.entityContext.getCopyMappingByClass(BannerWithAdGroupId::class.java).get(firstGroupBanner.bannerId)

        val brokenBannerIds: Set<Long> =
            result.massResult.validationResult.flattenErrors()
                .map { it.value }
                .filterIsInstance<BannerWithAdGroupId>()
                .map { it.id }.toSet()

        softly {
            copyAssert.softlyAssertResultContainsAllDefects(
                result,
                listOf(BannerDefectIds.Num.MAX_BANNERS_IN_ADGROUP),
                this
            )
            assertThat(brokenBannerIds).containsExactlyInAnyOrderElementsOf(secondGroupBanners.map { it.bannerId })
            assertThat(newBannerId).isNotEqualTo(firstGroupBanner.bannerId)
        }
    }

    @Test
    fun `CopyValidationResultUtils logs only warnings on copy adgroup with too many banners`() {
        val result: CopyResult<*> = sameClientCampaignCopyOperation(campaign).copy()
        val warningsAndErrors = CopyValidationResultUtils.getFailedResults(result.massResult.validationResult)
        val warnings = warningsAndErrors[0]
        val errors = warningsAndErrors[1]
        softly {
            assertThat(warnings).hasSize(BannerConstants.MAX_BANNERS_IN_ADGROUP + 1)
            assertThat(errors).isEmpty()
        }
    }

    private fun createBanners(adGroupInfo: AdGroupInfo, bannersCount: Int): List<NewTextBannerInfo> {
        val bannerInfos: List<NewTextBannerInfo> = (0 until bannersCount)
            .map {
                NewTextBannerInfo()
                    .withAdGroupInfo(adGroupInfo)
                    .withBanner(
                        fullTextBanner()
                            .withAdGroupId(adGroupInfo.adGroupId)
                            .withCampaignId(adGroupInfo.campaignId)
                    )
            }
            .toList()
        val container = BannerRepositoryContainer(adGroupInfo.shard)
        bannerModifyRepository.add(
            dslContextProvider.ppc(adGroupInfo.shard),
            container,
            bannerInfos.map { it.getBanner() as BannerWithAdGroupId }
        )
        return bannerInfos
    }
}
