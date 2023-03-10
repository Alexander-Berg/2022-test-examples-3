package ru.yandex.direct.jobs.uac.service

import java.time.Instant
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.db.PpcPropertyNames.FETCH_DATA_FROM_GRUT_FOR_ASSET_LINK_STATUSES
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.entity.uac.service.GrutCampaignContentUpdateService
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.tracing.util.TraceUtil
import ru.yandex.grut.objects.proto.AssetLink
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus.ALS_ACTIVE
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus.ALS_CREATED
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus.ALS_DELETED
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus.ALS_MODERATING
import ru.yandex.grut.objects.proto.AssetLink.TAssetLinkStatus.EAssetLinkStatus.ALS_REJECTED
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_IMAGE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_SITELINK
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TEXT
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_TITLE
import ru.yandex.grut.objects.proto.MediaType.EMediaType.MT_VIDEO
import ru.yandex.grut.objects.proto.RejectReasons


/**
 * ?????????????????? ???????????????????? ?????????????? ?? ???????????????? ???????????????? ?? ?????????? ?????????? ??????????
 * ?????????????? updateCampaignContentsAndCampaignFlags ?? [CampaignContentUpdateService]
 */
@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class GrutCampaignContentUpdateServiceUpdateCampaignContentStatusTest : BaseGrutCampaignContentUpdateServiceTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: GrutCampaignContentUpdateService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    open var adGroupBriefEnabled = false

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        createGrutClient(clientInfo)
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)

        ppcPropertiesSupport.set(FETCH_DATA_FROM_GRUT_FOR_ASSET_LINK_STATUSES, "true")
    }

    @AfterEach
    fun after() {
        ppcPropertiesSupport.remove(FETCH_DATA_FROM_GRUT_FOR_ASSET_LINK_STATUSES)
    }

    @Suppress("unused")
    fun parametersForTextCampaignContentStatus() =
        listOf(
            Arguments.of("BannerStatusModerate=YES ?????? ???????????????????? ???????????????? -> ACTIVE",
                BannerStatusModerate.YES, ALS_ACTIVE, false),
            Arguments.of("BannerStatusModerate=NO ?????? ???????????????????? ???????????????? -> REJECTED",
                BannerStatusModerate.NO, ALS_REJECTED, false),
            Arguments.of("BannerStatusModerate=SENT ?????? ???????????????????? ???????????????? -> MODERATING",
                BannerStatusModerate.SENT, ALS_MODERATING, false),
            Arguments.of("BannerStatusModerate=NEW ?????? ???????????????????? ???????????????? -> CREATED",
                BannerStatusModerate.NEW, ALS_CREATED, false),
            Arguments.of("BannerStatusModerate=READY ?????? ???????????????????? ???????????????? -> CREATED (???????? MODERATING ?????? ??????????)",
                BannerStatusModerate.READY, ALS_CREATED, false),
            Arguments.of("BannerStatusModerate=SENDING ?????? ???????????????????? ???????????????? -> CREATED (???????? MODERATING ?????? ??????????)",
                BannerStatusModerate.SENDING, ALS_CREATED, false),

            Arguments.of("[GrUT] BannerStatusModerate=YES ?????? ???????????????????? ???????????????? -> ACTIVE",
                BannerStatusModerate.YES, ALS_ACTIVE, true),
            Arguments.of("[GrUT] BannerStatusModerate=NO ?????? ???????????????????? ???????????????? -> REJECTED",
                BannerStatusModerate.NO, ALS_REJECTED, true),
            Arguments.of("[GrUT] BannerStatusModerate=SENT ?????? ???????????????????? ???????????????? -> MODERATING",
                BannerStatusModerate.SENT, ALS_MODERATING, true),
            Arguments.of("[GrUT] BannerStatusModerate=NEW ?????? ???????????????????? ???????????????? -> CREATED",
                BannerStatusModerate.NEW, ALS_CREATED, true),
            Arguments.of("[GrUT] BannerStatusModerate=READY ?????? ???????????????????? ???????????????? -> MODERATING",
                BannerStatusModerate.READY, ALS_MODERATING, true),
            Arguments.of("[GrUT] BannerStatusModerate=SENDING ?????? ???????????????????? ???????????????? -> MODERATING",
                BannerStatusModerate.SENDING, ALS_MODERATING, true),
        )

    /**
     * ?????????????????? ?????????????????? ?????????????? ???????????????????? ???????????????? content_campaign ?????? ???????????? ????????????????????
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForTextCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByTextState(
        description: String,
        bannerStatusModerate: BannerStatusModerate,
        expectStatus: EAssetLinkStatus,
        bannerInGrut: Boolean,
    ) {
        val testData = createTestData(
            setOf(MT_TITLE, MT_TEXT),
            newAssetLinkIdGeneration = true,
            adGroupBriefEnabled = adGroupBriefEnabled
        )
        val bannerId = createMobileContentBanner(testData, bannerStatusModerate = bannerStatusModerate, inGrut = bannerInGrut)
        createGrutBanner(testData, bannerId)

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                testData.bannerAssetLinkIds.map { assetLinkId ->
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(assetLinkId.toIdLong())
                        .setAssetId(testData.assetLinkIdToAssetId[assetLinkId]!!.toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(expectStatus)
                        .build()
                }
            )
        }.build()

        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    /**
     * ???????? ?? ???????????? ?????? ???? ?????????????????????????????? ??????????????, ?? ???? ???????????????? ???????????????? UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS,
     * ???? ?????? ?????????? ?????????????? ?????????? ???????????? rejected
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_AssetWithoutBanners_Rejected(
    ) {
        val testData = createTestData(
            setOf(MT_TITLE, MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                testData.bannerAssetIds.map {
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(it.toIdLong())
                        .setAssetId(it.toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(ALS_REJECTED)
                        .build()
                }
            )
        }.build()

        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    /**
     * ???????? ?? ???????????? ?????? ???? ?????????????????????????????? ??????????????, ?? ???????????????? ???????????????? UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS,
     * ???? ?????? ?????????? ?????????????? ?????????? ???????????? moderating
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_AssetWithoutBanners_Moderating(
    ) {
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS, "true")
        val testData = createTestData(
            setOf(MT_TITLE, MT_TEXT),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                testData.bannerAssetIds.map {
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(it.toIdLong())
                        .setAssetId(it.toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(ALS_MODERATING)
                        .build()
                }
            )
        }.build()

        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
        ppcPropertiesSupport.set(PpcPropertyNames.UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS, "false")
    }

    @Suppress("unused")
    fun parametersForImageCampaignContentStatus() =
        listOf(
            Arguments.of("StatusBannerImageModerate=YES ?????? image ???????????????? -> ACTIVE",
                StatusBannerImageModerate.YES, ALS_ACTIVE),
            Arguments.of("StatusBannerImageModerate=NO ?????? image ???????????????? -> REJECTED",
                StatusBannerImageModerate.NO, ALS_REJECTED),
            Arguments.of("StatusBannerImageModerate=SENT ?????? image ???????????????? -> MODERATING",
                StatusBannerImageModerate.SENT, ALS_MODERATING),
            Arguments.of("StatusBannerImageModerate=SENDING ?????? image ???????????????? -> MODERATING",
                StatusBannerImageModerate.SENDING, ALS_MODERATING),
            Arguments.of("StatusBannerImageModerate=NEW ?????? image ???????????????? -> CREATED",
                StatusBannerImageModerate.NEW, ALS_CREATED),
            Arguments.of("StatusBannerImageModerate=READY ?????? image ???????????????? -> CREATED",
                StatusBannerImageModerate.READY, ALS_CREATED),
        )


    /**
     * ?????????????????? ?????????????????? ?????????????? image ???????????????? content_campaign ?????? ???????????? ????????????????????
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForImageCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByImageState(
        description: String,
        statusBannerImageModerate: StatusBannerImageModerate,
        expectStatus: EAssetLinkStatus,
    ) {
        val testData = createTestData(
            assetTypes = setOf(MT_IMAGE),
            newAssetLinkIdGeneration = true,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, statusBannerImageModerate = statusBannerImageModerate)
        createGrutBanner(testData, bannerId)

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                listOf(
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(testData.bannerAssetLinkIds[0].toIdLong())
                        .setAssetId(testData.bannerAssetIds[0].toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(expectStatus).build()
                ))
        }.build()
        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    @Suppress("unused")
    fun parametersForVideoCampaignContentStatus() =
        listOf(
            Arguments.of("BannerCreativeStatusModerate=YES ?????? video ???????????????? -> ACTIVE",
                BannerCreativeStatusModerate.YES, ALS_ACTIVE),
            Arguments.of("BannerCreativeStatusModerate=NO ?????? video ???????????????? -> REJECTED",
                BannerCreativeStatusModerate.NO, ALS_REJECTED),
            Arguments.of("BannerCreativeStatusModerate=SENT ?????? video ???????????????? -> MODERATING",
                BannerCreativeStatusModerate.SENT, ALS_MODERATING),
            Arguments.of("BannerCreativeStatusModerate=SENDING ?????? video ???????????????? -> MODERATING",
                BannerCreativeStatusModerate.SENDING, ALS_MODERATING),
            Arguments.of("BannerCreativeStatusModerate=NEW ?????? video ???????????????? -> CREATED",
                BannerCreativeStatusModerate.NEW, ALS_CREATED),
            Arguments.of("BannerCreativeStatusModerate=READY ?????? video ???????????????? -> CREATED",
                BannerCreativeStatusModerate.READY, ALS_CREATED),
        )

    /**
     * ?????????????????? ?????????????????? ?????????????? video ???????????????? content_campaign ?????? ???????????? ????????????????????
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForVideoCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByVideoState(
        description: String,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate,
        expectStatus: EAssetLinkStatus,
    ) {
        val testData = createTestData(
            setOf(MT_VIDEO),
            newAssetLinkIdGeneration = true,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData, bannerCreativeStatusModerate = bannerCreativeStatusModerate)
        createGrutBanner(testData, bannerId)

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                listOf(
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetId(testData.bannerAssetIds[0].toIdLong())
                        .setAssetLinkId(testData.bannerAssetLinkIds[0].toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(expectStatus).build()
                ))
        }.build()
        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    @Suppress("unused")
    fun parametersForSitelinksSetCampaignContentStatus() =
        listOf(
            Arguments.of("BannerStatusSitelinksModerate=YES ?????? sitelinkSet -> ACTIVE",
                BannerStatusSitelinksModerate.YES, ALS_ACTIVE),
            Arguments.of("BannerStatusSitelinksModerate=NO ?????? sitelinkSet -> REJECTED",
                BannerStatusSitelinksModerate.NO, ALS_REJECTED),
            Arguments.of("BannerStatusSitelinksModerate=SENT ?????? sitelinkSet -> MODERATING",
                BannerStatusSitelinksModerate.SENT, ALS_MODERATING),
            Arguments.of("BannerStatusSitelinksModerate=SENDING ?????? sitelinkSet -> MODERATING",
                BannerStatusSitelinksModerate.SENDING, ALS_MODERATING),
            Arguments.of("BannerStatusSitelinksModerate=NEW ?????? sitelinkSet -> CREATED",
                BannerStatusSitelinksModerate.NEW, ALS_CREATED),
            Arguments.of("BannerStatusSitelinksModerate=READY ?????? sitelinkSet -> CREATED",
                BannerStatusSitelinksModerate.READY, ALS_CREATED),
        )

    /**
     * ?????????????????? ?????????????????? ?????????????? video ???????????????? content_campaign ?????? ???????????? ????????????????????
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForSitelinksSetCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusBySitelinksSetState(
        description: String,
        bannerStatusSitelinksModerate: BannerStatusSitelinksModerate,
        expectStatus: EAssetLinkStatus,
    ) {
        val testData = createTestData(
            setOf(MT_SITELINK),
            campaignType = CampaignType.TEXT,
            newAssetLinkIdGeneration = true,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createTextBanner(testData, bannerSitelinksSetStatusModerate = bannerStatusSitelinksModerate)
        createGrutBanner(testData, bannerId)

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                listOf(
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(testData.assetLinkIdToAssetId.keys.first().toIdLong())
                        .setAssetId(testData.assetIdsByAssetTypes.values.first().toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(expectStatus).build()
                ))
        }.build()
        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    /**
     * ???????? ?????????????? ?????? ???????????? -> ???????????? DELETED
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_DeletedCampaignContents_DeletedStatus() {
        val testData = createTestData(
            setOf(MT_TITLE),
            assetLinksRemoveAt = Instant.now().epochSecond,
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        val bannerId = createMobileContentBanner(testData)
        createGrutBanner(testData, bannerId)

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                listOf(
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(testData.bannerAssetIds[0].toIdLong())
                        .setAssetId(testData.bannerAssetIds[0].toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(ALS_DELETED).build()
                ))
        }.build()
        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    /**
     * ???????? ?? ???????????????? ?????? ???????????? ???? ???????????????? (????????????????, ?????? ????????????????) -> ???????????? REJECTED
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_NoBanners_RejectedStatus() {
        val testData = createTestData(
            setOf(MT_TITLE),
            adGroupBriefEnabled = adGroupBriefEnabled,
        )
        createGrutBanner(testData, TraceUtil.randomId())

        val expectedAssetLinkStatuses = AssetLink.TAssetLinksStatuses.newBuilder().apply {
            addAllLinkStatuses(
                listOf(
                    AssetLink.TAssetLinkStatus.newBuilder()
                        .setAssetLinkId(testData.bannerAssetIds[0].toIdLong())
                        .setAssetId(testData.bannerAssetIds[0].toIdLong())
                        .setRejectReasons(RejectReasons.TRejectReasons.newBuilder().build())
                        .setStatus(ALS_REJECTED).build()
                ))
        }.build()
        callAndCheckUpdateCampaignContentsStatus(testData.campaignInfo, expectedAssetLinkStatuses)
    }

    private fun callAndCheckUpdateCampaignContentsStatus(
        campaignInfo: TypedCampaignInfo,
        expectedAssetLinkStatuses: AssetLink.TAssetLinksStatuses,
    ) {
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(campaignInfo.shard, listOf(campaignInfo.id.toLong()))

        grutApiService.briefGrutApi.getBrief(campaignInfo.id)
        val gotCampaign = grutApiService.briefGrutApi.getBrief(campaignInfo.id)!!
        val actualAssetLinkStatuses = gotCampaign.spec.briefAssetLinksStatuses

        Assertions.assertThat(actualAssetLinkStatuses)
            .`as`("???????????? campaign_contents")
            .isEqualTo(expectedAssetLinkStatuses)
    }


}
