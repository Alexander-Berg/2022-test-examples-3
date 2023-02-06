package ru.yandex.direct.core.testing.steps.uac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting
import ru.yandex.direct.core.entity.uac.AssetConstants.ASSET_TEXT
import ru.yandex.direct.core.entity.uac.AssetConstants.ASSET_TITLE
import ru.yandex.direct.core.entity.uac.AssetConstants.DIRECT_IMAGE_HASH
import ru.yandex.direct.core.entity.uac.AssetConstants.FILENAME
import ru.yandex.direct.core.entity.uac.AssetConstants.MDS_URL
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_DESCRIPTION
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_HREF
import ru.yandex.direct.core.entity.uac.AssetConstants.SITELINK_TITLE
import ru.yandex.direct.core.entity.uac.AssetConstants.SOURCE_URL
import ru.yandex.direct.core.entity.uac.AssetConstants.THUMB
import ru.yandex.direct.core.entity.uac.AssetConstants.VIDEO_DURATION
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.TRACKING_URL
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toCampaignSpec
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.createAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.UacStrategyData
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateRandomIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCampaignMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCampaignMeasurerSystem
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.samples.CONTENT_HTML5_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_IMAGE_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_VIDEO_META
import ru.yandex.direct.core.grut.api.AdGroupBriefGrutModel
import ru.yandex.direct.core.grut.api.BriefAdGroup
import ru.yandex.direct.core.grut.api.BriefBanner
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.Html5AssetGrut
import ru.yandex.direct.core.grut.api.ImageAssetGrut
import ru.yandex.direct.core.grut.api.SitelinkAssetGrut
import ru.yandex.direct.core.grut.api.TextAssetGrut
import ru.yandex.direct.core.grut.api.TitleAssetGrut
import ru.yandex.direct.core.grut.api.VideoAssetGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestClients.TEST_TIN
import ru.yandex.direct.core.testing.data.TestClients.TEST_TIN_TYPE
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.CampaignsMobileContentSteps
import ru.yandex.direct.core.testing.steps.MobileAppSteps
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.Strategy
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.randomNegativeLong
import ru.yandex.direct.utils.DateTimeUtils
import ru.yandex.grut.objects.proto.Asset.TMdsInfo
import ru.yandex.grut.objects.proto.AssetLink.TAssetLink
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinks
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinksStatuses
import ru.yandex.grut.objects.proto.Banner.EBannerSource
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.client.Schema
import ru.yandex.grut.objects.proto.client.Schema.TCampaignMeta
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit

