package ru.yandex.direct.web.entity.uac

import ru.yandex.direct.core.entity.banner.model.ImageType
import ru.yandex.direct.core.entity.image.container.UploadedBannerImageInformation
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta
import ru.yandex.direct.core.entity.image.model.ImageMdsMetaInfo
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta
import ru.yandex.direct.core.entity.uac.getExpectedImageContentMeta
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.HolidaySettings
import ru.yandex.direct.core.entity.uac.model.InventoryType
import ru.yandex.direct.core.entity.uac.model.LimitPeriodType
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Sitelink
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.TimeTarget
import ru.yandex.direct.core.entity.uac.model.UacAdjustmentRequest
import ru.yandex.direct.core.entity.uac.model.UacGoal
import ru.yandex.direct.core.entity.uac.model.UacRetargetingCondition
import ru.yandex.direct.core.entity.uac.model.UacShowsFrequencyLimit
import ru.yandex.direct.core.entity.uac.model.UacStrategy
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacCpmAsset
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacSearchLift
import ru.yandex.direct.core.testing.data.TestDomain
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.web.entity.uac.converter.proto.UacContentProtoConverter.toProto
import ru.yandex.direct.web.entity.uac.model.ArchiveInfo
import ru.yandex.direct.web.entity.uac.model.CreateCampaignRequest
import ru.yandex.direct.web.entity.uac.model.CreativeStatistics
import ru.yandex.direct.web.entity.uac.model.Document
import ru.yandex.direct.web.entity.uac.model.Group
import ru.yandex.direct.web.entity.uac.model.Grouping
import ru.yandex.direct.web.entity.uac.model.GtaRelatedAttribute
import ru.yandex.direct.web.entity.uac.model.PatchCampaignRequest
import ru.yandex.direct.web.entity.uac.model.SaasKeyNames
import ru.yandex.direct.web.entity.uac.model.SaasResponse
import ru.yandex.direct.web.entity.uac.model.UacCampaign
import ru.yandex.direct.web.entity.uac.model.UacCampaignAccess
import ru.yandex.direct.web.entity.uac.model.UacCampaignProtoResponse
import ru.yandex.direct.web.entity.uac.model.UacCampaignServicedState
import ru.yandex.direct.web.entity.uac.service.emptyPatchRequest
import ru.yandex.direct.web.proto.api.uac.GetCampaignResponse
import ru.yandex.grut.objects.proto.BrandSurvey
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.CampaignBriefStatus
import ru.yandex.grut.objects.proto.GeoSegment
import ru.yandex.grut.objects.proto.MediaType.EMediaType
import java.math.BigDecimal
import java.time.LocalDateTime

private val directImageMdsMeta = ImageMdsMeta()
    .withMeta(ImageMdsMetaInfo().withOrigFormat("JPEG").withOrigSizeBytes(123456))
    .withSizes(mapOf(
        "x90" to ImageSizeMeta().withHeight(90).withWidth(90),
        "y91" to ImageSizeMeta().withHeight(91).withWidth(91)
    ))

val directBannerImageInformation = UploadedBannerImageInformation()
    .withImageHash("direct_image_hash")
    .withMdsMeta(directImageMdsMeta)
    .withImageType(ImageType.REGULAR)

fun createUacSaasResponse(database_ids: List<String>): SaasResponse {
    return SaasResponse(grouping = listOf(
        Grouping(group = listOf(
            Group(document = database_ids.mapIndexed { idx, value ->
                Document(
                    archiveInfo = ArchiveInfo(
                        gtaRelatedAttribute = database_ids.map {
                            GtaRelatedAttribute(
                                key = SaasKeyNames.DATABASE_ID,
                                value = value,
                            )
                        }
                    ),
                    relevance = idx
                )
            })
        ))
    ))
}

