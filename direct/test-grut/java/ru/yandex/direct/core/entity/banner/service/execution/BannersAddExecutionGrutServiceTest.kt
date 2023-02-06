package ru.yandex.direct.core.entity.banner.service.execution

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository
import ru.yandex.direct.core.entity.turbolanding.model.StatusPostModerate
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.grut.api.AdGroupGrut
import ru.yandex.direct.core.grut.api.AdGroupGrutApi
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.CampaignGrutApi
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutApi
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.grut.client.GrutClient
import ru.yandex.grut.objects.proto.client.Schema.EObjectType
import ru.yandex.grut.objectwatcher.ObjectWatcher
import ru.yandex.qatools.allure.annotations.Description

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class BannersAddExecutionGrutServiceTest {
    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var adgroupSteps: AdGroupSteps

    @Autowired
    private lateinit var adgroupRepository: AdGroupRepository

    @Autowired
    private lateinit var bannersAddOperationFactory: BannersAddOperationFactory

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var campaignOldRepository: ru.yandex.direct.core.entity.campaign.repository.CampaignRepository

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutContext: GrutContext

    @Autowired
    private lateinit var userSteps: UserSteps

    @Autowired
    private lateinit var turbolandingRepository: TurboLandingRepository

    @Autowired
    private lateinit var sitelinkRepository: SitelinkRepository

    @Autowired
    private lateinit var sitelinkSetRepository: SitelinkSetRepository

    @Autowired
    private lateinit var grutClient: GrutClient

    private lateinit var userInfo: UserInfo

    private val shard = 1

    @Before
    fun before() {
        userInfo = userSteps.createDefaultUser()
    }

    @Test
    @Description("При создании баннера создаётся вся иерархия клиент-кампания-группа, если её ещё нет в груте")
    fun testAddBannerWithNoClientNoCampaignNoAdGroup() {
        // Создаём группу (с кампанией и клиентом) в mysql
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)
        val mysqlAdGroup = mysqlAdGroupInfo.adGroup

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutClient = ClientGrutApi(grutContext).getClient(mysqlAdGroupInfo.clientInfo.client!!.clientId)
        val grutCampaign = CampaignGrutApi(grutContext).getCampaignByDirectId(mysqlAdGroupInfo.campaignId)
        val grutAdGroup = AdGroupGrutApi(grutContext).getAdGroup(mysqlAdGroupInfo.adGroupId)
        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // Убедимся, что в груте созданы клиент, кампания, группа, баннер
        SoftAssertions().apply {
            assertThat(grutClient).isNotNull
            assertThat(grutCampaign).isNotNull
            assertThat(grutAdGroup).isNotNull
            assertThat(grutBanner).isNotNull

            assertThat(grutCampaign!!.meta.directId).isEqualTo(mysqlAdGroup.campaignId)

            assertThat(grutAdGroup).extracting("meta.id", "meta.campaignId", "spec.regionsIdsList")
                .containsExactly(mysqlAdGroup.id, grutCampaign.meta.id, mysqlAdGroup.geo)

            assertThat(grutBanner!!.spec.href).isEqualTo(banner.href)
            assertThat(grutBanner.status.skipModeration).isTrue
        }.assertAll()
    }

    @Test
    @Description("Если клиент/кампания/группа уже созданы, то они не перезаписываются и не апдейтятся")
    fun testAddUcBannerWithExistingClientCampaignAdGroup() {
        // Без созданного клиента в грут не добавится заявка
        grutSteps.createClient(userInfo.clientInfo!!)

        // Создадим кампанию-заявку (в mysql и в груте)
        val brief = grutSteps.createAndGetTextCampaign(userInfo.clientInfo!!).uacCampaign
        val campaignId = brief.id.toIdLong()
        val campaign0 = campaignRepository.getCampaigns(shard, listOf(campaignId))[0]

        // Создадим кампанию и группу в mysql и потом в груте
        val mysqlAdGroupInfo = adgroupSteps.createActiveTextAdGroup(CampaignInfo(userInfo.clientInfo, campaign0))
        CampaignGrutApi(grutContext).createOrUpdateCampaign(
            CampaignGrutModel(
                campaignTypedRepository.getTyped(shard, listOf(campaignId))[0] as CommonCampaign,
                0
            )
        )
        val createdGrutCampaign = CampaignGrutApi(grutContext).getCampaignByDirectId(mysqlAdGroupInfo.campaignId)
        AdGroupGrutApi(grutContext).createOrUpdateAdGroup(
            AdGroupGrut(
                mysqlAdGroupInfo.adGroupId,
                createdGrutCampaign!!.meta.id,
                mysqlAdGroupInfo.adGroup.name,
                mysqlAdGroupInfo.adGroupType,
                regions = mysqlAdGroupInfo.adGroup.geo,
                cryptaSegmentsMapping = emptyMap(),
            )
        )

        // Обновим клиента, кампанию и группу в mysql перед запуском операции добавления баннера
        val client = clientRepository.get(shard, listOf(userInfo.clientId))[0]
        val oldClientName = client.name
        val clientModelChanges = ModelChanges(client.id, Client::class.java)
        clientModelChanges.process("changed client name", Client.NAME)
        val clientAppliedChanges = clientModelChanges.applyTo(client)
        clientRepository.update(shard, listOf(clientAppliedChanges))

        val campaign = campaignOldRepository.getCampaigns(shard, listOf(campaignId))[0]
        val oldCampaignName = campaign.name
        val campaignModelChanges = ModelChanges(campaign.id, Campaign::class.java)
        campaignModelChanges.process("changed campaign name", Campaign.NAME)
        campaignOldRepository.updateCampaigns(shard, listOf(campaignModelChanges.applyTo(campaign)))

        val adGroup = adgroupRepository.getAdGroups(shard, listOf(mysqlAdGroupInfo.adGroupId))[0]
        val oldAdgroupName = adGroup.name
        val adGroupModelChanges = ModelChanges(adGroup.id, AdGroup::class.java)
        adGroupModelChanges.process("changed adgroup name", AdGroup.NAME)
        adgroupRepository.updateAdGroups(shard, userInfo.clientId, listOf(adGroupModelChanges.applyTo(adGroup)))

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutClient = ClientGrutApi(grutContext).getClient(mysqlAdGroupInfo.clientInfo.client!!.clientId)
        val grutCampaign = CampaignGrutApi(grutContext).getCampaignByDirectId(mysqlAdGroupInfo.campaignId)
        val grutAdGroup = AdGroupGrutApi(grutContext).getAdGroup(mysqlAdGroupInfo.adGroupId)
        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // Убедимся, что в груте создан баннер, а остальные объекты не поменялись
        SoftAssertions().apply {
            assertThat(grutClient!!.spec.name).isEqualTo(oldClientName)
            assertThat(grutCampaign!!.spec.name).isEqualTo(oldCampaignName)
            assertThat(grutAdGroup!!.spec.name).isEqualTo(oldAdgroupName)

            assertThat(grutBanner!!.spec.href).isEqualTo(banner.href)
        }.assertAll()
    }

    @Test
    @Description("Создание 2 баннеров в одной группе")
    fun testAddTwoBannersWithNoClientNoCampaignNoAdGroup() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner1 = createTextBanner(mysqlAdGroupInfo)
            .withHref("http://yandex.ru/?click=test1")
        val banner2 = createTextBanner(mysqlAdGroupInfo)
            .withHref("http://yandex.ru/?click=test2")

        val ids = addBannersToMysqlAndGrut(bannersAddOperationFactory, listOf(banner1, banner2), userInfo)

        val grutBanner1 = BannerGrutApi(grutContext).getBannerByDirectId(ids[0])
        val grutBanner2 = BannerGrutApi(grutContext).getBannerByDirectId(ids[1])

        SoftAssertions().apply {
            assertThat(grutBanner1!!.spec.href).isEqualTo(banner1.href)
            assertThat(grutBanner2!!.spec.href).isEqualTo(banner2.href)
        }.assertAll()
    }

    @Test
    @Description("Поля турболендингов на баннере корректно пишутся в грут")
    fun testAddBannerWithTurbolanding() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val turboLanding = TurboLanding()
            .withId(5L)
            .withClientId(userInfo.clientId.asLong())
            .withUrl("https://turbolanding.com/href")
            .withName("turbo-name")
            .withMetrikaCounters("[]")
            .withPreviewHref("")
            .withVersion(0)
            .withLastModeratedVersion(0)
            .withIsChanged(false)
            .withStatusPostModerate(StatusPostModerate.YES)
        turbolandingRepository.add(shard, listOf(turboLanding))

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withHref("http://yandex.ru/?click=test")
            .withTurboLandingId(turboLanding.id)
            .withTurboLandingHrefParams("test_param1=1&test_param2=2")

        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutClient = ClientGrutApi(grutContext).getClient(mysqlAdGroupInfo.clientInfo.client!!.clientId)
        val grutCampaign = CampaignGrutApi(grutContext).getCampaignByDirectId(mysqlAdGroupInfo.campaignId)
        val grutAdGroup = AdGroupGrutApi(grutContext).getAdGroup(mysqlAdGroupInfo.adGroupId)
        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // Убедимся, что в груте сохранены правильные поля турболендингов для баннера
        SoftAssertions().apply {
            assertThat(grutClient).isNotNull
            assertThat(grutCampaign).isNotNull
            assertThat(grutAdGroup).isNotNull
            assertThat(grutBanner).isNotNull

            assertThat(grutBanner!!.spec.href).isEqualTo(banner.href)
            assertThat(grutBanner.spec.turbolandingId).isEqualTo(banner.turboLandingId)
            assertThat(grutBanner.spec.turbolandingHref).isEqualTo(turboLanding.url)
            assertThat(grutBanner.spec.turbolandingHrefParams).isEqualTo(banner.turboLandingHrefParams)
        }.assertAll()
    }

    @Test
    @Description("Поля турболендингов на сайтлинках корректно пишутся в грут")
    fun testAddBannerWithSitelinksWithTurbolanding() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val turboLanding = TurboLanding()
            .withId(6L)
            .withClientId(userInfo.clientId.asLong())
            .withUrl("https://turbolanding.com/href")
            .withName("turbo-name")
            .withMetrikaCounters("[]")
            .withPreviewHref("")
            .withVersion(0)
            .withLastModeratedVersion(0)
            .withIsChanged(false)
            .withStatusPostModerate(StatusPostModerate.YES)
        turbolandingRepository.add(shard, listOf(turboLanding))

        val sitelinkSet = SitelinkSet()
            .withClientId(userInfo.clientId.asLong())
            .withSitelinks(
                listOf(
                    Sitelink()
                        .withHref("http://sitelink.com/href")
                        .withTurboLandingId(turboLanding.id)
                        .withTitle("")
                )
            )
        sitelinkRepository.add(shard, sitelinkSet.sitelinks)
        sitelinkSetRepository.add(shard, listOf(sitelinkSet))

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withHref("http://yandex.ru/?click=test")
            .withSitelinksSetId(sitelinkSet.id)

        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutClient = ClientGrutApi(grutContext).getClient(mysqlAdGroupInfo.clientInfo.client!!.clientId)
        val grutCampaign = CampaignGrutApi(grutContext).getCampaignByDirectId(mysqlAdGroupInfo.campaignId)
        val grutAdGroup = AdGroupGrutApi(grutContext).getAdGroup(mysqlAdGroupInfo.adGroupId)
        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        // Убедимся, что в груте сохранены правильные поля турболендингов для сайтлинков
        SoftAssertions().apply {
            assertThat(grutClient).isNotNull
            assertThat(grutCampaign).isNotNull
            assertThat(grutAdGroup).isNotNull
            assertThat(grutBanner).isNotNull

            assertThat(grutBanner!!.spec.href).isEqualTo(banner.href)
            assertThat(grutBanner.spec.sitelinkSet.sitelinksList[0].turbolandingId)
                .isEqualTo(sitelinkSet.sitelinks[0].turboLandingId)
            assertThat(grutBanner.spec.sitelinkSet.sitelinksList[0].turbolandingHref)
                .isEqualTo(turboLanding.url)
            assertThat(grutBanner.spec.hasTurbolandingHref()).isFalse
            assertThat(grutBanner.spec.hasTurbolandingHrefParams()).isFalse
        }.assertAll()
    }

    @Test
    @Description("geoflag из banners.opts пишется в грут")
    fun testAddBannerWithGeoFlag() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
            .withGeoFlag(true)
        val bannerId = addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.spec.flags.hasGeoflag()).isTrue
            assertThat(grutBanner.spec.flags.geoflag).isEqualTo(banner.geoFlag)
        }.assertAll()
    }

    @Test
    @Description("Создание баннера с включенной фичей MODERATION_BANNERS_IN_GRUT_ENABLED")
    fun testAddBannerWithSkipModerationIsFalse() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val banner = createTextBanner(mysqlAdGroupInfo)
        val bannerId = addBannerToMysqlAndGrut(
            bannersAddOperationFactory, banner, userInfo,
            FeatureName.MODERATION_BANNERS_IN_GRUT_ENABLED
        )

        val grutBanner = BannerGrutApi(grutContext).getBannerByDirectId(bannerId)

        SoftAssertions().apply {
            assertThat(grutBanner).isNotNull
            assertThat(grutBanner!!.status.skipModeration).isFalse
        }.assertAll()
    }

    @Test
    @Description("Поле shard сохраняется в transaction context")
    fun testAddBanner_TransactionContextIsCorrect() {
        val mysqlAdGroupInfo = adgroupSteps.createDefaultAdGroup(userInfo.clientInfo)

        val watch = ObjectWatcher(grutClient).createWatch(
            "testAddBanner_TransactionContextIsCorrect",
            EObjectType.OT_BANNER_CANDIDATE,
        )

        val banner = createTextBanner(mysqlAdGroupInfo)
        addBannerToMysqlAndGrut(bannersAddOperationFactory, banner, userInfo)

        Thread.sleep(3000)

        val watchlogEvents = watch.getEventsImmediately()
        SoftAssertions().apply {
            assertThat(watchlogEvents).isNotEmpty
            if (watchlogEvents.isNotEmpty()) {
                assertThat(watchlogEvents[0].transactionContext.shard).isEqualTo(shard)
            }
        }.assertAll()
    }
}
