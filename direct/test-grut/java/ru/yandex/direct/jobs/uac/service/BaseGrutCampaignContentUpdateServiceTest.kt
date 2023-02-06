package ru.yandex.direct.jobs.uac.service

import java.time.Instant
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.banner.container.BannerAdditionalActionsContainer
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerService
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerFlags
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.execution.BannersAddExecutionGrutService
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateAdGroupRequest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateCampaignRequest
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter
import ru.yandex.direct.core.entity.uac.createAdGroupBriefGrutModel
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateRandomIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.api.AdGroupBriefGrutModel
import ru.yandex.direct.core.grut.api.BannerTestGrutApi
import ru.yandex.direct.core.grut.api.BriefBanner
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.ImageAssetGrut
import ru.yandex.direct.core.grut.api.SitelinkAssetGrut
import ru.yandex.direct.core.grut.api.TextAssetGrut
import ru.yandex.direct.core.grut.api.TitleAssetGrut
import ru.yandex.direct.core.grut.api.VideoAssetGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.feature.FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.grut.objects.proto.Asset
import ru.yandex.grut.objects.proto.AssetLink
import ru.yandex.grut.objects.proto.Banner
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.MediaType.EMediaType
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_SITELINK
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO
import ru.yandex.grut.objects.proto.client.Schema

data class TestData(
    val campaignInfo: TypedCampaignInfo,
    val adGroupInfo: AdGroupInfo,
    val grutCampaign: Schema.TCampaign,
    val adGroupId: Long,
    val campaignId: Long,

    val assetIdsByAssetTypes: Map<EMediaType, String>,
    val assetLinkIdToAssetId: Map<String, String>,
) {
    val bannerAssetIds: List<String>
        // caйтлинки на баннер не передаются
        get() = assetIdsByAssetTypes.filterKeys { it != MT_SITELINK }.values.toList()

    val bannerAssetLinkIds: List<String>
        get() {
            val assetIds = bannerAssetIds.toSet()
            return assetLinkIdToAssetId.asSequence()
                .filter { assetIds.contains(it.value) }
                .map { it.key }
                .toList()
        }
}

abstract class BaseGrutCampaignContentUpdateServiceTest {
    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    protected lateinit var grutApiService: GrutApiService

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var bannersAddExecutionGrutService: BannersAddExecutionGrutService

    @Autowired
    private lateinit var bannersAddOperationContainerService: BannersAddOperationContainerService

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var bannerTestGrutApi: BannerTestGrutApi

    protected lateinit var clientInfo: ClientInfo
    protected lateinit var userInfo: UserInfo
    protected lateinit var mobileAppInfo: MobileAppInfo

    protected fun createTestData(
        assetTypes: Set<EMediaType> = setOf(),
        assetLinksRemoveAt: Long? = null,
        campaignType: CampaignType = CampaignType.MOBILE_CONTENT,
        newAssetLinkIdGeneration: Boolean = false,
        adGroupBriefEnabled: Boolean = false,
    ): TestData {
        val campaignInfo = if (campaignType == CampaignType.MOBILE_CONTENT) createMobileContentCampaignInfo()
        else createTextCampaignInfo()

        val adGroupInfo = if (campaignType == CampaignType.MOBILE_CONTENT) steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())
        else steps.adGroupSteps().createActiveTextAdGroup(campaignInfo.toCampaignInfo())

        val grutCampaign = createGrutCampaign(campaignInfo)
        val campaignId = grutCampaign.meta.id

        val grutAdGroupId = createGrutAdGroup(campaignInfo, adGroupInfo)

        val assetIdsByAssetTypes = createAssets(clientInfo, assetTypes)
        val assetLinkIdToAssetId = assetIdsByAssetTypes.values
            .associateBy { if (newAssetLinkIdGeneration) generateUniqueRandomId() else it }

        steps.featureSteps().addClientFeature(clientInfo.clientId, UAC_MULTIPLE_AD_GROUPS_ENABLED, adGroupBriefEnabled)
        if (adGroupBriefEnabled) {
            val adGroupBriefModel = createGroupBriefModel(campaignId)
            setAssetLinksToAdGroupBrief(adGroupBriefModel, assetLinkIdToAssetId, assetLinksRemoveAt)
        } else {
            setAssetLinksToCampaign(grutCampaign, assetLinkIdToAssetId, assetLinksRemoveAt)
        }

