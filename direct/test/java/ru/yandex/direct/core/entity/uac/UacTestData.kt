package ru.yandex.direct.core.entity.uac

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.apache.commons.lang.RandomStringUtils
import ru.yandex.direct.avatars.client.model.AvatarInfo
import ru.yandex.direct.avatars.client.model.answer.ImageSize
import ru.yandex.direct.avatars.client.model.answer.SmartArea
import ru.yandex.direct.avatars.config.AvatarsConfig
import ru.yandex.direct.avatars.config.ServerConfig
import ru.yandex.direct.bannerstorage.client.model.BusinessType
import ru.yandex.direct.bannerstorage.client.model.Creative
import ru.yandex.direct.bannerstorage.client.model.CreativeGroup
import ru.yandex.direct.bannerstorage.client.model.CreativeLayoutCode
import ru.yandex.direct.bannerstorage.client.model.CreativePreview
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagData
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType
import ru.yandex.direct.core.entity.uac.UacCommonUtils.CREATIVE_ID_KEY
import ru.yandex.direct.core.entity.uac.model.AccountFeatures
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.model.direct_ad_group.DirectAdGroupStatus
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentStatus
import ru.yandex.direct.core.entity.uac.model.direct_content.DirectContentType
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCampaignMeasurer
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAd
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectContent
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.CONTENT_HTML5_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_IMAGE_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_VIDEO_META
import ru.yandex.direct.core.entity.uac.samples.CONTENT_VIDEO_NON_SKIPPABLE_META
import ru.yandex.direct.core.grut.api.AdGroupBriefGrutModel
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.tracing.util.TraceUtil
import ru.yandex.direct.utils.JsonUtils.toJson
import ru.yandex.direct.utils.fromJson

object AssetConstants {
    const val THUMB = "https://avatars.mds.yandex.net/get-uac-test/4220162/121e631f-db50-463e-9fd0-dfe799c78df8/thumb"
    const val SOURCE_URL = "sourceUrl"
    const val MDS_URL = "mdsUrl"
    const val FILENAME = "filename"
    const val DIRECT_IMAGE_HASH = "directImageHash"
    const val VIDEO_DURATION = 15
    const val SITELINK_TITLE = "Sitelink title"
    const val SITELINK_HREF = "Sitelink href"
    const val SITELINK_DESCRIPTION = "Sitelink description"
    const val ASSET_TEXT = "Asset text"
    const val ASSET_TITLE = "Asset title"
}

const val STORE_URL = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=ru.yandex.music"
const val TRACKING_URL = "https://redirect.appmetrica.yandex.com/serve/1179849830915165578?" +
    "c=ya_direct&c=ya_direct&google_aid_sha1={GOOGLE_AID_LC_SH1_HEX}&android_id_sha1={ANDROID_ID_LC_SH1_HEX}&" +
    "device_type={DEVICE_TYPE}&source_type={STYPE}&source={SRC}&google_aid={google_aid}&click_id={logid}&" +
    "search_term={PHRASE}&region_name={REGN_BS}&phrase_id={PHRASE_EXPORT_ID}&android_id={ANDROID_ID_LC}&" +
    "position_type={PTYPE}&campaign_id=54494649"
const val TRACKING_URL_WITH_UNICODE = "https://control.kochava.com/v1/cpi/click?" +
    "campaign_id=koidtandroidus205652e7fe894452f471b27363e9ea&network_id=221&adid=&device_id=&device_id_type=&" +
    "site_id=p277-s322-cf1a3fda0&append_app_conv_trk_params=1&creative_id=Call Home Span 480x320"
const val TRACKING_URL_WITH_UNICODE_ENCODED = "https://control.kochava.com/v1/cpi/click?" +
    "campaign_id=koidtandroidus205652e7fe894452f471b27363e9ea&network_id=221&adid=&device_id=&device_id_type=&" +
    "site_id=p277-s322-cf1a3fda0&append_app_conv_trk_params=1&creative_id=Call+Home+Span+480x320"

const val APP_ID = "com.airealmobile.livingwateracademey_34889"
const val STORE_URL_FOR_APP_ID =
    "https://play.google.com/store/apps/details?id=com.airealmobile.livingwateracademey_34889"

