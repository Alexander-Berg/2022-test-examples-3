package ru.yandex.direct.jobs.uac.service

import com.google.common.collect.Lists
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.MAX_BANNERS_IN_UAC_TEXT_AD_GROUP
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository
import ru.yandex.direct.core.entity.uac.createAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdsLong
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.samples.defaultContentImageMeta
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.grut.api.AdGroupBriefGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.CreativeSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.dbutil.sharding.ShardKey
import ru.yandex.direct.dbutil.sharding.ShardSupport
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.jobs.uac.UpdateAdsJob
import ru.yandex.direct.utils.fromJson
import ru.yandex.grut.objects.proto.MediaType
import ru.yandex.grut.objects.proto.client.Schema

open class GrutUpdateAdsJobForAdGroupBriefBaseTest {
    companion object {
        val KEYWORDS_FOR_CAMPAIGN = listOf(
            "camp keyword",
            "campaign key",
        )
        val KEYWORDS_FOR_ADGROUP = listOf(
            "промокод озон",
            "озон промокод на скидку",
        )
        val EXPECTED_KEYWORDS_FOR_ADGROUP = setOf(
            "промокод озон -скидка",
            "озон промокод на скидку",
        )

        const val MAX_COUNT_OF_BANNERS_IN_GROUP = 2
        const val CREATIVE_ID = 2147318391L
        const val HTML5_CREATIVE_ID = 271980L
    }

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var updateAdsJob: UpdateAdsJob

    @Autowired
    lateinit var adGroupService: AdGroupService

    @Autowired
    lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    lateinit var keywordRepository: KeywordRepository

    @Autowired
    lateinit var grutSteps: GrutSteps

    @Autowired
    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    lateinit var grutApiService: GrutApiService

    @Autowired
    lateinit var canvasClientStub: CanvasClientStub

    @Autowired
    lateinit var creativeSteps: CreativeSteps

    @Autowired
    lateinit var shardSupport: ShardSupport

    @Autowired
    lateinit var grutUacCampaignService: GrutUacCampaignService

