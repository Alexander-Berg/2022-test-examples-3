package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.model.ModelChanges
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateBannerWithSitelinksTest {
    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var sitelinkRepository: SitelinkRepository

    @Autowired
    private lateinit var sitelinkSetRepository: SitelinkSetRepository

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
    @Description("Редактирование сайтлинков баннера")
    fun testUpdateBannerSitelinks() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)
        val grutBannerBefore = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // update
        val sitelink = Sitelink()
            .withTitle("title")
            .withHref("https://yandex.ru/sitelink")
            .withDescription("description")
        sitelinkRepository.add(userInfo.shard, listOf(sitelink))

        val sitelinkSet = SitelinkSet()
            .withSitelinks(listOf(sitelink))
            .withClientId(userInfo.clientId.asLong())
        sitelinkSetRepository.add(userInfo.shard, listOf(sitelinkSet))

        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(sitelinkSet.id, TextBanner.SITELINKS_SET_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        // compare
        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerBefore).isNotNull
            assertThat(grutBannerBefore!!.spec.sitelinkSet.sitelinksList).isEmpty()

            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.sitelinkSet.sitelinksList).hasSize(1)
            assertThat(grutBannerAfter.spec.sitelinkSet.sitelinksList[0].title).isEqualTo(sitelink.title)
            assertThat(grutBannerAfter.spec.sitelinkSet.sitelinksList[0].href).isEqualTo(sitelink.href)
            assertThat(grutBannerAfter.spec.sitelinkSet.sitelinksList[0].description).isEqualTo(sitelink.description)
        }.assertAll()
    }

    @Test
    @Description("Удаление сайтлинков из баннера")
    fun testRemoveBannerSitelinks() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val sitelink = Sitelink()
            .withTitle("title")
            .withHref("https://yandex.ru/sitelink")
            .withDescription("description")
        sitelinkRepository.add(userInfo.shard, listOf(sitelink))

        val sitelinkSet = SitelinkSet()
            .withSitelinks(listOf(sitelink))
            .withClientId(userInfo.clientId.asLong())
        sitelinkSetRepository.add(userInfo.shard, listOf(sitelinkSet))

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withSitelinksSetId(sitelinkSet.id)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)
        val grutBannerBefore = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // update
        val modelChanges = ModelChanges(bannerId, TextBanner::class.java)
        modelChanges.process(null, TextBanner.SITELINKS_SET_ID)

        updateBannerInMysqlAndGrut(bannersUpdateOperationFactory, modelChanges, userInfo)

        // compare
        val grutBannerAfter = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBannerBefore).isNotNull
            assertThat(grutBannerBefore!!.spec.sitelinkSet.sitelinksList).isNotEmpty()

            assertThat(grutBannerAfter).isNotNull
            assertThat(grutBannerAfter!!.spec.sitelinkSet.hasId()).isFalse
            assertThat(grutBannerAfter.spec.sitelinkSet.sitelinksList).isEmpty()
        }.assertAll()
    }
}