const val OZON_DOMAIN = "www.ozon.ru"
const val OZON_SHOP_URL = "https://www.ozon.ru/seller/linwu-272701/"
const val MARKET_DOMAIN = "market.yandex.ru"
const val MARKET_SHOP_URL = "https://market.yandex.ru/offer/EbL7v5U5_IqS6yOMZboB0g?cpc=fMA5j5q7LVasNq_wKFWIKJQW1k0cgr" +
    "cH_EJKXxE8ALGbZnX1PQ-oVNDu3mVIsvdwy7uHbUa1q69mlbjDfK25RqU6SG5b25Q3FAz1wSKqgHaDKOLeTsAxDxq_PdXBvmGwSlcQSmyJFPhwD_" +
    "-fCWuo0msVoa0uH86Z4L4Sfzc7KcorOOpYDdqmPYF1i1SIAkpQ&hid=278345&hyperid=963627748&lr=2&modelid=963627748&nid=18071" +
    "953&show-uid=16421710046448062604800001&businessId=835158"
const val MARKET_BUSINESS_SHOP_URL = "https://market.yandex.ru/business--store/835158"
const val MARKET_STORE_SHOP_URL = "https://market.yandex.ru/store--store?businessId=835158"
const val MARKET_ACCOUNT_URL = "https://partner.market.yandex.ru/business/835158/direct-bonus"

const val GOOGLE_PLAY_BAD_URL = "https://play.google.com/store/search?q=яндекс&c=apps&hl=ru"
const val ITUNES_BAD_URL = "https://apps.apple.com/ru/developer/yandex-llc/id308094652"

const val VALID_TRACKING_URL = "https://app.adjust.com/xorrw6b?campaign=57018349_Android_Network&idfa={IDFA_UC}&" +
    "gps_adid={GOOGLE_AID_LC}&campaign_id=57018349&creative_id=9886512054&publisher_id={GBID}&ya_click_id={TRACKID}"
const val VALID_REDIRECT_URL = "https://apps.apple.com/app/id1327721379?mt=8&pt=118743718&ct=Yandex+Direct"
const val VALID_APP_ID = "id1327721379"

fun defaultAppInfo(
    id: String = generateUniqueRandomId(),
    appId: String = APP_ID,
    bundleId: String? = APP_ID,
    language: String = "ru",
    region: String = "ru",
    platform: Platform = Platform.ANDROID,
    source: Store = Store.GOOGLE_PLAY,
    data: String = ANDROID_APP_INFO_DATA,
) = UacYdbAppInfo(
    id = id,
    pkVersion = 1,
    appId = appId,
    bundleId = bundleId,
    language = language,
    region = region,
    platform = platform,
    source = source,
    data = data,
    updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) - Duration.ofHours(1)
)

fun createDefaultHtml5Content(
    id: String = generateUniqueRandomId(),
    accountId: String = generateUniqueRandomId(),
    creativeId: Long? = randomPositiveLong(),
) = UacYdbContent(
    id = id,
    ownerId = null,
    type = MediaType.HTML5,
    sourceUrl = "https://storage.mds.yandex.net/get-bstor/3935473/d97141c5-6096-46e0-8a22-ea101f33f08d.zip",
    thumb = "https://avatars.mds.yandex.net/get-html5/42/name/orig",
    mdsUrl = "https://storage.mds.yandex.net/get-bstor/3935473/d97141c5-6096-46e0-8a22-ea101f33f08d.zip",
    meta = fromJson(modifyMeta(CONTENT_HTML5_META, CREATIVE_ID_KEY, creativeId)),
    videoDuration = null,
    filename = "index.html.ok.zip",
    accountId = accountId,
    directImageHash = null,
)

fun createDefaultImageContent(
    id: String = generateUniqueRandomId(),
    accountId: String = generateUniqueRandomId(),
    imageHash: String = RandomStringUtils.randomAlphanumeric(22),
    meta: Map<String, Any?> = fromJson(CONTENT_IMAGE_META),
    thumb: String = "https://avatars.mds.yandex.net/get-uac-test/4220162/121e631f-db50-463e-9fd0-dfe799c78df8/thumb",
    sourceUrl: String = "https://im0-tub-ru.yandex.net/i?id=c28da98e47c918fd487e5522de31c628-l&n=13",
) = UacYdbContent(
    id = id,
    ownerId = null,
    type = MediaType.IMAGE,
    thumb = thumb,
    sourceUrl = sourceUrl,
    mdsUrl = null,
    meta = meta,
    videoDuration = null,
    filename = null,
    accountId = accountId,
    directImageHash = imageHash,
)