        return TestData(
            campaignInfo = campaignInfo,
            adGroupInfo = adGroupInfo,
            grutCampaign = grutCampaign,
            adGroupId = grutAdGroupId,
            campaignId = campaignId,
            assetIdsByAssetTypes = assetIdsByAssetTypes,
            assetLinkIdToAssetId = assetLinkIdToAssetId,
        )
    }

    private fun createMobileContentCampaignInfo(): TypedCampaignInfo {
        return typedCampaignStepsUnstubbed.createMobileContentCampaign(
            userInfo, clientInfo,
            TestCampaigns.defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId)
        )
    }

    private fun createTextCampaignInfo(): TypedCampaignInfo {
        return typedCampaignStepsUnstubbed.createDefaultTextCampaign(userInfo, clientInfo)
    }

    protected fun createGrutClient(clientInfo: ClientInfo) {
        grutApiService.clientGrutDao.createOrUpdateClient(
            ClientGrutModel(
                client = Client().withId(clientInfo.clientId!!.asLong()).withCreateDate(LocalDateTime.now()),
                ndsHistory = listOf()
            )
        )
    }

    private fun createGrutCampaign(campaignInfo: TypedCampaignInfo): Schema.TCampaign {

        val campaignSpec = UacGrutCampaignConverter.toCampaignSpec(createYdbCampaign())
        val campaignId = grutApiService.briefGrutApi
            .createBrief(
                buildCreateCampaignRequest(
                    clientId = clientInfo.clientId!!.asLong(),
                    campaignId = campaignInfo.campaign.id,
                    campaignType = Campaign.ECampaignTypeOld.CTO_MOBILE_APP,
                    campaignSpec = campaignSpec
                )
            )

        return grutApiService.briefGrutApi.getBrief(campaignId)!!
    }

    private fun createGrutAdGroup(campaignInfo: TypedCampaignInfo, adGroupInfo: AdGroupInfo): Long {
        grutApiService.briefAdGroupGrutApi
            .createOrUpdateBriefAdGroup(buildCreateAdGroupRequest(campaignInfo.id, adGroupInfo.adGroupId))
        return adGroupInfo.adGroupId
    }

    private fun createAssets(clientInfo: ClientInfo, assetTypes: Set<EMediaType>): Map<EMediaType, String> {
        val assetTypesWithAssetIds = mutableMapOf<EMediaType, String>()
        if (assetTypes.contains(MT_TITLE)) {
            val titleAssetId = grutApiService.assetGrutApi.createObject(
                TitleAssetGrut(
                    id = generateRandomIdLong(),
                    clientId = clientInfo.clientId!!.asLong(),
                    title = "Title"
                )
            ).toIdString()
            assetTypesWithAssetIds[MT_TITLE] = titleAssetId
        }
        if (assetTypes.contains(MT_TEXT)) {
            val textAssetId = grutApiService.assetGrutApi.createObject(
                TextAssetGrut(
                    id = generateRandomIdLong(),
                    clientId = clientInfo.clientId!!.asLong(),
                    text = "Text"
                )
            ).toIdString()
            assetTypesWithAssetIds[MT_TEXT] = textAssetId
        }

        if (assetTypes.contains(MT_IMAGE)) {
            val imageAssetId = grutApiService.assetGrutApi.createObject(
                ImageAssetGrut(
                    id = generateRandomIdLong(),
                    clientId = clientInfo.clientId!!.asLong(),
                    imageHash = "123113",
                    mdsInfo = Asset.TMdsInfo.newBuilder().build()
                )
            ).toIdString()
            assetTypesWithAssetIds[MT_IMAGE] = imageAssetId
        }
        if (assetTypes.contains(MT_VIDEO)) {
            val videoAssetId = grutApiService.assetGrutApi.createObject(
                VideoAssetGrut(
                    id = generateRandomIdLong(),
                    clientId = clientInfo.clientId!!.asLong(),
                    duration = 15,
                    mdsInfo = Asset.TMdsInfo.newBuilder().build()
                )
            ).toIdString()
            assetTypesWithAssetIds[MT_VIDEO] = videoAssetId
        }
        if (assetTypes.contains(MT_SITELINK)) {
            val siteLinkAssetId = grutApiService.assetGrutApi.createObject(
                SitelinkAssetGrut(
                    id = generateRandomIdLong(),
                    clientId = clientInfo.clientId!!.asLong(),
                    title = "",
                    href = "",
                    description = null
                )
            ).toIdString()
            assetTypesWithAssetIds[MT_SITELINK] = siteLinkAssetId
        }
        return assetTypesWithAssetIds
    }

    private fun setAssetLinksToCampaign(
        grutCampaign: Schema.TCampaign,
        assetLinkIdToAssetId: Map<String, String>,
        assetLinksRemoveAt: Long?,
    ) {
        val assetLinks = assetLinkIdToAssetId.map { (assetLinkId, assetId) ->
            AssetLink.TAssetLink.newBuilder().apply {
                id = assetLinkId.toIdLong()
                this.assetId = assetId.toIdLong()
                if (assetLinksRemoveAt != null) {
                    removeTime = assetLinksRemoveAt.toInt()
                }
                order = 1
                createTime = Instant.now().epochSecond.toInt()
            }.build()
        }
        val brief = Schema.TCampaign.newBuilder().apply {
            meta = Schema.TCampaignMeta.newBuilder().setId(grutCampaign.meta.id).build()
            spec = Campaign.TCampaignSpec.newBuilder(grutCampaign.spec)
                .setBriefAssetLinks(AssetLink.TAssetLinks.newBuilder().addAllLinks(assetLinks).build()).build()
        }.build()
        grutApiService.briefGrutApi.updateBriefFull(brief)
    }

    private fun setAssetLinksToAdGroupBrief(
        adGroupBriefModel: AdGroupBriefGrutModel,
        assetLinkIdToAssetId: Map<String, String>,
        assetLinksRemoveAt: Long?,
    ) {
        val assets = assetLinkIdToAssetId
            .map { (assetLinkId, assetId) ->
                createCampaignContent(
                    id = assetLinkId,
                    contentId = assetId,
                    removedAt = if (assetLinksRemoveAt != null) UacYdbUtils.fromEpochSecond(assetLinksRemoveAt) else null,
                    createdAt = LocalDateTime.now(),
                    order = 1
                )
            }
        grutApiService.adGroupBriefGrutApi.createOrUpdateAdGroupBrief(
            adGroupBriefModel.copy(assetLinks = assets)
        )
    }

    private fun createGroupBriefModel(campaignId: Long): AdGroupBriefGrutModel {
        val adGroupBriefIds = grutApiService.adGroupBriefGrutApi.createAdGroupBriefs(
            listOf(createAdGroupBriefGrutModel(campaignId))
        )
        assertThat(adGroupBriefIds)
            .`as`("Групповая заявка создана в груте")
            .isNotEmpty
        return grutApiService.adGroupBriefGrutApi.getAdGroupBrief(adGroupBriefIds[0])!!
    }

    protected fun createGrutBanner(
        testData: TestData,
        bannerId: Long,
        isDeleted: Boolean = false,
    ) {
        val briefBanner = BriefBanner(
            id = bannerId,
            adGroupId = testData.adGroupId,
            briefId = testData.campaignId,
            source = Banner.EBannerSource.BS_DIRECT,
            assetIds = testData.bannerAssetIds.map { it.toIdLong() },
            assetLinksIds = testData.bannerAssetLinkIds.map { it.toIdLong() },
            status =
            if (isDeleted) Banner.TBannerSpec.EBannerStatus.BSS_DELETED
            else Banner.TBannerSpec.EBannerStatus.BSS_ACTIVE
        )
        grutApiService.briefBannerGrutApi.createOrUpdateBriefBanner(briefBanner)
    }

    protected fun createMobileContentBanner(
        testData: TestData,
        bannerFlags: BannerFlags? = null,
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.YES,
        statusBannerImageModerate: StatusBannerImageModerate? = null,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate? = null,
        inGrut: Boolean = false,
        isDeleted: Boolean = false,
    ): Long {
        return if (inGrut) {
            createTextBannerInGrut(
                testData, bannerFlags, bannerStatusModerate, statusBannerImageModerate,
                bannerCreativeStatusModerate, null, isDeleted
            )
        } else {
            createMobileContentBannerInMysql(
                testData, bannerFlags, bannerStatusModerate,
                statusBannerImageModerate ?: StatusBannerImageModerate.YES,
                bannerCreativeStatusModerate ?: BannerCreativeStatusModerate.YES,
                isDeleted,
            )
        }
    }

    private fun createMobileContentBannerInMysql(
        testData: TestData,
        bannerFlags: BannerFlags? = null,
        bannerStatusModerate: BannerStatusModerate,
        statusBannerImageModerate: StatusBannerImageModerate,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate,
        isDeleted: Boolean,
    ): Long {
        val creativeInfo = steps.creativeSteps().createCreative(clientInfo)
        val imageAdImageFormat = steps.bannerSteps().createWideImageFormat(clientInfo)

        val banner = TestNewMobileAppBanners
            .fullMobileAppBanner(testData.campaignInfo.id, testData.adGroupInfo.adGroupId)
            .withStatusModerate(bannerStatusModerate)
            .withImageStatusModerate(statusBannerImageModerate)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withCreativeId(creativeInfo.creativeId)
            .withImageHash(imageAdImageFormat.imageHash)
            .withImageStatusShow(true)
            .withBsBannerId(0L)
            .withImageBsBannerId(0L)
            .withFlags(bannerFlags)
            .withImageDateAdded(LocalDateTime.now())
            .withStatusArchived(isDeleted)

        return steps.mobileAppBannerSteps()
            .createMobileAppBanner(
                NewMobileAppBannerInfo()
                    .withClientInfo(clientInfo)
                    .withAdGroupInfo(testData.adGroupInfo)
                    .withImageFormat(imageAdImageFormat)
                    .withBanner(banner)
            ).bannerId
    }

    protected fun createTextBanner(
        testData: TestData,
        bannerFlags: BannerFlags? = null,
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.YES,
        statusBannerImageModerate: StatusBannerImageModerate? = null,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate? = null,
        bannerSitelinksSetStatusModerate: BannerStatusSitelinksModerate? = null,
        inGrut: Boolean = false,
    ): Long {
        return if (inGrut) {
            createTextBannerInGrut(
                testData, bannerFlags, bannerStatusModerate, statusBannerImageModerate,
                bannerCreativeStatusModerate, bannerSitelinksSetStatusModerate
            )
        } else {
            createTextBannerInMysql(
                testData, bannerFlags, bannerStatusModerate,
                statusBannerImageModerate ?: StatusBannerImageModerate.YES,
                bannerCreativeStatusModerate ?: BannerCreativeStatusModerate.YES,
                bannerSitelinksSetStatusModerate ?: BannerStatusSitelinksModerate.YES
            )
        }
    }

    private fun createTextBannerInMysql(
        testData: TestData,
        bannerFlags: BannerFlags? = null,
        bannerStatusModerate: BannerStatusModerate,
        statusBannerImageModerate: StatusBannerImageModerate,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate,
        bannerSitelinksSetStatusModerate: BannerStatusSitelinksModerate,
    ): Long {
        val creativeInfo = steps.creativeSteps().createCreative(clientInfo)
        val imageAdImageFormat = steps.bannerSteps().createWideImageFormat(clientInfo)

        val banner = TestNewTextBanners
            .fullTextBanner(testData.campaignInfo.id, testData.adGroupInfo.adGroupId)
            .withStatusModerate(bannerStatusModerate)
            .withImageStatusModerate(statusBannerImageModerate)
            .withBsBannerId(0L)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withCreativeId(creativeInfo.creativeId)
            .withImageHash(imageAdImageFormat.imageHash)
            .withImageStatusShow(true)
            .withBsBannerId(0L)
            .withImageBsBannerId(0L)
            .withFlags(bannerFlags)
            .withImageDateAdded(LocalDateTime.now())
            .withStatusSitelinksModerate(bannerSitelinksSetStatusModerate)

        return steps.textBannerSteps()
            .createBanner(
                NewTextBannerInfo()
                    .withClientInfo(clientInfo)
                    .withAdGroupInfo(testData.adGroupInfo)
                    .withBanner(banner)
            ).bannerId
    }

    private fun createTextBannerInGrut(
        testData: TestData,
        bannerFlags: BannerFlags? = null,
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.YES,
        statusBannerImageModerate: StatusBannerImageModerate?,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate?,
        bannerSitelinksSetStatusModerate: BannerStatusSitelinksModerate?,
        isDeleted: Boolean = false,
    ): Long {
        val shard = testData.campaignInfo.shard
        val bannerId = shardHelper.generateBannerIds(listOf(testData.adGroupId))[0]
        val clientRegionId = testData.campaignInfo.clientInfo.client!!.countryRegionId

        val banner = TextBanner()
            .withId(bannerId)
            .withAdGroupId(testData.adGroupInfo.adGroupId)
            .withCampaignId(testData.campaignInfo.id)
            .withStatusModerate(bannerStatusModerate)
            .withImageStatusModerate(statusBannerImageModerate)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withStatusSitelinksModerate(bannerSitelinksSetStatusModerate)
            .withFlags(bannerFlags)
            .withStatusArchived(isDeleted)

        val operationContainer = BannersAddOperationContainerImpl(
            shard,
            userInfo.uid,
            RbacRole.CLIENT,
            userInfo.clientId,
            userInfo.uid,
            userInfo.uid,
            clientRegionId,
            emptySet(),
            ModerationMode.FORCE_SAVE_DRAFT,
            false,
            false,
            false
        )
        operationContainer.client = testData.campaignInfo.clientInfo.client

        val validBannersToApply = listOf(banner)

        bannersAddOperationContainerService.fillContainers(operationContainer, validBannersToApply)

        bannersAddExecutionGrutService.execute(
            validBannersToApply,
            operationContainer,
            BannerRepositoryContainer(shard),
            BannerAdditionalActionsContainer(userInfo.clientId, clientRegionId)
        )

        bannerTestGrutApi.setStatusModerate(bannerId, bannerStatusModerate)

        return bannerId
    }
}
