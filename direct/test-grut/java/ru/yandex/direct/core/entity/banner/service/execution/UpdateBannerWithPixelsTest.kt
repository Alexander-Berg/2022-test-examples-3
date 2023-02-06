package ru.yandex.direct.core.entity.banner.service.execution

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.CpmBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl
import ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl2
import ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerWithPixelsTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var bannersUpdateOperationFactory: BannersUpdateOperationFactory

    @Autowired
    private lateinit var grutContext: GrutContext

    private lateinit var userInfo: UserInfo
    private var bannerId: BannerId = 0

    @Before
    fun before() {
        userInfo = steps.userSteps().createDefaultUser()
        bannerId = createCpmBannerWithOnlyAdFoxPixelUrl()
    }

    @Test
    @Description("Изменение набора пикселей")
    fun testUpdateBannerWithPixelsModify() {
        val modelChanges = ModelChanges(bannerId, CpmBanner::class.java).apply {
            process(listOf(adfoxPixelUrl2()), CpmBanner.PIXELS)
        }

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        softly {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.pixelsList).containsOnly(adfoxPixelUrl2())
        }
    }

    @Test
    @Description("Сброс набора пикселей")
    fun testUpdateBannerWithPixelsReset() {
        val modelChanges = ModelChanges(bannerId, CpmBanner::class.java).apply {
            process(null, CpmBanner.PIXELS)
        }

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        softly {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.pixelsList).isEmpty()
        }
    }

    private fun createCpmBannerWithOnlyAdFoxPixelUrl(): BannerId {
        val adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(userInfo.clientInfo)

        val creativeInfo = steps.creativeSteps()
            .addDefaultHtml5Creative(adGroupInfo.clientInfo, steps.creativeSteps().nextCreativeId)
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(adGroupInfo.clientInfo)

        val banner = fullCpmBanner(adGroupInfo.campaignId, adGroupInfo.adGroupId, creativeInfo.creativeId)
            .withPixels(listOf(adfoxPixelUrl()))

        return addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)
    }
}

private typealias BannerId = Long