fun createDefaultVideoContent(
    id: String = generateUniqueRandomId(),
    accountId: String = generateUniqueRandomId(),
    creativeId: Long? = randomPositiveLong(),
    nonSkippable: Boolean = false,
) = UacYdbContent(
    id = id,
    ownerId = null,
    type = MediaType.VIDEO,
    thumb = "https://avatars.mds.yandex.net/get-uac-test/4220162/e2ab8f51-6f08-4539-acb2-394c87c51aab/s4x3",
    sourceUrl = "https://www.youtube.com/embed/XyjrY6JLYaU?ps=play&vq=large&rel=0&autohide=1&showinfo=0",
    mdsUrl = "https://storage.mds.yandex.net/get-bstor/3908697/16dbe953-6300-47f6-89c2-b85d3fd36477.mp4",
    meta = fromJson(
        modifyMeta(
            if (nonSkippable) CONTENT_VIDEO_NON_SKIPPABLE_META else CONTENT_VIDEO_META,
            CREATIVE_ID_KEY,
            creativeId
        )
    ),
    videoDuration = 29,
    filename = null,
    accountId = accountId,
    directImageHash = null,
)

fun createDirectAdGroup(
    id: String = generateUniqueRandomId(),
    directCampaignId: String = generateUniqueRandomId(),
    status: DirectAdGroupStatus = DirectAdGroupStatus.CREATED,
    directAdGroupId: Long,
) = UacYdbDirectAdGroup(
    id = id,
    directCampaignId = directCampaignId,
    status = status,
    directAdGroupId = directAdGroupId,
)

fun createDirectAd(
    titleContentId: String = generateUniqueRandomId(),
    textContentId: String = generateUniqueRandomId(),
    directContentId: String? = null,
    directAdGroupId: String? = null,
    directAdId: Long? = null,
    directImageContentId: String? = null,
    directVideoContentId: String? = null,
    directHtml5ContentId: String? = null,
    status: DirectAdStatus = DirectAdStatus.CREATED,
) = UacYdbDirectAd(
    id = generateUniqueRandomId(),
    status = status,
    titleContentId = titleContentId,
    textContentId = textContentId,
    directContentId = directContentId,
    directAdGroupId = directAdGroupId,
    directAdId = directAdId,
    directImageContentId = directImageContentId,
    directVideoContentId = directVideoContentId,
    directHtml5ContentId = directHtml5ContentId,
)

fun createDirectContent(
    id: String = generateUniqueRandomId(),
    status: DirectContentStatus = DirectContentStatus.CREATED,
    type: DirectContentType = DirectContentType.IMAGE,
    directImageHash: String? = null,
    directVideoId: Long? = null,
    directHtml5Id: Long? = null,
) = UacYdbDirectContent(
    id = id,
    status = status,
    type = type,
    directImageHash = directImageHash,
    directVideoId = directVideoId,
    directHtml5Id = directHtml5Id,
)

fun createImageCampaignContent(
    campaignId: String = generateUniqueRandomId(),
    contentId: String = generateUniqueRandomId(),
    order: Int = 0,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    removedAt: LocalDateTime? = null,
    status: CampaignContentStatus = CampaignContentStatus.CREATED,
    rejectReasons: List<ModerationDiagData>? = null,
) = createMediaCampaignContent(
    campaignId = campaignId,
    type = MediaType.IMAGE,
    contentId = contentId,
    order = order,
    createdAt = createdAt,
    status = status,
    rejectReasons = rejectReasons,
    removedAt = removedAt,
)

fun createMediaCampaignContent(
    campaignId: String = generateUniqueRandomId(),
    type: MediaType,
    contentId: String = generateUniqueRandomId(),
    order: Int = 0,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    removedAt: LocalDateTime? = null,
    status: CampaignContentStatus = CampaignContentStatus.CREATED,
    rejectReasons: List<ModerationDiagData>? = null,
) = UacYdbCampaignContent(
    campaignId = campaignId,
    type = type,
    contentId = contentId,
    order = order,
    createdAt = createdAt,
    status = status,
    rejectReasons = rejectReasons,
    removedAt = removedAt,
    text = null,
)

