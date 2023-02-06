package ru.yandex.direct.core.copyentity.banner

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assumeCopyResultIsSuccessful
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.entity.banner.type.creative.model.CreativeSize
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5
import ru.yandex.direct.core.testing.data.TestNewCpmBanners
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyBannerWithCreativeSizesTest : BaseCopyBannerTest() {
    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    fun invalidCreativeSizes() = listOf(
        CreativeSize(728, 90),
        CreativeSize(729, 90),
        CreativeSize(1457, 180),
    )

    fun validCreativeSizes() = listOf(
        CreativeSize(640, 134),
        CreativeSize(1456, 180),
    )

    fun allCreativeSizes() = validCreativeSizes() + invalidCreativeSizes()

    @Test
    @Parameters(method = "allCreativeSizes")
    fun copyBannerWithCreativeSizes(creativeSize: CreativeSize) {
        val campaign = steps.cpmYndxFrontPageSteps().createCampaign(
            client,
            TestCpmYndxFrontPageCampaigns.fullCpmYndxFrontpageCampaign()
        )

        val adGroup = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaign)

        val creative = defaultHtml5(client.clientId, null)
            .withWidth(creativeSize.width)
            .withHeight(creativeSize.height)

        val banner = steps.cpmBannerSteps().createCpmBanner(
            NewCpmBannerInfo()
                .withBanner(
                    TestNewCpmBanners.fullCpmBanner(null)
                )
                .withAdGroupInfo(adGroup)
                .withCreative(creative)
        )

        val result: CopyResult<*> = sameAdGroupBannerCopyOperation(banner).copy()
        assumeCopyResultIsSuccessful(result)
    }
}
