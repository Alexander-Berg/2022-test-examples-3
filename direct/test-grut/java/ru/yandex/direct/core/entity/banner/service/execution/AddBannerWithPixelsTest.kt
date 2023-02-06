package ru.yandex.direct.core.entity.banner.service.execution

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl
import ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl
import ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AddBannerWithPixelsTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var grutContext: GrutContext

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = steps.userSteps().createDefaultUser()
    }

    @Test
    @Description("Сохранение баннера с пикселями")
    fun testAddBannerWithPixels() {
        val adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(userInfo.clientInfo)

        val creativeInfo = steps.creativeSteps()
            .addDefaultHtml5Creative(adGroupInfo.clientInfo, steps.creativeSteps().nextCreativeId)
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(adGroupInfo.clientInfo)

        val banner = fullCpmBanner(adGroupInfo.campaignId, adGroupInfo.adGroupId, creativeInfo.creativeId)
            .withPixels(listOf(adfoxPixelUrl(), yaAudiencePixelUrl()))

        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        softly {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.pixelsList).containsOnly(adfoxPixelUrl(), yaAudiencePixelUrl())
        }
    }

    @Test
    @Description("Сохранение баннера поддерживающего пикселями c null в pixels")
    fun testAddBannerWithPixels_withNullInPixels() {
        val adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(userInfo.clientInfo)

        val creativeInfo = steps.creativeSteps()
            .addDefaultHtml5Creative(adGroupInfo.clientInfo, steps.creativeSteps().nextCreativeId)
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(adGroupInfo.clientInfo)

        val banner = fullCpmBanner(adGroupInfo.campaignId, adGroupInfo.adGroupId, creativeInfo.creativeId)
            .withPixels(null)

        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        softly {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.pixelsList).isEmpty()
        }
    }
}