fun createTextCampaignContent(
    campaignId: String = generateUniqueRandomId(),
    text: String = RandomStringUtils.randomAlphanumeric(10),
    order: Int = 0,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    removedAt: LocalDateTime? = null,
    status: CampaignContentStatus = CampaignContentStatus.CREATED,
    rejectReasons: List<ModerationDiagData>? = null,
    mediaType: MediaType = MediaType.TEXT,
) = UacYdbCampaignContent(
    campaignId = campaignId,
    type = mediaType,
    text = text,
    order = order,
    createdAt = createdAt,
    removedAt = removedAt,
    status = status,
    rejectReasons = rejectReasons,
)

fun createSitelinkCampaignContent(
    campaignId: String = generateUniqueRandomId(),
    sitelink: Sitelink = Sitelink("title", "href", "description"),
    order: Int = 0,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    removedAt: LocalDateTime? = null,
    status: CampaignContentStatus = CampaignContentStatus.CREATED,
    rejectReasons: List<ModerationDiagData>? = null,
) = UacYdbCampaignContent(
    campaignId = campaignId,
    type = MediaType.SITELINK,
    sitelink = sitelink,
    order = order,
    createdAt = createdAt,
    removedAt = removedAt,
    status = status,
    rejectReasons = rejectReasons,
)

fun createCampaignContent(
    id: String = generateUniqueRandomId(),
    campaignId: String = generateUniqueRandomId(),
    contentId: String = generateUniqueRandomId(),
    type: MediaType? = MediaType.TEXT,
    removedAt: LocalDateTime? = null,
    text: String? = null,
    status: CampaignContentStatus = CampaignContentStatus.CREATED,
    order: Int = 1,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
) = UacYdbCampaignContent(
    id = id,
    campaignId = campaignId,
    contentId = contentId,
    type = type,
    order = order,
    createdAt = createdAt,
    status = status,
    removedAt = removedAt,
    text = text,
)

fun createDirectCampaign(
    id: String = generateUniqueRandomId(),
    directCampaignId: Long = randomPositiveLong(),
    status: DirectCampaignStatus = DirectCampaignStatus.CREATED
) = UacYdbDirectCampaign(
    id = id,
    directCampaignId = directCampaignId,
    status = status,
    syncedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    rejectReasons = "1",
)

fun createYdbCampaign(
    id: String = generateUniqueRandomId(),
    accountId: String = generateUniqueRandomId(),
    appId: String? = generateUniqueRandomId(),
    advType: AdvType = AdvType.MOBILE_CONTENT,
    targetStatus: TargetStatus = TargetStatus.STARTED,
    href: String = STORE_URL,
    trackingUrl: String? = TRACKING_URL,
    startedAt: LocalDateTime? = null,
    impressionUrl: String? = null,
    counterIds: List<Int>? = null,
    regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID),
    minusRegions: List<Long>? = null,
    hyperGeoId: Long? = null,
    assetLinks: List<UacYdbCampaignContent>? = null,
    strategy: UacStrategy? = null,
    keywords: List<String>? = null,
    minusKeywords: List<String>? = null,
    socdem: Socdem? = null,
    deviceTypes: Set<DeviceType>? = null,
    inventoryTypes: Set<InventoryType>? = null,
    briefSynced: Boolean? = null,
    retargetingCondition: UacRetargetingCondition? = null,
    isEcom: Boolean? = null,
    feedId: Long? = null,
    trackingParams: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    campaignMeasurers: List<UacCampaignMeasurer>? = null,
    cpmAssets: Map<String, UacCpmAsset>? = null,
    zenPublisherId: String? = null,
    contentFlags: Map<String, Any>? = null,
    directCampaignStatus: DirectCampaignStatus? = null,
    cpa: BigDecimal? = BigDecimal.valueOf(100000000L, 6),
    relevanceMatch: UacRelevanceMatch? = null,
    showTitleAndBody: Boolean? = null,
    videosAreNonSkippable: Boolean? = null,
    audienceSegmentsSynchronized: Boolean? = null,
) = UacYdbCampaign(
    id = id,
    name = "Яндекс.Музыка и Подкасты – скачивайте и слушайте (Android)",
    advType = advType,
    cpa = cpa,
    weekLimit = BigDecimal.valueOf(10000000000L, 6),
    regions = regions,
    minusRegions = minusRegions,
    storeUrl = href,
    appId = appId,
    targetId = TargetType.ORDER,
    trackingUrl = trackingUrl,
    account = accountId,
    impressionUrl = impressionUrl,
    createdAt = createdAt,
    updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    startedAt = startedAt,
    targetStatus = targetStatus,
    contentFlags = contentFlags,
    options = null,
    skadNetworkEnabled = null,
    adultContentEnabled = null,
    hyperGeoId = hyperGeoId,
    keywords = keywords,
    minusKeywords = minusKeywords,
    socdem = socdem,
    deviceTypes = deviceTypes,
    inventoryTypes = inventoryTypes,
    goals = null,
    counters = counterIds,
    permalinkId = null,
    phoneId = null,
    calltrackingSettingsId = null,
    timeTarget = null,
    strategy = strategy,
    retargetingCondition = retargetingCondition,
    videosAreNonSkippable = videosAreNonSkippable,
    zenPublisherId = zenPublisherId,
    brandSurveyId = null,
    assetLinks = assetLinks,
    briefSynced = briefSynced,
    showsFrequencyLimit = null,
    strategyPlatform = null,
    isEcom = isEcom,
    crr = null,
    feedId = feedId,
    feedFilters = null,
    trackingParams = trackingParams,
    cpmAssets = cpmAssets,
    campaignMeasurers = campaignMeasurers,
    uacBrandsafety = null,
    uacDisabledPlaces = null,
    recommendationsManagementEnabled = null,
    priceRecommendationsManagementEnabled = null,
    directCampaignStatus = directCampaignStatus,
    relevanceMatch = relevanceMatch,
    showTitleAndBody = showTitleAndBody,
    audienceSegmentsSynchronized = audienceSegmentsSynchronized,
)