fun createCreativeStatistics(
    clicks: Long = 0,
    shows: Long = 0,
    costMicros: Long = 0,
    costCurMicros: Long = 0,
    costTaxFreeMicros: Long = 0,
    avgCpm: Double = 0.0,
    avgCpc: Double = 0.0,
    videoAvgTrueViewCost: Double = 0.0,
    uniqViewers: Long = 0,
    videoFirstQuartileRate: Double = 0.0,
    videoMidpointRate: Double = 0.0,
    videoThirdQuartileRate: Double = 0.0,
    videoCompleteRate: Double = 0.0,
    videoTrueView: Long = 0,
    avgNShow: Double = 0.0,
    avgNShowComplete: Double = 0.0,
    conversions: Long = 0,
    installs: Long = 0,
    postViewConversions: Long = 0,
    postViewInstalls: Long = 0,
    ctr: Double = 0.0,
    cr: Double = 0.0,
    cpa: Double = 0.0,
    cpc: Double = 0.0,
    cpm: Double = 0.0,
) = CreativeStatistics(
    clicks = clicks,
    shows = shows,
    costMicros = costMicros,
    costCurMicros = costCurMicros,
    costTaxFreeMicros = costTaxFreeMicros,
    avgCpm = avgCpm,
    avgCpc = avgCpc,
    videoAvgTrueViewCost = videoAvgTrueViewCost,
    uniqViewers = uniqViewers,
    videoFirstQuartileRate = videoFirstQuartileRate,
    videoMidpointRate = videoMidpointRate,
    videoThirdQuartileRate = videoThirdQuartileRate,
    videoCompleteRate = videoCompleteRate,
    videoTrueView = videoTrueView,
    avgNShow = avgNShow,
    avgNShowComplete = avgNShowComplete,
    conversions = conversions,
    installs = installs,
    postViewConversions = postViewConversions,
    postViewInstalls = postViewInstalls,
    ctr = ctr,
    cr = cr,
    cpa = cpa,
    cpc = cpc,
    cpm = cpm,
)

fun createUcCampaignRequest(
    retargetingCondition: UacRetargetingCondition? = null,
    displayName: String = "fukudo-shiba.ru от 25.05.21",
    href: String = "http://fukudo-shiba.ru",
    texts: List<String>? = listOf("купить щенка"),
    titles: List<String>? = listOf("купить щенка сиба-ину"),
    regions: List<Long>? = listOf(Region.RUSSIA_REGION_ID),
    minusRegions: List<Long>? = listOf(Region.SAINT_PETERSBURG_REGION_ID),
    contentIds: List<String>? = emptyList(),
    weekLimit: BigDecimal? = BigDecimal.valueOf(2300),
    limitPeriod: LimitPeriodType? = LimitPeriodType.MONTH,
    advType: AdvType = AdvType.TEXT,
    hyperGeoId: Long? = null,
    keywords: List<String>? = listOf("сиба ину купить щенка", "сиба ину купить щенка москва"),
    minusKeywords: List<String>? = listOf("кот", "котенок"),
    socdem: Socdem? = defaultSocdem(),
    deviceTypes: Set<DeviceType>? = setOf(DeviceType.PHONE, DeviceType.TABLET),
    inventoryTypes: Set<InventoryType>? = null,
    goals: List<UacGoal>? = null,
    counters: List<Int>? = null,
    sitelinks: List<Sitelink>? = defaultSitelinks(),
    appId: String? = null,
    trackingUrl: String? = null,
    targetId: TargetType? = null,
    adultContentEnabled: Boolean? = null,
    cpa: BigDecimal? = null,
    timeTarget: TimeTarget? = defaultTimeTarget(),
    strategy: UacStrategy? = null,
    showsFrequencyLimit: UacShowsFrequencyLimit? = null,
    adjustments: List<UacAdjustmentRequest>? = null,
    isEcom: Boolean? = false,
    cpmAssets: Map<String, UacCpmAsset>? = null,
    uacDisabledPlaces: UacDisabledPlaces? = null,
    showTitleAndBody: Boolean? = null,
    videosAreNonSkippable: Boolean? = null,
    searchLift: UacSearchLift? = null,
) = CreateCampaignRequest(
    displayName = displayName,
    href = href,
    texts = texts,
    titles = titles,
    regions = regions,
    minusRegions = minusRegions,
    contentIds = contentIds,
    weekLimit = weekLimit,
    limitPeriod = limitPeriod,
    advType = advType,
    hyperGeoId = hyperGeoId,
    keywords = keywords,
    minusKeywords = minusKeywords,
    socdem = socdem,
    deviceTypes = deviceTypes,
    inventoryTypes = inventoryTypes,
    goals = goals,
    counters = counters,
    goalCreateRequest = null,
    permalinkId = null,
    phoneId = null,
    calltrackingPhones = null,
    sitelinks = sitelinks,
    appId = appId,
    trackingUrl = trackingUrl,
    impressionUrl = null,
    targetId = targetId,
    skadNetworkEnabled = null,
    adultContentEnabled = adultContentEnabled,
    cpa = cpa,
    crr = null,
    timeTarget = timeTarget,
    strategy = strategy,
    retargetingCondition = retargetingCondition,
    videosAreNonSkippable = videosAreNonSkippable,
    brandSurveyId = null,
    brandSurveyName = null,
    showsFrequencyLimit = showsFrequencyLimit,
    strategyPlatform = null,
    adjustments = adjustments,
    isEcom = isEcom,
    feedId = null,
    feedFilters = null,
    trackingParams = null,
    cloneFromCampaignId = null,
    cpmAssets = cpmAssets,
    campaignMeasurers = null,
    uacBrandsafety = null,
    uacDisabledPlaces = uacDisabledPlaces,
    widgetPartnerId = null,
    source = null,
    mobileAppId = null,
    isRecommendationsManagementEnabled = false,
    isPriceRecommendationsManagementEnabled = false,
    relevanceMatch = null,
    showTitleAndBody = showTitleAndBody,
    altAppStores = null,
    bizLandingId = null,
    searchLift = searchLift,
)

