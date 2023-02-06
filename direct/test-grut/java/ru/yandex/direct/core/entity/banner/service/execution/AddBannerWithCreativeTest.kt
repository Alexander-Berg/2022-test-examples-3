package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.CreativeGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.grut.objects.proto.Creative.TCreativeMetaBase.ECreativeType
import ru.yandex.qatools.allure.annotations.Description
import java.math.BigDecimal

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AddBannerWithCreativeTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var creativeSteps: CreativeSteps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var grutContext: GrutContext

    @Autowired
    private lateinit var userSteps: UserSteps

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = userSteps.createDefaultUser()
    }

    @Test
    @Description("Сохранение баннера с креативом")
    fun testAddBannerWithCreative() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val clientInfo = mysqlAdGroupInfo.getClientInfo()

        val creativeInfo = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withCreativeId(creativeInfo.creativeId)
            .withCreativeStatusModerate(BannerCreativeStatusModerate.NEW)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutCreative = CreativeGrutApi(grutContext).getCreative(creativeInfo.creativeId)

        SoftAssertions().apply {
            val creative = creativeInfo.creative

            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.creativeIdsList).hasSize(1)

            assertThat(grutBanner.spec.creativeIdsList[0]).isEqualTo(creativeInfo.creativeId)

            assertThat(grutCreative).isNotNull

            assertThat(grutCreative!!.spec.name).isEqualTo(creative.name)

            assertThat(grutCreative.spec.hasVideoData()).isTrue
            assertThat(grutCreative.spec.hasHtml5Data()).isFalse
            assertThat(grutCreative.meta.creativeType).isEqualTo(ECreativeType.CT_VIDEO_ADDITION.number)
            assertThat(grutCreative.spec.layoutId).isEqualTo(creative.layoutId)
            assertThat(grutCreative.spec.previewUrl).isEqualTo(creative.previewUrl)
            assertThat(grutCreative.spec.livePreviewUrl).isEqualTo(creative.livePreviewUrl)
            assertThat(grutCreative.spec.duration).isEqualTo(creative.duration)
            assertThat(grutCreative.spec.videoData.stockCreativeId).isEqualTo(creative.stockCreativeId)
            assertThat(grutCreative.spec.videoData.duration).isEqualTo(
                creative.additionalData.duration.multiply(
                    BigDecimal.valueOf(1000_000)
                ).toLong()
            )
            assertThat(grutCreative.spec.videoData.hasPackshot).isEqualTo(true)
            assertThat(grutCreative.spec.videoData.videoFormatsList[0].type)
                .isEqualTo(creative.additionalData.formats[0].type)
            assertThat(grutCreative.spec.videoData.videoFormatsList[0].url)
                .isEqualTo(creative.additionalData.formats[0].url)
            assertThat(grutCreative.spec.videoData.videoFormatsList[0].width)
                .isEqualTo(creative.additionalData.formats[0].width)
            assertThat(grutCreative.spec.videoData.videoFormatsList[0].height)
                .isEqualTo(creative.additionalData.formats[0].height)
        }.assertAll()
    }
}
