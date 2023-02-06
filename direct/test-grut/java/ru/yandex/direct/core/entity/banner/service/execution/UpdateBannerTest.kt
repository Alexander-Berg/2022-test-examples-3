package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate
import ru.yandex.direct.core.entity.banner.model.Language
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.OrganizationsSteps
import ru.yandex.direct.core.testing.steps.TurboLandingSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var turboLandingSteps: TurboLandingSteps

    @Autowired
    private lateinit var organizationsSteps: OrganizationsSteps

    @Autowired
    private lateinit var vcardRepository: VcardRepository

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var organizationsClientStub: OrganizationsClientStub

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
    @Description("Редактирование простых полей баннера")
    fun testUpdateBanner() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withHref("https://href-before.com")
            .withTitle("title before")
            .withTitleExtension("titleext before")
            .withBody("body before")
            .withLanguage(Language.RU_)
            .withGeoFlag(false)
        val bannerId = addBannerToMysqlAndGrut(
            bannersAddOperationFactory, banner, userInfo,
            FeatureName.MODERATION_BANNERS_IN_GRUT_ENABLED
        )

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process("https://href-after.com", TextBanner.HREF)
        modelChanges.process("title after", TextBanner.TITLE)
        modelChanges.process("titleext after", TextBanner.TITLE_EXTENSION)
        modelChanges.process("body after", TextBanner.BODY)
        modelChanges.process(Language.EN, TextBanner.LANGUAGE)
        modelChanges.process(true, TextBanner.GEO_FLAG)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.title).isEqualTo("title after")
            assertThat(grutBannerAfter.spec.titleExtension).isEqualTo("titleext after")
            assertThat(grutBannerAfter.status.skipModeration).isFalse
        }.assertAll()
    }

    @Test
    @Description("Добавление турболендинга к баннеру")
    fun testUpdateBannerAddTurbolanding() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val turboLanding = turboLandingSteps.createDefaultTurboLanding(userInfo.clientId)

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(turboLanding.id, TextBanner.TURBO_LANDING_ID)
        modelChanges.process("params=before", TextBanner.TURBO_LANDING_HREF_PARAMS)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.turbolandingId).isEqualTo(turboLanding.id)
            assertThat(grutBannerAfter.spec.turbolandingHref).isEqualTo(turboLanding.url)
            assertThat(grutBannerAfter.spec.turbolandingHrefParams).isEqualTo("params=before")
        }.assertAll()
    }

    @Test
    @Description("Удаление турболендинга из баннера")
    fun testUpdateBannerRemoveTurbolanding() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val turboLanding = turboLandingSteps.createDefaultTurboLanding(userInfo.clientId)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withTurboLandingId(turboLanding.id)
            .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.NEW)
            .withTurboLandingHrefParams("params=before")
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(null, TextBanner.TURBO_LANDING_ID)
        modelChanges.process(null, TextBanner.TURBO_LANDING_HREF_PARAMS)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.hasTurbolandingId()).isFalse
            assertThat(grutBannerAfter.spec.hasTurbolandingHref()).isFalse
            assertThat(grutBannerAfter.spec.hasTurbolandingHrefParams()).isFalse
        }.assertAll()
    }

    @Test
    @Description("Добавление пермалинка к баннеру")
    fun testUpdateBannerAddPermalink() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        // Визитка нужна для прохождения валидации на поле PREFER_V_CARD_OVER_PERMALINK
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
        val organization = organizationsSteps.createClientOrganization(userInfo.clientInfo!!)
        organizationsClientStub.addUidsByPermalinkId(organization.permalinkId, listOf(userInfo.uid))

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(organization.permalinkId, TextBanner.PERMALINK_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.permalink.id).isEqualTo(organization.permalinkId)
        }.assertAll()
    }
}
