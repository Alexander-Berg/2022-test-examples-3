package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.CreativeGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerWithCreativeTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var creativeSteps: CreativeSteps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var bannersUpdateOperationFactory: BannersUpdateOperationFactory

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
    @Description("Добавление креатива к существующему баннеру в груте")
    fun testUpdateBannerAddCreative() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val clientInfo = mysqlAdGroupInfo.getClientInfo()

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val creativeInfo = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(creativeInfo.creativeId, TextBanner.CREATIVE_ID)
        modelChanges.process(BannerCreativeStatusModerate.NEW, TextBanner.CREATIVE_STATUS_MODERATE)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutCreative = CreativeGrutApi(grutContext).getCreative(creativeInfo.creativeId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.creativeIdsList).hasSize(1)

            assertThat(grutBanner.spec.creativeIdsList[0]).isEqualTo(creativeInfo.creativeId)

            // креатив должен был создаться в груте в операции обновления баннера
            assertThat(grutCreative).isNotNull
        }.assertAll()
    }

    @Test
    @Description("Удаление креатива из баннера в груте")
    fun testUpdateBannerRemoveCreative() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val clientInfo = mysqlAdGroupInfo.getClientInfo()

        val creativeInfo = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withCreativeId(creativeInfo.creativeId)
            .withCreativeStatusModerate(BannerCreativeStatusModerate.NEW)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(null, TextBanner.CREATIVE_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.creativeIdsList).isEmpty()
        }.assertAll()
    }

    @Test
    @Description("Замена креатива в баннере в груте")
    fun testUpdateBannerReplaceCreative() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val clientInfo = mysqlAdGroupInfo.getClientInfo()

        val creativeInfo1 = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withCreativeId(creativeInfo1.creativeId)
            .withCreativeStatusModerate(BannerCreativeStatusModerate.NEW)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val creativeInfo2 = creativeSteps.addDefaultVideoAdditionCreative(clientInfo)

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(creativeInfo2.creativeId, TextBanner.CREATIVE_ID)
        modelChanges.process(BannerCreativeStatusModerate.NEW, TextBanner.CREATIVE_STATUS_MODERATE)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutCreative = CreativeGrutApi(grutContext).getCreative(creativeInfo2.creativeId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.creativeIdsList).hasSize(1)

            assertThat(grutBanner.spec.creativeIdsList[0]).isEqualTo(creativeInfo2.creativeId)

            // креатив должен был создаться в груте в операции обновления баннера
            assertThat(grutCreative).isNotNull
        }.assertAll()
    }
}