@Component
@Lazy
class GrutSteps(
    @Autowired private val campaignSteps: CampaignSteps,
    @Autowired private val campaignsMobileContentSteps: CampaignsMobileContentSteps,
    @Autowired private val mobileAppSteps: MobileAppSteps,
    @Autowired private val uacYdbAppInfoRepository: UacYdbAppInfoRepository,
    @Autowired private val uacCampaignSteps: UacCampaignSteps,
    @Autowired private val grutApiService: GrutApiService,
) {
    data class GrutCampaignInfo(
        val uacCampaign: UacYdbCampaign,
        val campaignInfo: CampaignInfo,
    )

    fun createDefaultTextAsset(clientId: ClientId): String {
        return createTextAsset(clientId, ASSET_TEXT)
    }

    fun createTextAsset(clientId: ClientId, text: String): String {
        return createTextAssets(clientId, listOf(text))[0]
    }

    fun createTextAssets(clientId: ClientId, texts: Collection<String>): List<String> {
        val assets = texts.map {
            TextAssetGrut(
                // генерим очень большой id, который не влазит в знаковый Long
                // чтобы проверить обратную совместимость
                id = randomNegativeLong(),
                clientId = clientId.asLong(),
                text = it
            )
        }
        return grutApiService.assetGrutApi.createObjects(assets).map { it.toIdString() }
    }

    fun createDefaultTitleAsset(clientId: ClientId): String {
        return createTitleAsset(clientId, ASSET_TITLE)
    }

    fun createTitleAsset(clientId: ClientId, title: String = ASSET_TITLE): String {
        return grutApiService.assetGrutApi.createObject(TitleAssetGrut(id = randomNegativeLong(), clientId = clientId.asLong(), title = title)).toIdString()
    }

    fun createSitelinkAsset(clientId: ClientId): String {
        return createSitelinkAsset(clientId, Sitelink(SITELINK_TITLE, SITELINK_HREF, SITELINK_DESCRIPTION))
    }

    fun createSitelinkAsset(clientId: ClientId, sitelink: Sitelink): String {
        return grutApiService.assetGrutApi.createObject(
            SitelinkAssetGrut(
                id = randomNegativeLong(),
                clientId = clientId.asLong(),
                title = sitelink.title,
                href = sitelink.href,
                description = sitelink.description,
            )
        ).toIdString()
    }

    fun createDefaultImageAsset(
        clientId: ClientId,
        imageHash: String = DIRECT_IMAGE_HASH,
        imageMeta: String = CONTENT_IMAGE_META,
        sourceUrl: String = SOURCE_URL,
    ): String {
        return grutApiService.assetGrutApi.createObject(
            ImageAssetGrut(
                id = randomNegativeLong(),
                clientId = clientId.asLong(),
                mdsInfo = TMdsInfo.newBuilder().apply {
                    thumb = THUMB
                    this.sourceUrl = sourceUrl
                    mdsUrl = MDS_URL
                    filename = FILENAME
                    meta = imageMeta
                }.build(),
                imageHash = imageHash
            )
        ).toIdString()
    }

    fun createDefaultVideoAsset(
        clientId: ClientId,
        videoDuration: Int = VIDEO_DURATION,
        sourceUrl: String = SOURCE_URL,
        mdsUrl: String = MDS_URL,
    ): String {
        return grutApiService.assetGrutApi.createObject(
            VideoAssetGrut(
                id = randomNegativeLong(),
                clientId = clientId.asLong(),
                mdsInfo = TMdsInfo.newBuilder().apply {
                    thumb = THUMB
                    this.sourceUrl = sourceUrl
                    this.mdsUrl = mdsUrl
                    filename = FILENAME
                    meta = CONTENT_VIDEO_META
                }.build(),
                duration = videoDuration
            )
        ).toIdString()
    }

    fun createAndGetTextCampaign(
        clientInfo: ClientInfo,
        isEcom: Boolean? = false,
        feedId: Long? = null,
        keywords: List<String>? = null,
        contentFlags: Map<String, Any>? = null,
    ): GrutCampaignInfo {
        return createTextCampaignInternal(clientInfo, TargetStatus.STARTED, null, isEcom, feedId, keywords, contentFlags)
    }

    fun createTextCampaign(
        clientInfo: ClientInfo,
        targetStatus: TargetStatus = TargetStatus.STARTED,
        directCampaignStatus: DirectCampaignStatus = DirectCampaignStatus.DRAFT,
        isEcom: Boolean? = false,
        feedId: Long? = null,
        keywords: List<String>? = null,
        contentFlags: Map<String, Any>? = null,
        counterIds: List<Int>? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
    ): Long {
        return createTextCampaignInternal(
            clientInfo, targetStatus, directCampaignStatus, isEcom,
            feedId, keywords, contentFlags, counterIds, startedAt = startedAt
        ).uacCampaign.id.toIdLong()
    }

    fun createTextCampaign(
        clientInfo: ClientInfo,
        isEcom: Boolean? = false,
        feedId: Long? = null,
        keywords: List<String>? = null,
        contentFlags: Map<String, Any>? = null,
        retargetingCondition: UacRetargetingCondition? = null,
    ): Long {
        return createTextCampaignInternal(
            clientInfo, TargetStatus.STARTED, null, isEcom, feedId, keywords, contentFlags,
            retargetingCondition = retargetingCondition,
        ).uacCampaign.id.toIdLong()
    }

    fun createTextCampaign(
        clientInfo: ClientInfo,
        isEcom: Boolean? = false,
        feedId: Long? = null,
        keywords: List<String>? = null,
        contentFlags: Map<String, Any>? = null,
        campaignSource: CampaignSource = CampaignSource.UAC,
        strategy: Strategy? = TestCampaigns.averageCpaStrategy()
    ): Long {
        return createTextCampaignInternal(
            clientInfo, TargetStatus.STARTED, null, isEcom,
            feedId, keywords, contentFlags, null, campaignSource, strategy
        ).uacCampaign.id.toIdLong()
    }

    private fun createTextCampaignInternal(
        clientInfo: ClientInfo,
        targetStatus: TargetStatus = TargetStatus.STOPPED,
        directCampaignStatus: DirectCampaignStatus? = null,
        isEcom: Boolean? = false,
        feedId: Long? = null,
        keywords: List<String>? = null,
        contentFlags: Map<String, Any>? = null,
        counterIds: List<Int>? = null,
        campaignSource: CampaignSource = CampaignSource.UAC,
        strategy: Strategy? = TestCampaigns.averageCpaStrategy(),
        retargetingCondition: UacRetargetingCondition? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
    ): GrutCampaignInfo {
        val directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(strategy)
            .withSource(campaignSource)
        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)

        val uacCampaign = createYdbCampaign(
            targetStatus = targetStatus,
            directCampaignStatus = directCampaignStatus,
            startedAt = startedAt,
            id = campaignInfo.campaignId.toIdString(),
            appId = null,
            isEcom = isEcom,
            feedId = feedId,
            keywords = keywords,
            contentFlags = contentFlags,
            counterIds = counterIds,
            retargetingCondition = retargetingCondition,
        )
        return GrutCampaignInfo(createTextCampaign(clientInfo, uacCampaign), campaignInfo)
    }

    fun createTextCampaign(
        clientInfo: ClientInfo,
        uacCampaign: UacYdbCampaign
    ): UacYdbCampaign {
        val campaignSpec = toCampaignSpec(uacCampaign)

        val campaignId = grutApiService.briefGrutApi.createObject(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = uacCampaign.id.toIdLong()
                    campaignType = Campaign.ECampaignTypeOld.CTO_TEXT
                    this.clientId = clientInfo.clientId!!.asLong()
                }.build()
                spec = campaignSpec
            }.build()
        )

        return uacCampaign.copy(id = campaignId.toIdString())
    }

    fun createMobileAppCampaign(
        clientInfo: ClientInfo,
        campaignId: String = UacYdbUtils.generateUniqueRandomId(),
        createInDirect: Boolean = false,
        briefSynced: Boolean? = null,
        impressionUrl: String? = null,
        trackingUrl: String? = TRACKING_URL,
        appId: String? = null,
        accountId: String = UacYdbUtils.generateUniqueRandomId(),
        createImageContent: Boolean = false,
        createdAt: LocalDateTime = now().truncatedTo(ChronoUnit.SECONDS),
        retargetingCondition: UacRetargetingCondition? = null,
        strategy: Strategy = TestCampaigns.manualStrategy(),
        campaignSource: CampaignSource = CampaignSource.UAC,
        cpa: BigDecimal? = BigDecimal.valueOf(100000000L, 6),
        altAppStores: Set<MobileAppAlternativeStore>? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
        audienceSegmentsSynchronized: Boolean? = null,
        keywords: List<String>? = null,
        minusKeywords: List<String>? = null,
    ) = createAndGetMobileAppCampaign(
        clientInfo = clientInfo,
        campaignId = campaignId,
        createInDirect = createInDirect,
        briefSynced = briefSynced,
        impressionUrl = impressionUrl,
        trackingUrl = trackingUrl,
        appId = appId,
        accountId = accountId,
        createImageContent = createImageContent,
        createdAt = createdAt,
        retargetingCondition = retargetingCondition,
        strategy = strategy,
        campaignSource = campaignSource,
        cpa = cpa,
        altAppStores = altAppStores,
        startedAt = startedAt,
        audienceSegmentsSynchronized = audienceSegmentsSynchronized,
        keywords = keywords,
        minusKeywords = minusKeywords,
    ).id.toIdLong()

    fun createAndGetMobileAppCampaign(
        clientInfo: ClientInfo,
        campaignId: String = UacYdbUtils.generateUniqueRandomId(),
        createInDirect: Boolean = false,
        briefSynced: Boolean? = null,
        impressionUrl: String? = null,
        trackingUrl: String? = TRACKING_URL,
        appId: String? = null,
        accountId: String = UacYdbUtils.generateUniqueRandomId(),
        createImageContent: Boolean = false,
        createdAt: LocalDateTime = now().truncatedTo(ChronoUnit.SECONDS),
        retargetingCondition: UacRetargetingCondition? = null,
        strategy: Strategy = TestCampaigns.manualStrategy(),
        campaignSource: CampaignSource = CampaignSource.UAC,
        cpa: BigDecimal? = BigDecimal.valueOf(100000000L, 6),
        altAppStores: Set<MobileAppAlternativeStore>? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
        audienceSegmentsSynchronized: Boolean? = null,
        keywords: List<String>? = null,
        minusKeywords: List<String>? = null,
    ): UacYdbCampaign {
        var ydbAppInfo: UacYdbAppInfo? = null
        if (appId == null) {
            ydbAppInfo = defaultAppInfo()
            uacYdbAppInfoRepository.saveAppInfo(ydbAppInfo)
        }

        val uacCampaignId = if (!createInDirect) {
            campaignId
        } else {
            val directCampaign = TestCampaigns.activeMobileAppCampaign(null, null)
                .withOrderId(0L)
                .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
                .withStrategy(strategy)
                .withSource(campaignSource)

            val mobileAppInfo = mobileAppSteps.createMobileApp(clientInfo, STORE_URL)
            val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
            campaignsMobileContentSteps.createCampaignsMobileContent(
                campaignInfo.shard,
                campaignInfo.campaignId,
                mobileAppInfo.mobileAppId,
                setOf(MobileAppDeviceTypeTargeting.PHONE, MobileAppDeviceTypeTargeting.TABLET),
                setOf(MobileAppNetworkTargeting.WI_FI, MobileAppNetworkTargeting.CELLULAR),
                altAppStores
            )
            campaignInfo.campaignId.toString()
        }
        val imageContent = createDefaultImageAsset(clientInfo.clientId!!)
        val assetLinks = if (createImageContent) {
            listOf(
                createCampaignContent(id = imageContent, campaignId = uacCampaignId, contentId = imageContent)
            )
        } else {
            listOf()
        }
        val uacCampaign = createYdbCampaign(
            accountId = accountId,
            startedAt = startedAt,
            briefSynced = briefSynced,
            impressionUrl = impressionUrl,
            trackingUrl = trackingUrl,
            appId = appId ?: ydbAppInfo!!.id,
            assetLinks = assetLinks,
            createdAt = createdAt,
            retargetingCondition = retargetingCondition,
            cpa = cpa,
            audienceSegmentsSynchronized = audienceSegmentsSynchronized,
            keywords = keywords,
            minusKeywords = minusKeywords,
        )
        val campaignSpec = toCampaignSpec(uacCampaign)

        val id = grutApiService.briefGrutApi.createObject(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = uacCampaignId.toIdLong()
                    campaignType = Campaign.ECampaignTypeOld.CTO_MOBILE_APP
                    this.clientId = clientInfo.clientId!!.asLong()
                    creationTime = uacCampaign.createdAt.atZone(DateTimeUtils.MSK).toEpochSecond() * 1_000
                }.build()
                spec = campaignSpec
            }.build())
        return uacCampaign.copy(id = id.toIdString())
    }

    fun createEcomUcCampaign(
        clientInfo: ClientInfo,
        feedId: Long? = null,
        keywords: List<String>? = null,
        targetStatus: TargetStatus = TargetStatus.STARTED,
        directCampaignStatus: DirectCampaignStatus = DirectCampaignStatus.CREATED,
        counterIds: List<Int>? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
        useNewBackend: Boolean = false
    ): Pair<Long, EcomUcSubcampaignsInfo> {
        val campaignId = createTextCampaign(
            clientInfo,
            targetStatus,
            directCampaignStatus,
            counterIds = counterIds,
            isEcom = true,
            feedId = feedId,
            keywords = keywords,
            startedAt = startedAt,
        )
        return if (useNewBackend) {
            campaignId to EcomUcSubcampaignsInfo(smartInfo = CampaignInfo(), dynamicInfo = CampaignInfo())
        } else {
            val ecomSubcampaignsInfo = uacCampaignSteps.createEcomUcSubcampaigns(clientInfo, campaignId)
            campaignId to ecomSubcampaignsInfo
        }
    }

    fun createCpmBannerCampaign(
        clientInfo: ClientInfo,
        uacCampaign: UacYdbCampaign
    ): UacYdbCampaign {
        val campaignSpec = toCampaignSpec(uacCampaign)

        val campaignId = grutApiService.briefGrutApi.createObject(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = uacCampaign.id.toIdLong()
                    campaignType = Campaign.ECampaignTypeOld.CTO_CPM_BANNER
                    this.clientId = clientInfo.clientId!!.asLong()
                }.build()
                spec = campaignSpec
            }.build())

        return uacCampaign.copy(id = campaignId.toIdString())
    }

    private fun createCpmBannerCampaignInternal(
        clientInfo: ClientInfo,
        deviceTypes: Set<DeviceType> = setOf(DeviceType.PHONE_ANDROID, DeviceType.DESKTOP),
        intventoryTypes: Set<InventoryType>? = setOf(InventoryType.INAPP, InventoryType.INSTREAM),
        videosAreNonSkippable: Boolean? = null,
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
    ): UacYdbCampaign {
        val directCampaign = TestCampaigns.activeCpmBannerCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.autobudgetMaxImpressionsStrategy())

        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)

        val ydbCampaign = createYdbCampaign(
            advType = AdvType.CPM_BANNER,
            startedAt = startedAt,
            id = campaignInfo.campaignId.toIdString(),
            strategy = UacStrategy(
                UacStrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
                UacStrategyData(
                    BigDecimal.ZERO,
                    true,
                    BigDecimal("345.3"),
                    LocalDate.now().plusMonths(6),
                    LocalDate.now(),
                    BigDecimal("154563.5"), BigDecimal.valueOf(100),
                    0
                )
            ),
            campaignMeasurers = listOf(UacCampaignMeasurer(UacCampaignMeasurerSystem.MOAT, "{}")),
            inventoryTypes = intventoryTypes,
            deviceTypes = deviceTypes,
            videosAreNonSkippable = videosAreNonSkippable,
        )
        return createCpmBannerCampaign(clientInfo, ydbCampaign)
    }

    fun createCpmBannerCampaign(
        clientInfo: ClientInfo,
        deviceTypes: Set<DeviceType> = setOf(DeviceType.PHONE_ANDROID, DeviceType.DESKTOP),
        intventoryTypes: Set<InventoryType>? = setOf(InventoryType.INAPP, InventoryType.INSTREAM),
        startedAt: LocalDateTime? = now().truncatedTo(ChronoUnit.SECONDS),
    ): Long {
        return createCpmBannerCampaignInternal(clientInfo, deviceTypes, intventoryTypes, startedAt = startedAt).id.toIdLong()
    }

    fun createAndGetCpmBannerCampaign(
        clientInfo: ClientInfo,
        deviceTypes: Set<DeviceType> = setOf(DeviceType.PHONE_ANDROID, DeviceType.DESKTOP),
        inventoryTypes: Set<InventoryType> = setOf(InventoryType.INAPP, InventoryType.INSTREAM),
        videosAreNonSkippable: Boolean? = null,
    ): UacYdbCampaign {
        return createCpmBannerCampaignInternal(clientInfo, deviceTypes, inventoryTypes, videosAreNonSkippable)
    }

    fun createDefaultHtml5Asset(
        clientId: ClientId,
        sourceUrl: String = SOURCE_URL,
        mdsUrl: String = MDS_URL,
    ): String {
        return grutApiService.assetGrutApi.createObject(
            Html5AssetGrut(
                id = generateRandomIdLong(),
                clientId = clientId.asLong(),
                mdsInfo = TMdsInfo.newBuilder().apply {
                    thumb = THUMB
                    this.sourceUrl = sourceUrl
                    this.mdsUrl = mdsUrl
                    filename = FILENAME
                    meta = CONTENT_HTML5_META
                }.build()
            )
        ).toIdString()
    }

    fun createClient(
        clientId: ClientId = ClientId.fromLong(generateRandomIdLong()),
        name: String? = null,
        chiefUid: Long? = null,
        creationTime: Long = Instant.now().toEpochMilli()
    ): ClientId {
        grutApiService.clientGrutDao.createOrUpdateClient(
                ClientGrutModel(client =
                Client()
                    .withId(clientId.asLong())
                    .withChiefUid(chiefUid)
                    .withName(name)
                    .withAutoOverdraftLimit(BigDecimal.valueOf(0))
                    .withOverdraftLimit(BigDecimal.valueOf(0))
                    .withDebt(BigDecimal.valueOf(0))
                    .withTin(TEST_TIN)
                    .withTinType(TEST_TIN_TYPE)
                    .withCreateDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(creationTime / 1000), DateTimeUtils.MSK)),
                    ndsHistory = listOf()
            )
        )
        return clientId
    }


    fun createClient(clientInfo: ClientInfo): ClientId {
        return createClient(clientInfo.clientId!!, clientInfo.client?.name)
    }

    fun createAdGroup(campaignId: Long, adGroupId: Long = generateRandomIdLong()): Long {
        grutApiService.briefAdGroupGrutApi.createOrUpdateBriefAdGroup(
            BriefAdGroup(
                id = adGroupId,
                briefId = campaignId,
            )
        )
        return adGroupId
    }

    fun createAdGroupBrief(
        campaignId: Long,
        retargetingCondition: UacRetargetingCondition? = null,
    ) = grutApiService.adGroupBriefGrutApi.createObject(
            createAdGroupBriefGrutModel(
                campaignId,
                retargetingCondition = retargetingCondition,
            )
        )

    fun createBanner(
        campaignId: Long,
        adGroupId: Long,
        bannerId: Long = generateRandomIdLong(),
        assetIds: List<String>? = null,
        assetLinkIds: List<String>? = null,
        status: EBannerStatus = EBannerStatus.BSS_CREATED
    ): Long {
        return grutApiService.briefBannerGrutApi.createObject(
            BriefBanner(
                id = bannerId,
                adGroupId = adGroupId,
                briefId = campaignId,
                source = EBannerSource.BS_DIRECT,
                status = status,
                assetIds = assetIds?.map { it.toIdLong() } ?: listOf(),
                assetLinksIds = assetLinkIds?.map { it.toIdLong() } ?: listOf()
            )
        )
    }

    fun setAssetLinksToCampaign(campaignId: Long, assetIds: List<String>) {
        val campaign = grutApiService.briefGrutApi.getBrief(campaignId)
        val campaignSpec = campaign!!.spec
        val assetLinks = assetIds.map { TAssetLink.newBuilder().setAssetId(it.toIdLong()).build() }
        val assetLinksStatuses = TAssetLinksStatuses.newBuilder()
            .addAllLinkStatuses(assetIds.map {
                TAssetLinkStatus.newBuilder().setAssetId(it.toIdLong()).setStatus(EAssetLinkStatus.ALS_ACTIVE).build()
            }).build()
        grutApiService.briefGrutApi.updateBriefFull(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = campaignId
                }.build()
                spec = campaignSpec.toBuilder().setBriefAssetLinksStatuses(assetLinksStatuses).setBriefAssetLinks(
                    TAssetLinks.newBuilder().addAllLinks(assetLinks).build()
                ).build()
            }.build()
        )
    }

    fun setCustomAssetIdsToCampaign(campaignId: Long, assetIds: List<String>) {
        val assets = assetIds.map { createCampaignContent(contentId = it) }
        setCustomAssetLinksToCampaign(campaignId, assets)
    }

    fun setCustomAssetLinksToCampaign(campaignId: Long, campaignContents: List<UacYdbCampaignContent>) {
        val campaign = grutApiService.briefGrutApi.getBrief(campaignId)
        val campaignSpec = campaign!!.spec
        val assetLinks = TAssetLinks.newBuilder().apply {
            addAllLinks(campaignContents.map {
                TAssetLink.newBuilder().apply {
                    id = it.id.toIdLong()
                    assetId = it.contentId!!.toIdLong()
                    it.removedAt?.let { this.removeTime = UacYdbUtils.toEpochSecond(it).toInt() }
                }.build()
            })
        }
        val assetLinksStatuses = TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(campaignContents.map {
                TAssetLinkStatus.newBuilder().apply {
                    status = EAssetLinkStatus.ALS_ACTIVE
                    assetLinkId = it.id.toIdLong()
                    assetId = it.contentId!!.toIdLong()
                }.build()
            })
        }.build()

        grutApiService.briefGrutApi.updateBriefFull(
            Schema.TCampaign.newBuilder().apply {
                meta = TCampaignMeta.newBuilder().apply {
                    id = campaignId
                }.build()
                spec = campaignSpec.toBuilder()
                    .setBriefAssetLinksStatuses(assetLinksStatuses)
                    .setBriefAssetLinks(assetLinks)
                    .build()
            }.build()
        )
    }

    fun setCustomAssetIdsToAdGroupBrief(adGroupBriefId: Long, assetIds: List<String>) {
        val assets = assetIds.map { createCampaignContent(contentId = it) }
        setCustomAssetLinksToAdGroupBrief(adGroupBriefId, assets)
    }

    fun setCustomAssetLinksToAdGroupBrief(adGroupBriefId: Long, uacAssets: List<UacYdbCampaignContent>) {
        val adGroupBrief: AdGroupBriefGrutModel? = grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefId)
        assertThatKt(adGroupBrief != null)
            .`as`("Групповая заявка есть в груте")
            .isTrue
        grutApiService.adGroupBriefGrutApi.createOrUpdateAdGroupBrief(
            adGroupBrief!!.copy(
                assetLinks = uacAssets,
            )
        )
    }

    fun getCampaign(campaignId: Long): UacYdbCampaign {
        val campaignResponse = grutApiService.briefGrutApi.getBrief(campaignId)
        return campaignResponse!!.toUacYdbCampaign()
    }
}
