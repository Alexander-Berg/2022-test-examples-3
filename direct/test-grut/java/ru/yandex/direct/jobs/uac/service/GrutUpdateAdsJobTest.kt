package ru.yandex.direct.jobs.uac.service

import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.doReturn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.bannerstorage.client.DummyBannerStorageClient
import ru.yandex.direct.bannerstorage.client.model.Template
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.model.Banner
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.CpmBanner
import ru.yandex.direct.core.entity.banner.model.DynamicBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.landing.model.BizLanding
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.getCreativeGroup
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdsLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CpmAssetButton
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacButtonAction
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.samples.defaultContentImageMeta
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy
import ru.yandex.direct.core.testing.steps.uac.EcomUcSubcampaignsInfo
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.jobs.uac.UpdateAdsJob
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.test.utils.checkContainsInAnyOrder
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus
import ru.yandex.grut.objects.proto.Campaign.TCampaignBrief
import ru.yandex.grut.objects.proto.Campaign.TCampaignSpec
import ru.yandex.grut.objects.proto.client.Schema
import ru.yandex.grut.objects.proto.client.Schema.TCampaignMeta
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.function.Consumer

private const val BIZ_LANDING_ID = 123L
private const val BIZ_LANDING_URL = "https://test-landing.tst-client.site"

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUpdateAdsJobTest {
    companion object {
        private val KEYWORDS_FOR_ADD = listOf("промокод озон",
            "озон промокод на скидку")
        private val EXPECTED_KEYWORDS_IN_DB = listOf("промокод озон -скидка",
            "озон промокод на скидку")
        private val KEYWORDS_FOR_ADD_2 = listOf("купить машину", "купить черную машину")
        private val EXPECTED_KEYWORDS_IN_DB_2 = listOf("купить машину -черный",
            "купить черную машину")
        private const val CREATIVES_COUNT = 5
        private val VALID_MODERATION_STATUSES = listOf(BannerStatusModerate.YES, BannerStatusModerate.READY,
            BannerStatusModerate.SENDING, BannerStatusModerate.SENT)
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var updateAdsJob: UpdateAdsJob

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var dummyBannerStorageClient: DummyBannerStorageClient

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var canvasClientStub: CanvasClientStub

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private var masterCampaignId = 0L
    private lateinit var uacCampaignId: String
    private lateinit var titleAsset1: Schema.TAsset
    private lateinit var titleAsset2: Schema.TAsset
    private lateinit var textAsset1: Schema.TAsset
    private lateinit var textAsset2: Schema.TAsset
    private lateinit var imageAsset1: Schema.TAsset
    private lateinit var imageAsset2: Schema.TAsset
    private lateinit var ecomUcSubcampaignsInfo: EcomUcSubcampaignsInfo
    private lateinit var creativeIds: List<Int>
    private lateinit var uacCampaignContents: List<UacYdbCampaignContent>
    private var videoAsset: Schema.TAsset? = null
    private var html5Asset: Schema.TAsset? = null
    private var creativeId = 0L

    private lateinit var imageHash1: String
    private lateinit var imageHash2: String

    private fun setup(
        isEcom: Boolean,
        briefSynced: Boolean = false,
        advType: AdvType = AdvType.TEXT,
        showTitleAndBody: Boolean? = null,
        withVideo: Boolean = false,
    ) {
        steps.trustedRedirectSteps().addValidCounters()
        userInfo = clientInfo.chiefUserInfo!!
        grutSteps.createClient(clientInfo)

        if (advType == AdvType.CPM_BANNER) {
            val creativeId = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
            val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString())
            grutUacContentService.insertContents(listOf(videoContent))
            uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo).id
            masterCampaignId = uacCampaignId.toIdLong()
            uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
            grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
            updateAdsJob.withShard(clientInfo.shard)
            return
        } else if (!isEcom) {
            val campaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(
                    AverageCpaStrategy()
                        .withMaxWeekSum(1000.toBigDecimal()).withAverageCpa(100.toBigDecimal()))

            val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

            masterCampaignId = campaignInfo.campaignId
            val uacCampaign = createYdbCampaign(
                id = masterCampaignId.toString(),
                advType = AdvType.TEXT,
                keywords = KEYWORDS_FOR_ADD,
                accountId = clientInfo.clientId!!.toString(),
                isEcom = false,
                briefSynced = briefSynced,
                zenPublisherId = "someId",
                showTitleAndBody = showTitleAndBody,
            )
            uacCampaignId = uacCampaign.id
            grutSteps.createTextCampaign(clientInfo, uacCampaign)
        } else {
            val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId
            val ecom = grutSteps.createEcomUcCampaign(
                clientInfo,
                feedId,
                KEYWORDS_FOR_ADD,
                useNewBackend = featureService.isEnabledForClientId(clientInfo.clientId!!,
                    FeatureName.ECOM_UC_NEW_BACKEND_ENABLED))
            masterCampaignId = ecom.first
            ecomUcSubcampaignsInfo = ecom.second
            uacCampaignId = masterCampaignId.toIdString()

            doReturn(Template(0, "", emptyList(), emptyList()))
                .`when`(dummyBannerStorageClient).getTemplate(ArgumentMatchers.anyInt(), anyVararg())

            creativeIds = List(CREATIVES_COUNT) { randomPositiveInt() }
            doReturn(getCreativeGroup(creativeIds))
                .`when`(dummyBannerStorageClient).createSmartCreativeGroup(ArgumentMatchers.any())

            grutSteps.createEcomUcCampaign(clientInfo, feedId)
            setupAssets(false, false)
        }
    }

    private fun setupAssets(withVideo: Boolean, withHtml5: Boolean) {
        val imageFormat1 = TestBanners.defaultBannerImageFormat(null)
        val imageFormat2 = TestBanners.defaultBannerImageFormat(null)
        imageHash1 = imageFormat1.imageHash
        imageHash2 = imageFormat2.imageHash
        steps.bannerSteps().createBannerImageFormat(clientInfo, TestBanners.defaultBannerImageFormat(imageHash1))
        steps.bannerSteps().createBannerImageFormat(clientInfo, TestBanners.defaultBannerImageFormat(imageHash2))
        creativeId = 2147318391
        canvasClientStub.addCreatives(listOf(creativeId))
        val assetIds = mutableListOf(
            grutSteps.createTitleAsset(clientInfo.clientId!!, "Title1"),
            grutSteps.createTitleAsset(clientInfo.clientId!!, "Title2"),
            grutSteps.createTextAsset(clientInfo.clientId!!, "Text1"),
            grutSteps.createTextAsset(clientInfo.clientId!!, "Text2"),
            grutSteps.createDefaultImageAsset(clientInfo.clientId!!, imageHash1, defaultContentImageMeta(imageHash1)),
            grutSteps.createDefaultImageAsset(clientInfo.clientId!!, imageHash2, defaultContentImageMeta(imageHash2)),
        )
        if (withVideo) {
            assetIds.add(grutSteps.createDefaultVideoAsset(clientInfo.clientId!!))
        }
        if (withHtml5) {
            assetIds.add(grutSteps.createDefaultHtml5Asset(clientInfo.clientId!!))
            steps.creativeSteps().addDefaultHtml5Creative(clientInfo, 271980L)
        }
        val assets = grutApiService.assetGrutApi.getAssets(assetIds.toIdsLong())
        assets.checkSize(assetIds.size)

        titleAsset1 = assets[0]
        titleAsset2 = assets[1]
        textAsset1 = assets[2]
        textAsset2 = assets[3]
        imageAsset1 = assets[4]
        imageAsset2 = assets[5]
        videoAsset = if (withVideo) assets[6] else null
        html5Asset = if (withHtml5) assets.last() else null

        uacCampaignContents = assetIds.map {
            createCampaignContent(contentId = it)
        }
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
    }

    private fun setupTextCampaign(
        showTitleAndBody: Boolean? = null,
        briefSynced: Boolean = false,
        startedAt: LocalDateTime? = now(),
        withVideo: Boolean = false,
    ) {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStrategy(
                AverageCpaStrategy().withMaxWeekSum(1000.toBigDecimal()).withAverageCpa(100.toBigDecimal()))

        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        masterCampaignId = campaignInfo.campaignId
        val uacCampaign = createYdbCampaign(
            id = masterCampaignId.toString(),
            advType = AdvType.TEXT,
            keywords = KEYWORDS_FOR_ADD,
            accountId = clientInfo.clientId!!.toString(),
            isEcom = false,
            briefSynced = briefSynced,
            zenPublisherId = "someId",
            showTitleAndBody = showTitleAndBody,
            startedAt = startedAt,
        )
        uacCampaignId = uacCampaign.id
        grutSteps.createTextCampaign(clientInfo, uacCampaign)
        setupAssets(withVideo, false)
    }

    private fun setupTextEcomCampaign(withVideo: Boolean = false) {
        val feedId = steps.feedSteps().createDefaultFeed(clientInfo).feedId
        val ecom = grutSteps.createEcomUcCampaign(clientInfo, feedId, KEYWORDS_FOR_ADD)
        masterCampaignId = ecom.first
        ecomUcSubcampaignsInfo = ecom.second
        uacCampaignId = masterCampaignId.toIdString()

        doReturn(Template(0, "", emptyList(), emptyList()))
            .`when`(dummyBannerStorageClient).getTemplate(ArgumentMatchers.anyInt(), anyVararg())

        creativeIds = List(CREATIVES_COUNT) { randomPositiveInt() }
        doReturn(getCreativeGroup(creativeIds))
            .`when`(dummyBannerStorageClient).createSmartCreativeGroup(ArgumentMatchers.any())

        grutSteps.createEcomUcCampaign(clientInfo, feedId)
        setupAssets(withVideo, false)
    }

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.trustedRedirectSteps().addValidCounters()
        userInfo = clientInfo.chiefUserInfo!!
        grutSteps.createClient(clientInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, false)
    }

    @AfterEach
    fun after() {
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP)
    }

    @Test
    fun test() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        checkCommon(soft)
        checkText(soft)
        soft.assertAll()
    }

    @Test
    fun testSecondGeneration_UpdateKeywords() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }
                    .build()
                spec = TCampaignSpec.newBuilder().apply {
                    campaignBrief = TCampaignBrief.newBuilder()
                        .addAllKeywords(KEYWORDS_FOR_ADD_2)
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/keywords", "/spec/campaign_brief/brief_synced")
        )

        val bannerIdsBefore = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val grutCampaign = grutApiService.briefGrutApi.getBrief(masterCampaignId)!!
        val bannerIdsAfter = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }

        val soft = SoftAssertions()
        soft.assertThat(grutCampaign.spec.campaignBrief.keywordsList)
            .containsExactlyInAnyOrder(*EXPECTED_KEYWORDS_IN_DB_2.toTypedArray())
        soft.assertThat(bannerIdsBefore).containsExactlyInAnyOrder(*bannerIdsAfter.toTypedArray())
        soft.assertAll()
    }

    @Test
    fun testSecondGeneration_DeleteAsset() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val newUacCampaignContents = listOf(
            uacCampaignContents[0].copy(removedAt = now(), status = CampaignContentStatus.DELETED),
            *uacCampaignContents.subList(1, uacCampaignContents.size).toTypedArray()
        )
        grutSteps.setCustomAssetLinksToCampaign(uacCampaignId.toIdLong(), newUacCampaignContents)
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }.build()
                spec = TCampaignSpec.newBuilder().apply {
                    campaignBrief = TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/brief_synced")
        )

        val bannerIdsBefore = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }
            .toSet()

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val bannerIdsAfter = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }
            .toSet()
        val grutBannersById = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/campaign_id] = $uacCampaignId",
            index = "banners_by_campaign"
        ).associateBy { it.meta.id }

        val deletedBannerIds = bannerIdsBefore - bannerIdsAfter

        val soft = SoftAssertions()
        soft.assertThat(deletedBannerIds).hasSize(4)
        grutBannersById.forEach { (id, banner) ->
            if (deletedBannerIds.contains(id)) {
                soft.assertThat(banner.spec.status).isEqualTo(EBannerStatus.BSS_DELETED)
            } else {
                soft.assertThat(banner.spec.status).isNotEqualTo(EBannerStatus.BSS_DELETED)
            }
        }
        soft.assertAll()
    }

    @Test
    fun testSecondGeneration_AddAsset() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val newAssetId = grutSteps.createTextAsset(clientInfo.clientId!!, "Text3")
        val newAssetLinkId = UacYdbUtils.generateUniqueRandomId()

        val newUacCampaignContents = listOf(
            *uacCampaignContents.toTypedArray(),
            UacYdbCampaignContent(
                id = newAssetLinkId,
                campaignId = uacCampaignId,
                contentId = newAssetId,
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.TEXT,
            )
        )
        grutSteps.setCustomAssetLinksToCampaign(uacCampaignId.toIdLong(), newUacCampaignContents)

        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }.build()
                spec = TCampaignSpec.newBuilder().apply {
                    campaignBrief = TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/brief_synced")
        )

        val bannerIdsBefore = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }
            .toSet()

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val bannerIdsAfter = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it.id }
            .toSet()
        val grutBannersById = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/campaign_id] = $uacCampaignId",
            index = "banners_by_campaign",
        ).associateBy { it.meta.id }

        val createdBannerIds = bannerIdsAfter - bannerIdsBefore

        val soft = SoftAssertions()
        soft.assertThat(createdBannerIds).hasSize(4)
        soft.assertThat(grutBannersById).hasSize(12)
        createdBannerIds.forEach { bannerId ->
            soft.assertThat(grutBannersById[bannerId]!!.spec.assetIdsList).contains(newAssetId.toIdLong())
            soft.assertThat(grutBannersById[bannerId]!!.spec.assetLinkIdsList).contains(newAssetLinkId.toIdLong())
        }
        soft.assertAll()
    }

    @Test
    fun testCreateBanner_withBizLanding() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)

        // Сохраним информацию о лэндинге и ссылку на него
        grutApiService.bizLandingGrutApi.createOrUpdateBizLanding(
            BizLanding().withId(BIZ_LANDING_ID).withUrl(BIZ_LANDING_URL)
        )
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }
                    .build()
                spec = TCampaignSpec.newBuilder().apply {
                    this.bizLandingId = BIZ_LANDING_ID
                }.build()
            }.build(),
            setPaths = listOf("/spec/biz_landing_id")
        )

        /*
        Проверяемый вызов джобы
         */
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
        // На всякий случай проверим ссылку на всех объявлениях
        val bannerPredicate = Consumer<Banner> {
            assertThat(it)
                .hasFieldOrPropertyWithValue("href", BIZ_LANDING_URL)
        }
        assertThat(banners)
            .allSatisfy(bannerPredicate)
    }

    @Test
    fun testUpdateBanner_withBizLanding() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        // Сохраним информацию о лэндинге
        grutApiService.bizLandingGrutApi.createOrUpdateBizLanding(
            BizLanding().withId(BIZ_LANDING_ID).withUrl(BIZ_LANDING_URL)
        )
        // Положим ссылку на лэндинг в кампанию-заявку и сбросим флаг синхронизации
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }
                    .build()
                spec = TCampaignSpec.newBuilder().apply {
                    this.bizLandingId = BIZ_LANDING_ID
                    campaignBrief = TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/biz_landing_id", "/spec/campaign_brief/brief_synced")
        )

        /*
        Проверяемый вызов джобы
         */
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
        val bannerPredicate = Consumer<Banner> {
            assertThat(it)
                .hasFieldOrPropertyWithValue("href", BIZ_LANDING_URL)
        }
        assertThat(banners)
            .allSatisfy(bannerPredicate)
    }

    @Test
    fun testUpdateBanner_removeBizLandingUrl() {
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)

        // Сохраним информацию о лэндинге и ссылку на него
        grutApiService.bizLandingGrutApi.createOrUpdateBizLanding(
            BizLanding().withId(BIZ_LANDING_ID).withUrl(BIZ_LANDING_URL)
        )
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }
                    .build()
                spec = TCampaignSpec.newBuilder().apply {
                    this.bizLandingId = BIZ_LANDING_ID
                }.build()
            }.build(),
            setPaths = listOf("/spec/biz_landing_id")
        )
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        // Удалим ссылку на лэндинг, не забывая сбросить флаг синхронизации
        grutApiService.briefGrutApi.updateBrief(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder()
                    .apply {
                        id = uacCampaignId.toIdLong()
                    }
                    .build()
                spec = TCampaignSpec.newBuilder().apply {
                    campaignBrief = TCampaignBrief.newBuilder()
                        .setBriefSynced(false)
                        .build()
                }.build()
            }.build(),
            setPaths = listOf("/spec/campaign_brief/brief_synced"),
            removePaths = listOf("/spec/biz_landing_id")
        )

        /*
        Проверяемый вызов джобы
         */
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
        val originalUrl = STORE_URL
        val bannerPredicate = Consumer<Banner> {
            assertThat(it)
                .hasFieldOrPropertyWithValue("href", originalUrl)
        }
        assertThat(banners)
            .allSatisfy(bannerPredicate)
    }

    @Test
    fun test_multipleAdGroups() {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP, "1")
        setupTextCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(uacCampaignId.toIdLong()))
        val adGroupIdToBannerIdInMysql = banners.asSequence().map { it as TextBanner }.groupBy({ it.adGroupId }, { it.id })

        val adGroupIdToBannerIdInGrut = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/campaign_id] = $uacCampaignId",
            index = "banners_by_campaign",
            attributeSelector = listOf("/meta/id", "/meta/ad_group_id")
        ).groupBy({ it.meta.adGroupId }, { it.meta.id })
        soft.assertThat(adGroupIdToBannerIdInMysql).isEqualTo(adGroupIdToBannerIdInGrut)
        soft.assertAll()
    }

    @Test
    fun test_draftCampaign() {
        setupTextCampaign(startedAt = null)
        updateAdsJob.withShard(clientInfo.shard)

        assertThrows<IllegalStateException> {
            updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)
        }

        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it as TextBanner }

        SoftAssertions.assertSoftly {
            it.assertThat(adGroups).isEmpty()
            it.assertThat(banners).isEmpty()
        }
    }

    @Test
    fun test_syncedCampaign() {
        setupTextCampaign(briefSynced = true)
        updateAdsJob.withShard(clientInfo.shard)

        assertThrows<IllegalStateException> {
            updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)
        }

        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .map { it as TextBanner }

        SoftAssertions.assertSoftly {
            it.assertThat(adGroups).isEmpty()
            it.assertThat(banners).isEmpty()
        }
    }

    @Test
    fun testEcom() {
        setupTextEcomCampaign()

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        checkCommon(soft)
        checkDynamic(soft, ecomUcSubcampaignsInfo.dynamicInfo.campaignId)
        checkSmart(soft, ecomUcSubcampaignsInfo.smartInfo.campaignId)
        soft.assertAll()
    }

    @Test
    fun testEcomOnNewBackend() {
        val clientId = clientInfo.clientId!!
        steps.featureSteps().enableClientFeature(clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.ENABLED_DYNAMIC_FEED_AD_TARGET_IN_TEXT_AD_GROUP)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.CREATIVE_FREE_ECOM_UC)
        steps.featureSteps().enableClientFeature(clientId, FeatureName.SMART_NO_CREATIVES)
        setup(isEcom = true)

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        checkCommon(soft)
        checkDynamicOnNewBackend(soft, masterCampaignId)
        checkSmartOnNewBackend(soft, masterCampaignId)
        soft.assertAll()
    }

    @Test
    fun test_TextCampaign_WithShowTitleAndBody() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DISABLE_VIDEO_CREATIVE, true)
        setupTextCampaign(showTitleAndBody = true, withVideo = true)

        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val soft = SoftAssertions()
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as TextBanner }
        banners.forEach {
            soft.assertThat(it.showTitleAndBody).isEqualTo(true)
        }
        soft.assertAll()
    }

    @Test
    fun testCpm() {
        val creativeId = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString(), nonSkippable = false)
        grutUacContentService.insertContents(listOf(videoContent))
        uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo, videosAreNonSkippable=false).id
        masterCampaignId = uacCampaignId.toIdLong()
        uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        // Проверить что пикселя в базе нет, но баннер есть
        var banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        assertThat(banners[0].pixels).isNullOrEmpty()

        //Поменять пиксель в camp, пересинхронизировать
        var camp = grutSteps.getCampaign(masterCampaignId)
        val contentId: String = camp.assetLinks?.get(0)?.contentId!!
        camp = camp.copy(cpmAssets = mapOf(
            contentId to UacCpmAsset(
                title = "asset title",
                body = "asset body",
                bannerHref = null,
                pixels = listOf(
                    "https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"
                ),
                measurers = null,
                logoImageHash = null,
                titleExtension = "title extension",
                button = null,
            ))
            , briefSynced = false
        )
        grutApiService.briefGrutApi.updateBriefFull(Schema.TCampaign.newBuilder().apply {
            meta = TCampaignMeta.newBuilder().setId(masterCampaignId).build()
            spec = UacGrutCampaignConverter.toCampaignSpec(camp)
        }.build())

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        assertThat(banners[0].pixels).isNotEmpty
    }

    @Test
    fun testCpmNonSkippable() {//Создаётся непропускаемое видео
        val creativeId = steps.creativeSteps().addDefaultNonSkippableCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString(), nonSkippable = true)
        grutUacContentService.insertContents(listOf(videoContent))
        uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo, videosAreNonSkippable=true).id
        masterCampaignId = uacCampaignId.toIdLong()
        uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
        updateAdsJob.withShard(clientInfo.shard)

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        var banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false).map { it as CpmVideoAdGroup }
        assertThat(adGroups).isNotEmpty
        assertThat(adGroups[0].isNonSkippable).isTrue
    }

    @Test
    fun testCpmNonSkippableMixCampaignTrueCreativeFalse() {
        //Создаётся креатив непропускаемое видео и заявка пропускаемое.
        // Джоба должна разрулить такую ситуацию и создать isNonSkippable=true группу
        val creativeId = steps.creativeSteps().addDefaultNonSkippableCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString(), nonSkippable = true)
        grutUacContentService.insertContents(listOf(videoContent))
        uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo, videosAreNonSkippable=false).id
        masterCampaignId = uacCampaignId.toIdLong()
        uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
        updateAdsJob.withShard(clientInfo.shard)

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        var banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false).map { it as CpmVideoAdGroup }
        assertThat(adGroups).isNotEmpty
        assertThat(adGroups[0].isNonSkippable).isTrue
    }

    @Test
    fun testCpmNonSkippableMixCampaignFalseCreativeTrue() {
        //Создаётся креатив обычное видео и заявка непропускаемое.
        // Джоба должна разрулить такую ситуацию и создать isNonSkippable=false группу
        val creativeId = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString(), nonSkippable = false)
        grutUacContentService.insertContents(listOf(videoContent))
        uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo, videosAreNonSkippable=true).id
        masterCampaignId = uacCampaignId.toIdLong()
        uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
        updateAdsJob.withShard(clientInfo.shard)

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        var banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false).map { it as CpmVideoAdGroup }
        assertThat(adGroups).isNotEmpty
        assertThat(adGroups[0].isNonSkippable).isFalse

        //Поменять пиксель в camp, пересинхронизировать
        var camp = grutSteps.getCampaign(masterCampaignId)
        val contentId: String = camp.assetLinks?.get(0)?.contentId!!
        camp = camp.copy(cpmAssets = mapOf(
            contentId to UacCpmAsset(
                title = "asset title",
                body = "asset body",
                bannerHref = null,
                pixels = listOf(
                    "https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=%25aw_random%25"
                ),
                measurers = null,
                logoImageHash = null,
                titleExtension = "title extension",
                button = null,
            )),
            briefSynced = false,
        )
        grutApiService.briefGrutApi.updateBriefFull(Schema.TCampaign.newBuilder().apply {
            meta = TCampaignMeta.newBuilder().setId(masterCampaignId).build()
            spec = UacGrutCampaignConverter.toCampaignSpec(camp)
        }.build())

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val adGroupsUpdated = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(
            setOf(masterCampaignId)), LimitOffset.maxLimited(), false).map { it as CpmVideoAdGroup }
        assertThat(adGroupsUpdated).isNotEmpty
        assertThat(adGroupsUpdated[0].isNonSkippable).isFalse
    }

    @Test
    fun testCpmButtonCaption() {
        val creativeId = steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().nextCreativeId).creativeId
        val videoContent = createDefaultVideoContent(creativeId = creativeId, accountId = clientInfo.clientId!!.toString(), nonSkippable = false)
        grutUacContentService.insertContents(listOf(videoContent))
        uacCampaignId = grutSteps.createAndGetCpmBannerCampaign(clientInfo, videosAreNonSkippable=false).id
        masterCampaignId = uacCampaignId.toIdLong()
        uacCampaignContents = listOf(createCampaignContent(contentId = videoContent.id))
        grutSteps.setCustomAssetLinksToCampaign(masterCampaignId, uacCampaignContents)
        updateAdsJob.withShard(clientInfo.shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        var camp = grutSteps.getCampaign(masterCampaignId)
        val contentId: String = camp.assetLinks?.get(0)?.contentId!!
        camp = camp.copy(cpmAssets = mapOf(
            contentId to UacCpmAsset(
                title = "asset title",
                body = "asset body",
                bannerHref = "http://direct.yandex.ru",
                pixels = null,
                measurers = null,
                logoImageHash = null,
                titleExtension = "title extension",
                button = CpmAssetButton(action = UacButtonAction.BOOK, customText = "", href = "http://google.ru"),
            ))
            , briefSynced = false
        )
        grutApiService.briefGrutApi.updateBriefFull(Schema.TCampaign.newBuilder().apply {
            meta = TCampaignMeta.newBuilder().setId(masterCampaignId).build()
            spec = UacGrutCampaignConverter.toCampaignSpec(camp)
        }.build())

        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        var banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as CpmBanner }
        assertThat(banners).isNotEmpty
        assertThat(banners[0].href).isEqualTo("http://direct.yandex.ru")
    }

    private fun checkCommon(soft: SoftAssertions) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId))
            .filterIsInstance<TextBanner>()
        soft.assertThat(banners).hasSize(8)
        val gotTitleAndTextList = banners.map { BannerAssets(it.title, it.body, it.imageHash) }
        soft.assertThat(gotTitleAndTextList).containsExactlyInAnyOrder(
            BannerAssets(titleAsset1.spec.title, textAsset1.spec.text, imageHash1),
            BannerAssets(titleAsset1.spec.title, textAsset2.spec.text, imageHash1),
            BannerAssets(titleAsset2.spec.title, textAsset1.spec.text, imageHash1),
            BannerAssets(titleAsset2.spec.title, textAsset2.spec.text, imageHash1),
            BannerAssets(titleAsset1.spec.title, textAsset1.spec.text, imageHash2),
            BannerAssets(titleAsset1.spec.title, textAsset2.spec.text, imageHash2),
            BannerAssets(titleAsset2.spec.title, textAsset1.spec.text, imageHash2),
            BannerAssets(titleAsset2.spec.title, textAsset2.spec.text, imageHash2),
        )

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val keywordPhrases = keywordRepository.getKeywordsByAdGroupId(clientInfo.shard, adGroupIds[0])
            .map { it.phrase }
        soft.assertThat(keywordPhrases).hasSize(2)
        soft.assertThat(keywordPhrases).containsExactlyInAnyOrder(*EXPECTED_KEYWORDS_IN_DB.toTypedArray())
        val grutBanners = grutApiService.briefBannerGrutApi.getBanners(banners.map { it.id }).associateBy { it.meta.id }
        soft.assertThat(grutBanners).hasSize(8)

        val assetIdToAssetLinkId = uacCampaignContents.associate { it.contentId!! to it.id }
        grutBanners.values.forEach { banner ->
            val expectedAssetLinkIds = banner.spec.assetIdsList
                .map { assetIdToAssetLinkId[it.toIdString()]!! }
            val actualAssetLinkIds = banner.spec.assetLinkIdsList.map { it.toIdString() }

            actualAssetLinkIds.checkContainsInAnyOrder(*expectedAssetLinkIds.toTypedArray())
        }

        val assetIdsByTitles = listOf(titleAsset1, titleAsset2).associate { it.spec.title to it.meta.id }
        val assetIdsByTexts = listOf(textAsset1, textAsset2).associate { it.spec.text to it.meta.id }
        val assetIdsByImages = listOf(imageAsset1, imageAsset2).associate { it.spec.image.directImageHash to it.meta.id }

        banners.forEach {
            val grutBanner = grutBanners[it.id]
            val titleAssetId = assetIdsByTitles[it.title]
            val textAssetId = assetIdsByTexts[it.body]
            val imageAssetId = assetIdsByImages[it.imageHash]
            soft.assertThat(grutBanner!!.spec.assetIdsList).containsExactlyInAnyOrder(titleAssetId, textAssetId, imageAssetId)
        }

        val grutAdGroup = grutApiService.briefAdGroupGrutApi.getAdGroup(adGroupIds[0])
        soft.assertThat(grutAdGroup).isNotNull
        soft.assertThat(grutAdGroup!!.meta.campaignId).isEqualTo(masterCampaignId)
    }

    private fun checkText(soft: SoftAssertions) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(masterCampaignId)).map { it as TextBanner }
        banners.forEach {
            soft.assertThat(it.zenPublisherId).isEqualTo("someId")
        }
    }

    private fun checkDynamic(soft: SoftAssertions, campaignId: Long) {
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(setOf(campaignId)), LimitOffset.maxLimited(), false)
        soft.assertThat(adGroups).hasSize(1)
        val adGroup = adGroups!![0]
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId)).map { it as DynamicBanner }
        soft.assertThat(banners).hasSize(2)
        val gotTitleAndTextList = banners.map { BannerAssets(it.title, it.body) }
        val dynamicTitle = "{Dynamic title}"
        soft.assertThat(gotTitleAndTextList).containsExactlyInAnyOrder(
            BannerAssets(dynamicTitle, textAsset1.spec.text),
            BannerAssets(dynamicTitle, textAsset2.spec.text)
        )

        val grutBanners = grutApiService.briefBannerGrutApi.getBanners(banners.map { it.id })
            .associateBy { it.meta.id }
        soft.assertThat(grutBanners).hasSize(2)

        val assetIdsByTexts = listOf(textAsset1, textAsset2).associate { it.spec.text to it.meta.id }

        banners.forEach {
            val grutBanner = grutBanners[it.id]
            val textAssetId = assetIdsByTexts[it.body]
            soft.assertThat(grutBanner!!.spec.assetIdsList).contains(textAssetId)
        }

        val grutAdGroup = grutApiService.briefAdGroupGrutApi.getAdGroup(adGroup.id)
        soft.assertThat(grutAdGroup).isNotNull
        soft.assertThat(grutAdGroup!!.meta.campaignId).isEqualTo(masterCampaignId)
    }

    private fun checkDynamicOnNewBackend(soft: SoftAssertions, campaignId: Long) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId))
            .filterIsInstance<DynamicBanner>()
        soft.assertThat(banners).hasSize(2)
        val gotTitleAndTextList = banners.map { BannerAssets(it.title, it.body) }
        val dynamicTitle = "{Dynamic title}"
        soft.assertThat(gotTitleAndTextList).containsExactlyInAnyOrder(
            BannerAssets(dynamicTitle, textAsset1.spec.text),
            BannerAssets(dynamicTitle, textAsset2.spec.text)
        )

        val moderationStatuses = banners.map { it.statusModerate }.toSet()
        soft.assertThat(moderationStatuses).hasSize(1)
        soft.assertThat(moderationStatuses).allMatch { VALID_MODERATION_STATUSES.contains(it) }

        val grutBanners = grutApiService.briefBannerGrutApi.getBanners(banners.map { it.id })
            .associateBy { it.meta.id }
        soft.assertThat(grutBanners).hasSize(2)

        val assetIdsByTexts = listOf(textAsset1, textAsset2).associate { it.spec.text to it.meta.id }

        banners.forEach {
            val grutBanner = grutBanners[it.id]
            val textAssetId = assetIdsByTexts[it.body]
            soft.assertThat(grutBanner!!.spec.assetIdsList).contains(textAssetId)
        }

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val grutAdGroup = grutApiService.briefAdGroupGrutApi.getAdGroup(adGroupIds[0])
        soft.assertThat(grutAdGroup).isNotNull
        soft.assertThat(grutAdGroup!!.meta.campaignId).isEqualTo(masterCampaignId)
    }

    private fun checkSmart(soft: SoftAssertions, campaignId: Long) {
        val adGroups = adGroupService.getAdGroupsBySelectionCriteria(AdGroupsSelectionCriteria().withCampaignIds(setOf(campaignId)), LimitOffset.maxLimited(), false)
        soft.assertThat(adGroups).hasSize(1)
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId)).map { it as PerformanceBanner }

        soft.assertThat(banners.map { it.creativeId.toInt() })
            .containsExactlyElementsOf(creativeIds)

        val grutAdGroup = grutApiService.briefAdGroupGrutApi.getAdGroup(adGroups[0].id)
        soft.assertThat(grutAdGroup).isNotNull
        soft.assertThat(grutAdGroup!!.meta.campaignId).isEqualTo(masterCampaignId)

        val grutBanners = grutApiService.briefBannerGrutApi.getBanners(banners.map { it.id })
        soft.assertThat(grutBanners).hasSize(creativeIds.size)
    }

    private fun checkSmartOnNewBackend(soft: SoftAssertions, campaignId: Long) {
        val banners = bannerTypedRepository.getBannersByCampaignIds(clientInfo.shard, listOf(campaignId))
            .filterIsInstance<PerformanceBannerMain>()

        val moderationStatuses = banners.map { it.statusModerate }.toSet()
        soft.assertThat(moderationStatuses).hasSize(1)
        soft.assertThat(moderationStatuses).allMatch { VALID_MODERATION_STATUSES.contains(it) }

        val adGroupIds = banners.map { it.adGroupId }.distinct()
        soft.assertThat(adGroupIds).hasSize(1)

        val grutAdGroup = grutApiService.briefAdGroupGrutApi.getAdGroup(adGroupIds[0])
        soft.assertThat(grutAdGroup).isNotNull
        soft.assertThat(grutAdGroup!!.meta.campaignId).isEqualTo(masterCampaignId)

        val grutBanners = grutApiService.briefBannerGrutApi.getBanners(banners.map { it.id })
        soft.assertThat(grutBanners).hasSize(1)
    }

    data class BannerAssets(
        val title: String,
        val text: String,
        val imageHash: String? = null,
    )
}