fun modifyMeta(meta: String, key: String, value: Any?): String {
    val data: MutableMap<String, Any?> = fromJson(meta)
    data[key] = value
    return toJson(data)
}

fun getExpectedImageContentMeta(directMdsMeta: Any?) = mapOf(
    "ColorWiz" to mapOf(
        "ColorWizBack" to "#FEFEFE",
        "ColorWizButton" to "#D8D8D8",
        "ColorWizButtonText" to "#000000",
        "ColorWizText" to "#62635F",
    ),
    "direct_image_hash" to "kDZ1kMIazmTxwMKQyELoYQ",
    "direct_mds_meta" to directMdsMeta,
    "s16x9" to mapOf(
        "height" to 478,
        "smart-center" to null,
        "smart-centers" to null,
        "width" to 850,
    ),
    "wx1080" to mapOf(
        "width" to 850,
        "smart-centers" to
            listOf(
                mapOf("h" to 266, "w" to 849, "x" to 0, "y" to 292),
                mapOf("h" to 478, "w" to 849, "x" to 0, "y" to 56),
                mapOf("h" to 607, "w" to 608, "x" to 181, "y" to 0),
                mapOf("h" to 295, "w" to 849, "x" to 0, "y" to 270),
                mapOf("h" to 607, "w" to 456, "x" to 159, "y" to 0),
                mapOf("h" to 607, "w" to 809, "x" to 20, "y" to 0),
            ),
        "height" to 607,
        "smart-center" to mapOf("h" to 607, "w" to 374, "x" to 213, "y" to 0),
    )
)

fun getExpectedVideoContentMeta() = mapOf(
    "formats" to emptyList<Any>(),
    "status" to "converting",
    "thumb" to mapOf(
        "height" to 720,
        "preview" to mapOf(
            "height" to 479,
            "width" to 852,
            "url" to "https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/preview480p",
        ),
        "url" to "https://avatars.mds.yandex.net/get-canvas-test/2003583/2a0000017979f0c73d1872d061b65d915f5a/orig",
        "width" to 1280,
    ),
    "creative_id" to 2147318391,
    "creative_type" to "video",
    "vast" to "",
)

