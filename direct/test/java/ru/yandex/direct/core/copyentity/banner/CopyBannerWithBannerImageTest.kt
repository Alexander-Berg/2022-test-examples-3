package ru.yandex.direct.core.copyentity.banner

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.banner.model.BannerImageOpts
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestBanners.smallImageFormat
import ru.yandex.direct.core.testing.info.BannerImageFormatInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyBannerWithBannerImageTest : BaseCopyBannerTest() {

    @Autowired
    private lateinit var testBannerImageRepository: TestBannerImageRepository

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copy banner with small image`() {
        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withClientInfo(client)
            .withBannerImageFormatInfo(BannerImageFormatInfo()
                .withBannerImageFormat(smallImageFormat(null))))

        val copiedBanner: TextBanner = copyValidBanner(banner)

        assertThat(copiedBanner.imageHash).isEqualTo(banner.bannerImageFormatInfo.imageHash)
    }

    fun checkOpts() = arrayOf(
        setOf(BannerImageOpts.SINGLE_AD_TO_BS),
        emptySet(),
        null,
    )

    @Test
    @Parameters(method = "checkOpts")
    fun `copy banner with opts`(
        bannerImageOpts: Set<BannerImageOpts>?,
    ) {
        val banner = steps.textBannerSteps().createBanner(
            NewTextBannerInfo()
                .withClientInfo(client)
                .withBannerImageFormatInfo(
                    BannerImageFormatInfo()
                        .withBannerImageFormat(smallImageFormat(null))
                )
        )
        testBannerImageRepository.updateImageOpts(banner.shard, banner.bannerId, bannerImageOpts)

        val copiedBanner: TextBanner = copyValidBanner(banner)

        assertThat(copiedBanner.opts).isEqualTo(bannerImageOpts)
    }
}