    lateinit var clientInfo: ClientInfo
    var shard = 0
    lateinit var userInfo: UserInfo
    var directCampaignId = 0L
    lateinit var uacCampaignId: String

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.trustedRedirectSteps().addValidCounters()
        userInfo = clientInfo.chiefUserInfo!!
        grutSteps.createClient(clientInfo)
        shard = clientInfo.shard

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.ECOM_UC_NEW_BACKEND_ENABLED, false)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED, true)
        ppcPropertiesSupport.set(MAX_BANNERS_IN_UAC_TEXT_AD_GROUP, MAX_COUNT_OF_BANNERS_IN_GROUP.toString())

        canvasClientStub.addCreatives(listOf(CREATIVE_ID))
    }

    @AfterEach
    fun after() {
        ppcPropertiesSupport.remove(MAX_BANNERS_IN_UAC_TEXT_AD_GROUP)
        creativeSteps.deleteCreativesByIds(clientInfo.shard, HTML5_CREATIVE_ID, CREATIVE_ID)
        shardSupport.deleteValues(ShardKey.CREATIVE_ID, listOf(HTML5_CREATIVE_ID, CREATIVE_ID))
    }

    fun createAssets(
        titleAssetCount: Int,
        textAssetCount: Int,
        imageAssetCount: Int,
        withVideo: Boolean = false,
        withHtml5: Boolean = false,
    ): Pair<List<Schema.TAsset>, List<UacYdbCampaignContent>> {
        val assetIds = mutableListOf<String>()
        for (titleAsset in 1..titleAssetCount) {
            assetIds.add(grutSteps.createTitleAsset(clientInfo.clientId!!, "Title $titleAsset"))
        }
        for (textAsset in 1..textAssetCount) {
            assetIds.add(grutSteps.createTextAsset(clientInfo.clientId!!, "Text $textAsset"))
        }
        for (imageAsset in 1..imageAssetCount) {
            val imageFormat = defaultBannerImageFormat(null)
            val imageHash = imageFormat.imageHash
            steps.bannerSteps().createBannerImageFormat(clientInfo, defaultBannerImageFormat(imageHash))
            assetIds.add(
                grutSteps.createDefaultImageAsset(
                    clientInfo.clientId!!,
                    imageHash,
                    defaultContentImageMeta(imageHash)
                )
            )
        }
        if (withVideo) {
            assetIds.add(grutSteps.createDefaultVideoAsset(clientInfo.clientId!!))
        }
        if (withHtml5) {
            assetIds.add(grutSteps.createDefaultHtml5Asset(clientInfo.clientId!!))
            creativeSteps.addDefaultHtml5Creative(
                clientInfo,
                HTML5_CREATIVE_ID
            )
        }

        val assets = grutApiService.assetGrutApi.getAssets(assetIds.toIdsLong())
        assertThat(assets)
            .`as`("Ассеты созданы в груте")
            .hasSize(assetIds.size)
        return Pair(assets, assetIds.map { createCampaignContent(contentId = it) })
    }

    fun setupTextCampaign() {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withStrategy(
                AverageCpaStrategy()
                    .withMaxWeekSum(1000.toBigDecimal())
                    .withAverageCpa(100.toBigDecimal())
            )
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)

        directCampaignId = campaignInfo.campaignId
        val uacCampaign = createYdbCampaign(
            id = directCampaignId.toString(),
            advType = AdvType.TEXT,
            keywords = KEYWORDS_FOR_CAMPAIGN,
            accountId = clientInfo.clientId!!.toString(),
            isEcom = false,
            briefSynced = false,
            zenPublisherId = "someId",
            startedAt = now(),
        )
        uacCampaignId = uacCampaign.id
        grutSteps.createTextCampaign(clientInfo, uacCampaign)

        // Создаем ассеты для заявки на кампанию
        val grutCampaignAssetIds = mutableListOf(
            grutSteps.createTitleAsset(clientInfo.clientId!!, "Title1"),
            grutSteps.createTextAsset(clientInfo.clientId!!, "Text1"),
        )
        val grutCampaignTAssets = grutApiService.assetGrutApi.getAssets(grutCampaignAssetIds.toIdsLong())
        assertThat(grutCampaignTAssets.size)
            .`as`("Все ассеты были созданы в груте для заявки на кампанию")
            .isEqualTo(grutCampaignTAssets.size)

        val grutCampaignAssets = grutCampaignAssetIds
            .map { createCampaignContent(contentId = it) }
        grutSteps.setCustomAssetLinksToCampaign(directCampaignId, grutCampaignAssets)
    }

    @Suppress("unused")
    fun parametersForTextCampaign() = arrayOf(
        arrayOf(1, 1, 0, false, false),
        arrayOf(2, 1, 0, false, false),
        arrayOf(3, 2, 1, false, false),
        arrayOf(4, 3, 2, true, false),
        arrayOf(5, 4, 3, true, true),
        arrayOf(6, 5, 4, true, true),
    )

    /**
     * Возвращает комбинацию ассетов для создания баннеров
     */
    fun getAssetsCombinations(grutTAssets: List<Schema.TAsset>): List<BannerAssets> {
        val titles = grutTAssets
            .filter { it.meta.mediaType == MediaType.EMediaType.MT_TITLE }
            .map { it.spec.title }
        val texts = grutTAssets
            .filter { it.meta.mediaType == MediaType.EMediaType.MT_TEXT }
            .map { it.spec.text }
        val imageHashes = grutTAssets
            .filter { it.meta.mediaType == MediaType.EMediaType.MT_IMAGE }
            .map { it.spec.image.directImageHash }
        val creativeIds = grutTAssets
            .filter { it.meta.mediaType == MediaType.EMediaType.MT_VIDEO }
            .mapNotNull {
                val assetMeta: Map<String, Any?> = fromJson(it.spec.video.mdsInfo.meta)
                assetMeta["creative_id"]?.toString()?.toLongOrNull()
            }
        val html5 = grutTAssets
            .filter { it.meta.mediaType == MediaType.EMediaType.MT_HTML5 }
            .mapNotNull {
                val assetMeta: Map<String, Any?> = fromJson(it.spec.html5.mdsInfo.meta)
                assetMeta["creative_id"]?.toString()?.toLongOrNull()
            }
        return getAssetsCombinations(titles, texts, imageHashes, creativeIds, html5)
    }

    /**
     * Возвращает комбинацию ассетов для создания баннеров
     */
    private fun getAssetsCombinations(
        titles: List<String>,
        texts: List<String>,
        imageHashes: List<String>?,
        creativeIds: List<Long>?,
        html5s: List<Long>? = null,
        advType: AdvType = AdvType.TEXT,
    ): List<BannerAssets> {
        val imageHashValues: List<String> = if (imageHashes.isNullOrEmpty()) listOf("") else imageHashes

        val (creativeIdValues, html5Values) =
            if (!html5s.isNullOrEmpty() && advType != AdvType.MOBILE_CONTENT) {
                Pair(
                    if (creativeIds.isNullOrEmpty()) html5s.map { it.toString() } else (html5s + creativeIds).map { it.toString() },
                    listOf(""),
                )
            } else {
                Pair(
                    if (creativeIds.isNullOrEmpty()) listOf("") else creativeIds.map { it.toString() },
                    if (html5s.isNullOrEmpty()) listOf("") else html5s.map { it.toString() },
                )
            }

        val combinations = Lists.cartesianProduct(titles, texts, imageHashValues, creativeIdValues, html5Values)
        return combinations
            .map {
                BannerAssets(
                    title = it[0],
                    text = it[1],
                    imageHash = it[2].ifEmpty { null },
                    creativeId = if (it[3].isEmpty() || HTML5_CREATIVE_ID == it[3].toLong()) null else it[3].toLong(),
                )
            }
    }

    fun createAdGroupBriefGrutModel(): AdGroupBriefGrutModel {
        val adGroupBriefIds = grutApiService.adGroupBriefGrutApi.createAdGroupBriefs(
            listOf(createAdGroupBriefGrutModel(uacCampaignId.toIdLong(), keywords = KEYWORDS_FOR_ADGROUP))
        )
        assertThat(adGroupBriefIds)
            .`as`("Групповая заявка создана в груте")
            .isNotEmpty
        return grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefIds[0])!!
    }


    /**
     * Двойной запуск джобы с изменением ассетов при повторной обработке джобой
     */
    fun doubleRunJob(
        adGroupBriefId: Long,
        titleAssetCount: Int,
        textAssetCount: Int,
        imageAssetCount: Int,
        withVideoAsset: Boolean,
        withHtml5Asset: Boolean,
    ): Pair<List<Schema.TAsset>, List<Schema.TAsset>> {
        val (grutTAssetsOld, assetsOld) = createAssets(
            titleAssetCount = 2,
            textAssetCount = 1,
            imageAssetCount = 1,
        )
        grutSteps.setCustomAssetLinksToAdGroupBrief(adGroupBriefId, assetsOld)

        updateAdsJob.withShard(shard)
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        val actualAdGroupBriefGrutModel = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)

        // Сохраняем новый список ассетов на группе
        val (grutTAssetsNew, assetsNew) = createAssets(
            titleAssetCount,
            textAssetCount,
            imageAssetCount,
            withVideoAsset,
            withHtml5Asset,
        )
        val assetsNewToSave = assetsNew
            // Добавляем старые в виде удаленных
            .plus(convertDeletedUacAssets(actualAdGroupBriefGrutModel!!.assetLinks!!))
        grutSteps.setCustomAssetLinksToAdGroupBrief(adGroupBriefId, assetsNewToSave)

        // Change briefSynced in campaign request
        val ucCampaign = grutUacCampaignService.getCampaignByDirectCampaignId(directCampaignId)!!
            .copy(briefSynced = false)
        grutUacCampaignService.updateCampaign(ucCampaign)

        // Повторно обрабатываем джобой
        updateAdsJob.processGrabbedJob(uacCampaignId, userInfo.uid)

        return Pair(grutTAssetsOld, grutTAssetsNew)
    }

    fun convertDeletedUacAssets(
        grutAssets: List<UacYdbCampaignContent>
    ): List<UacYdbCampaignContent> = grutAssets
        .map {
            it.copy(
                removedAt = now(),
                status = CampaignContentStatus.DELETED,
            )
        }

    data class BannerAssets(
        val title: String,
        val text: String,
        val imageHash: String? = null,
        val creativeId: Long? = null,
    )
}
