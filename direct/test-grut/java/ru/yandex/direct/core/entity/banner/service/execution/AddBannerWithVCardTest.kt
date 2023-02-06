package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.VCardGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class AddBannerWithVCardTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var grutContext: GrutContext

    @Autowired
    private lateinit var userSteps: UserSteps

    @Autowired
    private lateinit var vcardRepository: VcardRepository

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = userSteps.createDefaultUser()
    }

    @Test
    @Description("При создании баннера с визиткой в груте появляется заглушка визитки и баннер с vcard_id")
    fun testAddBannerWithVCard() {
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

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)
        val grutVcard = VCardGrutApi(grutContext).getVCards(listOf(vcard.id))

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.vcardId).isEqualTo(vcard.id)
            assertThat(grutVcard).isNotNull
            assertThat(grutVcard[0].meta.id).isEqualTo(vcard.id)
        }.assertAll()
    }
}