fun defaultSocdem() = Socdem(
    genders = listOf(Gender.MALE),
    ageLower = AgePoint.AGE_18,
    ageUpper = AgePoint.AGE_55,
    incomeLower = null,
    incomeUpper = null)

fun defaultSitelinks() = listOf(
    Sitelink(
        title = "sitelink title",
        description = "sitelink description",
        href = "https://sitelink-href.ru"))

fun defaultTimeTarget() = TimeTarget(
    idTimeZone = 2,
    useWorkingWeekends = true,
    enabledHolidaysMode = true,
    holidaysSettings = HolidaySettings(
        startHour = 12,
        endHour = 15,
        rateCorrections = null,
        show = true,
    ),
    timeBoard = (1..5).map {
        listOf(
            0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
        )
    } + listOf(
        listOf(
            0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
        )
    ) // Saturday
        + listOf(
        listOf(
            0, 0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0
        )
    ) // Sunday
)

fun updateCampaignRequest(
    recommendationsManagementEnabled: Boolean? = null,
    priceRecommendationsManagementEnabled: Boolean? = null,
    retargetingCondition: UacRetargetingCondition? = null,
): PatchCampaignRequest {
    val bannerHref = "https://" + TestDomain.randomDomain()
    val weekLimit = BigDecimal.valueOf(2300000000, 6)
    val regions = listOf(Region.RUSSIA_REGION_ID, Region.BY_REGION_ID)
    val socdem = Socdem(listOf(Gender.FEMALE), AgePoint.AGE_45, AgePoint.AGE_INF, Socdem.IncomeGrade.LOW, Socdem.IncomeGrade.PREMIUM)
    val actualGoals = listOf(UacGoal(RandomNumberUtils.nextPositiveInteger().toLong(), null))
    val counters = listOf(RandomNumberUtils.nextPositiveInteger())

    return emptyPatchRequest().copy(
        titles = listOf("title"),
        texts = listOf("text"),
        displayName = "Text campaign",
        weekLimit = weekLimit,
        href = bannerHref,
        regions = regions,
        socdem = socdem,
        goals = actualGoals,
        counters = counters,
        limitPeriod = LimitPeriodType.MONTH,
        keywords = listOf("keyword1", "keyword2"),
        deviceTypes = setOf(DeviceType.ALL),
        cpa = BigDecimal.valueOf(100000000L, 6),
        isRecommendationsManagementEnabled = recommendationsManagementEnabled,
        isPriceRecommendationsManagementEnabled = priceRecommendationsManagementEnabled,
        retargetingCondition = retargetingCondition,
    )
}

