package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.VCardGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerWithVCardTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var vcardRepository: VcardRepository

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
    @Description("Добавление визитки в баннер в груте")
    fun testUpdateBannerAddVCard() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val vcard = Vcard()
            .withCampaignId(mysqlAdGroupInfo.campaignId)
            .withUid(userInfo.uid)
            .withGeoId(213)
            .withCompanyName("Yandex company")
            .withOgrn("+7#499#955-58-28#")
        vcardRepository.addVcards(userInfo.shard, userInfo.uid, userInfo.clientId, listOf(vcard))

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(vcard.id, TextBanner.VCARD_ID)
        modelChanges.process(BannerVcardStatusModerate.NEW, TextBanner.VCARD_STATUS_MODERATE)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutVcard = VCardGrutApi(grutContext).getVCards(listOf(vcard.id))

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.vcardId).isEqualTo(vcard.id)
            assertThat(grutVcard).isNotNull
            assertThat(grutVcard[0].meta.id).isEqualTo(vcard.id)
        }.assertAll()
    }

    @Test
    @Description("Удаление визитки из баннера в груте")
    fun testUpdateBannerRemoveVCard() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val vcard = Vcard()
            .withCampaignId(mysqlAdGroupInfo.campaignId)
            .withUid(userInfo.uid)
            .withGeoId(213)
            .withCompanyName("Yandex company")
            .withOgrn("+7#499#955-58-28#")
        vcardRepository.addVcards(userInfo.shard, userInfo.uid, userInfo.clientId, listOf(vcard))

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withVcardId(vcard.id)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(null, TextBanner.VCARD_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.vcardId).isEqualTo(0)
        }.assertAll()
    }

    @Test
    @Description("Замена визитки в баннере в груте")
    fun testUpdateBannerReplaceVCard() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val vcard1 = Vcard()
            .withCampaignId(mysqlAdGroupInfo.campaignId)
            .withUid(userInfo.uid)
            .withGeoId(213)
            .withCompanyName("Yandex company")
            .withOgrn("+7#499#955-58-28#")
        vcardRepository.addVcards(userInfo.shard, userInfo.uid, userInfo.clientId, listOf(vcard1))

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withVcardId(vcard1.id)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val vcard2 = Vcard()
            .withCampaignId(mysqlAdGroupInfo.campaignId)
            .withUid(userInfo.uid)
            .withGeoId(213)
            .withCompanyName("Yandex company")
            .withOgrn("+7#499#955-58-28#")
        vcardRepository.addVcards(userInfo.shard, userInfo.uid, userInfo.clientId, listOf(vcard2))

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(vcard2.id, TextBanner.VCARD_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutVcard = VCardGrutApi(grutContext).getVCards(listOf(vcard2.id))

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.vcardId).isEqualTo(vcard2.id)
            assertThat(grutVcard).isNotNull
            assertThat(grutVcard[0].meta.id).isEqualTo(vcard2.id)
        }.assertAll()
    }
}