fun createModerationDiagModel(
    token: String? = null,
    shortText: String = "shortText",
    diagText: String = "diagText",
) = ModerationDiag()
    .withId(TraceUtil.randomId() % 1000000L + 2000) // для того, чтобы не пересечься с существующими
    .withToken(token)
    .withType(ModerationDiagType.COMMON)
    .withStrongReason(true)
    .withUnbanIsProhibited(false)
    .withDiagText(diagText)
    .withShortText(shortText)
    .withAllowFirstAid(true)

fun getCreativeGroup(creativeIds: List<Int>): CreativeGroup {
    return CreativeGroup(
        id = randomPositiveInt(),
        name = "Creatives",
        creatives = creativeIds.map { creativeId ->
            Creative()
                .withId(creativeId)
                .withName("Розничная торговля")
                .withThumbnailUrl("https://bsapi-test-maple.mediaselling.yandex.net/file/1835194?token=f")
                .withPreview(
                    CreativePreview().apply {
                        url = "https://bsapi-test-maple.mediaselling.yandex.net/page/7009245?token=a"
                    }
                )
                .withWidth(0)
                .withHeight(0)
                .withLayoutCode(
                    CreativeLayoutCode().apply {
                        layoutId = 44
                        themeId = 19
                    }
                )
                .withTemplateId(740)
                .withVersion(8763345)
                .withBusinessType(BusinessType(id = 2, directId = "retail", name = "Розничная торговля"))
                .withPredeployed(true)
        }
    )
}

fun createAccount(
    uid: Long,
    directClientId: Long,
    id: String = generateUniqueRandomId(),
    features: AccountFeatures? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
) = UacYdbAccount(
    uid = uid,
    directClientId = directClientId,
    id = id,
    features = features,
    createdAt = createdAt
)

fun createAdGroupBriefGrutModel(
    campaignId: Long,
    id: Long? = null,//generateRandomIdLong(),
    assetLinks: List<UacYdbCampaignContent>? = null,
    keywords: List<String> = emptyList(),
    regions: List<Long> = listOf(Region.RUSSIA_REGION_ID),
    url: String = STORE_URL,
    deviceTypes: Set<DeviceType>? = null,
    hyperGeoId: Long? = null,
    minusKeywords: List<String>? = null,
    minusRegions: List<Long>? = null,
    socdem: Socdem? = null,
    videosAreNonSkippable: Boolean? = null,
    retargetingCondition: UacRetargetingCondition? = null,
) = AdGroupBriefGrutModel(
    name = "Группа объявлений",
    campaignId = campaignId,
    url = url,
    retargetingCondition = retargetingCondition,
    brandSurveyId = null,
    showsFrequencyLimit = null,
    cpmAssets = null,
    campaignMeasurers = null,
    uacBrandsafety = null,
    uacDisabledPlaces = null,
    assetLinks = assetLinks,
    keywords = keywords,
    regions = regions,
    catalogIds = null,
    deviceTypes = deviceTypes,
    hyperGeoId = hyperGeoId,
    id = id,
    minusKeywords = minusKeywords,
    minusRegions = minusRegions,
    socdem = socdem,
    videosAreNonSkippable = videosAreNonSkippable,
    isCpmBrief = false,
)

const val thumbUrl = "https://avatars.mds.yandex.net/get-uac/42/some_name/thumb"

val avatarsConfig = AvatarsConfig(
    "test uac config",
    ServerConfig("avatars.mds.yandex.net", 443, "https"),
    ServerConfig("avatars-int.mds.yandex.net", 13000, "http"),
    Duration.ofSeconds(20),
    "uac",
    false
)

private val meta = mapOf(
    "ColorWizBack" to "#FFFFFF",
    "ColorWizButton" to "#EEEEEE",
    "ColorWizButtonText" to "#DDDDDD",
    "ColorWizText" to "#CCCCCC",
    "crc64" to "A0EDB296A3629F59",
    "orig-size" to mapOf(
        "x" to 1200,
        "y" to 800,
    ),
    "orig-format" to "JPEG",
    "orig-animated" to false,
    "orig-size-bytes" to 123456,
    "orig-orientation" to 0,
)

private val sizes = mapOf(
    "wx1080" to ImageSize()
        .withHeight(603)
        .withWidth(1080)
        .withSmartCenter(SmartArea(300, 500, 80, 3))
        .withSmartCenters(emptyMap()),
)

val avatarInfo = AvatarInfo(
    "uac", 42, "some_name", toJson(mapOf("meta" to meta)), sizes
)
