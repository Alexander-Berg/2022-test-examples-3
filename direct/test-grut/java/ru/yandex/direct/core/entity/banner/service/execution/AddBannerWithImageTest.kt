package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.image.converter.BannerImageConverter
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.utils.colorStringToUint64
import ru.yandex.direct.core.grut.api.utils.crc64StringToLong
import ru.yandex.direct.core.testing.info.BannerImageFormat
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.BannerSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.grut.auxiliary.proto.MdsInfo
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AddBannerWithImageTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var bannerSteps: BannerSteps

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
    @Description("сохранение баннера с картинкой")
    fun testAddBannerWithImage() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val clientInfo = mysqlAdGroupInfo.getClientInfo()

        val bannerImageFormat = bannerSteps.createBannerImageFormat(clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withImageStatusShow(true)
            .withImageStatusModerate(StatusBannerImageModerate.NEW)
            .withImageHash(bannerImageFormat.imageHash)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        val mdsMeta = BannerImageConverter.toImageMdsMeta(bannerImageFormat.mdsMetaJson)!!

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.imagesList).hasSize(1)

            val image = grutBanner.spec.imagesList[0]

            assertThat(image.imageHash).isEqualTo(bannerImageFormat.imageHash)
            assertThat(image.hasImageType()).isTrue
            assertThat(image.mdsFileInfo.namespace).isEqualTo(bannerImageFormat.avatarNamespace.value)
            assertThat(image.mdsFileInfo.groupId).isEqualTo(bannerImageFormat.mdsGroupId.toInt())
            assertThat(image.mdsFileInfo.mdsFileName).isEqualTo(bannerImageFormat.imageHash)
            assertThat(image.mdsFileInfo.environment).isEqualTo(convertTestAvatarHostToEnvironment(bannerImageFormat.avatarHost).number)

            assertThat(image.avatarsImageMeta.colorWizBackground).isEqualTo(colorStringToUint64(mdsMeta.meta.colorWizBack))
            assertThat(image.avatarsImageMeta.colorWizButton).isEqualTo(colorStringToUint64(mdsMeta.meta.colorWizButton))
            assertThat(image.avatarsImageMeta.colorWizButtonText).isEqualTo(colorStringToUint64(mdsMeta.meta.colorWizButtonText))
            assertThat(image.avatarsImageMeta.averageColor).isEqualTo(colorStringToUint64(mdsMeta.meta.averageColor))
            assertThat(image.avatarsImageMeta.mainColor).isEqualTo(colorStringToUint64(mdsMeta.meta.mainColor))
            assertThat(image.avatarsImageMeta.crc64).isEqualTo(crc64StringToLong(mdsMeta.meta.crc64))

            assertThat(image.avatarsImageMeta.backgroundColors.bottom).isEqualTo(colorStringToUint64(mdsMeta.meta.backgroundColors.bottom))
            assertThat(image.avatarsImageMeta.backgroundColors.left).isEqualTo(colorStringToUint64(mdsMeta.meta.backgroundColors.left))
            assertThat(image.avatarsImageMeta.backgroundColors.right).isEqualTo(colorStringToUint64(mdsMeta.meta.backgroundColors.right))
            assertThat(image.avatarsImageMeta.backgroundColors.top).isEqualTo(colorStringToUint64(mdsMeta.meta.backgroundColors.top))

            val formatNames = mdsMeta.sizes.keys.toList()
            for (formatName in formatNames) {
                assertThat(image.avatarsImageMeta.formatsList).anyMatch { it.formatName == formatName }

                val format = image.avatarsImageMeta.formatsList.find { it.formatName == formatName }!!

                assertThat(format.width).isEqualTo(mdsMeta.sizes[formatName]!!.width)
                assertThat(format.height).isEqualTo(mdsMeta.sizes[formatName]!!.height)

                // Проверяем, что смартцентры отсортированы в лексикографическом порядке по названию
                val sortedSmartCenters = mdsMeta.sizes[formatName]!!.smartCenters
                    .entries
                    .toList()
                    .sortedBy { it.key }
                    .map { it.value }

                assertThat(format.smartCentersList).hasSize(sortedSmartCenters.size)

                for (i in sortedSmartCenters.indices) {
                    val smartCenter = sortedSmartCenters[i]!!
                    assertThat(format.smartCentersList[i].w).isEqualTo(smartCenter.width)
                    assertThat(format.smartCentersList[i].h).isEqualTo(smartCenter.height)
                    assertThat(format.smartCentersList[i].x).isEqualTo(smartCenter.x)
                    assertThat(format.smartCentersList[i].y).isEqualTo(smartCenter.y)
                }
            }
        }.assertAll()
    }

    private fun convertTestAvatarHostToEnvironment(
        avatarHost: BannerImageFormat.AvatarHost
    ): MdsInfo.TMdsFileInfo.EEnvironment {
        return when (avatarHost) {
            BannerImageFormat.AvatarHost.PROD -> {
                MdsInfo.TMdsFileInfo.EEnvironment.ENV_PRODUCTION
            }
            BannerImageFormat.AvatarHost.TEST -> {
                MdsInfo.TMdsFileInfo.EEnvironment.ENV_TEST
            }
        }
    }
}
