package ru.yandex.direct.core.copyentity.banner

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.banner.model.BannerWithFixedBody
import ru.yandex.direct.core.entity.banner.model.BannerWithFixedTitle
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.McBanner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.NewMcBannerInfo

@CoreTest
class CopyBannerWithFixedBodyAndTitleTest : BaseCopyBannerTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copy banner with fixed title`() {
        val adGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(client)
        val banner = steps.dynamicBannerSteps().createDynamicBanner(adGroup)
        assertThat(banner.getBanner<BannerWithFixedTitle>().title).isNotNull

        val copiedBanner: DynamicBanner = copyValidBanner(banner)

        assertThat(copiedBanner.title).isNotNull
    }

    @Test
    fun `copy banner with fixed body`() {
        val adGroup = steps.adGroupSteps().createActiveMcBannerAdGroup(client)
        val banner = steps.mcBannerSteps().createMcBanner(NewMcBannerInfo()
            .withAdGroupInfo(adGroup))
        assertThat(banner.getBanner<BannerWithFixedBody>().body).isNotNull

        val copiedBanner: McBanner = copyValidBanner(banner)

        assertThat(copiedBanner.body).isNotNull
    }

}
