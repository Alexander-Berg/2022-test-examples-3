package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.BannerSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerWithImageTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var bannerSteps: BannerSteps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var grutContext: GrutContext

    @Autowired
    private lateinit var bannersUpdateOperationFactory: BannersUpdateOperationFactory

    @Autowired
    private lateinit var userSteps: UserSteps

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = userSteps.createDefaultUser()
    }

    @Test
    @Description("Добавление картинки к баннеру в груте")
    fun testUpdateBannerAddImage() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val bannerImageFormat = bannerSteps.createBannerImageFormat(mysqlAdGroupInfo.clientInfo)

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(true, TextBanner.IMAGE_STATUS_SHOW)
        modelChanges.process(100000000L + 1404L, TextBanner.IMAGE_BS_BANNER_ID)
        modelChanges.process(StatusBannerImageModerate.NEW, TextBanner.IMAGE_STATUS_MODERATE)
        modelChanges.process(bannerImageFormat.imageHash, TextBanner.IMAGE_HASH)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.imagesList).isNotNull

            assertThat(grutBannerAfter.spec.imagesList).hasSize(1)
            assertThat(grutBannerAfter.spec.imagesList[0].imageHash).isEqualTo(bannerImageFormat.imageHash)
        }.assertAll()
    }

    @Test
    @Description("Удаление картинки из баннера в груте")
    fun testUpdateBannerRemoveImage() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val bannerImageFormat = bannerSteps.createBannerImageFormat(mysqlAdGroupInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withImageStatusShow(true)
            .withImageStatusModerate(StatusBannerImageModerate.NEW)
            .withImageHash(bannerImageFormat.imageHash)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(null, TextBanner.IMAGE_HASH)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.imagesList).isNotNull

            assertThat(grutBannerAfter.spec.imagesList).hasSize(0)
        }.assertAll()
    }
}