fun campaignProtoResponse() = UacCampaignProtoResponse(
    reqId = "123",
    result = GetCampaignResponse.TGetCampaignResponse.newBuilder().apply {
        campaignBuilder.apply {
            metaBuilder.apply {
                id = 1L
                campaignType = Campaign.ECampaignTypeOld.CTO_MOBILE_APP
            }
            specBuilder.apply {
                campaignBriefBuilder.apply {
                    targetStatus = Campaign.TCampaignBrief.EBriefStatus.BS_STARTED
                }
            }
        }
        status = CampaignBriefStatus.ECampaignBriefStatus.CBS_STARTED
        accessBuilder.apply {
            addActions(Campaign.ECampaignAction.CA_BAN_PAY)
            addActions(Campaign.ECampaignAction.CA_AGENCY_SERVICED)
            canEdit = true
            servicedState = Campaign.ECampaignServicedState.CSS_ACCEPT_SERVICING
        }
        agencyInfoBuilder.apply {
            name = "name"
            email = "some.mail@ya.ru"
            phone = "12345"
            representativeName = "representative name"
            showAgencyContacts = true
        }
        managerInfoBuilder.apply {
            name = "name"
            email = "some.mail@ya.ru"
            addContactInfo(GetCampaignResponse.TContactPhone.newBuilder().apply {
                phone = "12345"
                extension = 678L
            })
        }
        hyperGeoBuilder.apply {
            id = 1L
            name = "name"
            addHyperGeoSegments(GetCampaignResponse.THyperGeo.THyperGeoSegment.newBuilder().apply {
                goalId = 2L
                clientId = 3L
                addCoveringGeo(4L)
                hyperGeoSegmentDetailsBuilder.apply {
                    segmentName = "name"
                    radius = 100500
                    periodLength = 5
                    timesQuantity = 6
                    geoSegmentType = GeoSegment.EGeoSegment.GS_CONDITION
                    addPointsBuilder().apply {
                        latitude = 56.8351382193431
                        longitude = 60.59133688008281
                        address = "address"
                    }
                }
            })
        }
        brandSurveyStatusBuilder.apply {
            surveyStatusDaily = BrandSurvey.EBrandSurveyStatus.BSS_ACTIVE
            addReasonIds(1L)
            addBrandSurveyStopReasonsDaily(BrandSurvey.EBrandSurveyStopReason.BSSR_LOW_BUDGET)
            basicUpliftBuilder.apply {
                adRecall = 1.0
            }
        }
        addAdjustmentsBuilder().apply {
            adjustmentBuilder.apply {
                region = 5L
                gender = ru.yandex.grut.objects.proto.Gender.EGender.G_MALE
                age = ru.yandex.grut.objects.proto.AgePoint.EAgePoint.AP_AGE_25
                percent = 50
                retargetingConditionId = 6L
            }
            regionName = "region"
        }
        appInfoBuilder.apply {
            appInfoBuilder.apply {
                metaBuilder.apply {
                    id = 1L
                }
                specBuilder.apply {
                    appId = "app_id"
                    platform = 1
                    url = "url"
                }
            }
        }
        addContentsBuilder().apply {
            id = "id"
            mediaType = EMediaType.MT_IMAGE
            thumb = "thumb"
            thumbId = "thumb_id"
            sourceUrl = "source_url"
            origSizeBuilder.apply {
                width = 400
                height = 200
            }
            metaBuilder.mergeFrom(toProto(getExpectedImageContentMeta(null), MediaType.IMAGE))
        }
    }.build()
)

fun uacCampaign() = UacCampaign(
    id = "id",
    accessKt = UacCampaignAccess(canEdit = false,  noActions = false, setOf(), setOf(),
        UacCampaignServicedState.SELF_SERVICED),
    agencyInfoKt = null,
    managerInfoKt = null,
    showsKt = 0,
    sumKt = BigDecimal.ZERO,
    advType = AdvType.TEXT,
    displayName = "name",
    appInfo = null,
    contents = listOf(),
    texts = null,
    titles = null,
    regions = null,
    regionNames = null,
    minusRegions = null,
    minusRegionNames = null,
    trackingUrl = null,
    impressionUrl = null,
    href = "href",
    faviconLink = null,
    targetId = null,
    cpa = null,
    weekLimit = null,
    createdTime = LocalDateTime.MIN,
    updatedTime = LocalDateTime.MIN,
    startedTime = null,
    status = Status.DRAFT,
    isStatusObsolete = false,
    extStatus = null,
    targetStatus = TargetStatus.STARTED,
    directId = null,
    rejectReasons = null,
    contentFlags = null,
    stateReasons = null,
    limitPeriod = null,
    skadNetworkEnabled = null,
    adultContentEnabled = null,
    hyperGeo = null,
    keywords = null,
    minusKeywords = null,
    socdem = null,
    deviceTypes = null,
    inventoryTypes = null,
    goals = null,
    counters = null,
    permalinkId = null,
    phoneId = null,
    calltrackingSettingsId = null,
    sitelinks = null,
    timeTarget = null,
    bid = null,
    strategy = null,
    retargetingCondition = null,
    videosAreNonSkippable = null,
    zenPublisherId = null,
    brandSurveyId = null,
    brandSurveyName = null,
    showsFrequencyLimit = null,
    strategyPlatform = null,
    brandSurveyStatus = null,
    adjustments = null,
    isEcom = null,
    crr = null,
    feedId = null,
    feedFilters = null,
    showOfferStats = false,
    trackingParams = null,
    cpmAssets = null,
    campaignMeasurers = null,
    uacBrandsafety = null,
    uacDisabledPlaces = null,
    isRecommendationsManagementEnabled = null,
    isPriceRecommendationsManagementEnabled = null,
    relevanceMatchCategories = null,
    showTitleAndBody = null,
    altAppStores = null,
    bizLandingId = null,
    searchLift = null,
)
