package ru.yandex.direct.core.copyentity.banner

import org.junit.Before
import org.junit.Test
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.info.NewTextBannerInfo

@CoreTest
class CopyBannerDomainTest : BaseCopyBannerTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyBannerWithDifferentHrefAndDomain() {
        val href = "https://ad.adriver.ru/cgi-bin/click.cgi?sid=1&bt=55&ad=720238&pid=3208045&bid=7218521&bn=7218521"
        val domain = "www.renault.ru"
        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withClientInfo(client)
            .withBanner(
                TestNewTextBanners.fullTextBanner()
                    .withHref(href)
                    .withDomain(domain)))

        val copiedBanner: TextBanner = copyValidBanner(banner)

        softly {
            assertThat(copiedBanner.href).isEqualTo(href)
            assertThat(copiedBanner.domain).isEqualTo(domain)
        }
    }
}
