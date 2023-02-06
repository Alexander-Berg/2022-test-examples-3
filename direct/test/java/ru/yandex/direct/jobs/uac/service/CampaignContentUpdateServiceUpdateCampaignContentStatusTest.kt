package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.Assertions
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
import ru.yandex.direct.common.db.PpcPropertyNames.UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAdGroup
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners
import ru.yandex.direct.core.testing.data.TestNewTextBanners
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest

/**
 * Проверяем обновление статуса у контента кампании в ydb через вызов
 * функции updateCampaignContentsAndCampaignFlags в [CampaignContentUpdateService]
 */
@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CampaignContentUpdateServiceUpdateCampaignContentStatusTest : AbstractUacRepositoryJobTest() {

    @Autowired
    private lateinit var campaignContentUpdateService: CampaignContentUpdateService

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var mobileContentCampaignInfo: TypedCampaignInfo
    private lateinit var mobileContentAdGroupInfo: AdGroupInfo
    private lateinit var textCampaignInfo: TypedCampaignInfo
    private lateinit var textAdGroupInfo: AdGroupInfo
    private lateinit var uacAppInfo: UacYdbAppInfo
    private lateinit var mobileContentUacCampaign: UacYdbCampaign
    private lateinit var mobileContentUacDirectAdGroup: UacYdbDirectAdGroup
    private lateinit var textUacCampaign: UacYdbCampaign
    private lateinit var textUacDirectAdGroup: UacYdbDirectAdGroup

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        steps.trustedRedirectSteps().addValidCounters()

        val mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL)
        mobileContentCampaignInfo = typedCampaignStepsUnstubbed.createMobileContentCampaign(userInfo, clientInfo,
            TestCampaigns.defaultMobileContentCampaignWithSystemFields(clientInfo)
                .withStrategy(TestCampaignsStrategy.defaultAutobudgetStrategy())
                .withMobileAppId(mobileAppInfo.mobileAppId))

        textCampaignInfo = typedCampaignStepsUnstubbed.createDefaultTextCampaign(userInfo, clientInfo)

        mobileContentAdGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(mobileContentCampaignInfo.toCampaignInfo())
        textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(mobileContentCampaignInfo.toCampaignInfo())

        uacAppInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo)

        mobileContentUacCampaign = createYdbCampaign(appId = uacAppInfo.id)
        textUacCampaign = createYdbCampaign(advType = AdvType.TEXT)
        uacYdbCampaignRepository.addCampaign(mobileContentUacCampaign)

        mobileContentUacDirectAdGroup = createDirectAdGroup(
            directAdGroupId = mobileContentAdGroupInfo.adGroupId,
            directCampaignId = mobileContentUacCampaign.id,
        )

        textUacDirectAdGroup = createDirectAdGroup(
            directAdGroupId = textAdGroupInfo.adGroupId,
            directCampaignId = textUacCampaign.id,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(mobileContentUacDirectAdGroup)
    }

    @Suppress("unused")
    fun parametersForTextCampaignContentStatus() =
        listOf(
            Arguments.of("BannerStatusModerate=YES для текстового контента -> ACTIVE",
                BannerStatusModerate.YES, CampaignContentStatus.ACTIVE),
            Arguments.of("BannerStatusModerate=NO для текстового контента -> REJECTED",
                BannerStatusModerate.NO, CampaignContentStatus.REJECTED),
            Arguments.of("BannerStatusModerate=SENT для текстового контента -> MODERATING",
                BannerStatusModerate.SENT, CampaignContentStatus.MODERATING),
            Arguments.of("BannerStatusModerate=NEW для текстового контента -> CREATED",
                BannerStatusModerate.NEW, CampaignContentStatus.CREATED),
            Arguments.of("BannerStatusModerate=READY для текстового контента -> CREATED",
                BannerStatusModerate.READY, CampaignContentStatus.CREATED),
            Arguments.of("BannerStatusModerate=SENDING для текстового контента -> CREATED",
                BannerStatusModerate.SENDING, CampaignContentStatus.CREATED),
        )

    /**
     * Проверяем изменение статуса текстового контента content_campaign при разных параметрах
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForTextCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByTextState(
        description: String,
        bannerStatusModerate: BannerStatusModerate,
        expectStatus: CampaignContentStatus,
    ) {
        val bannerId = createMobileContentBanner(bannerStatusModerate)

        val titleCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.TITLE,
        )
        val textCampaignContent = createTextCampaignContent(
            campaignId = mobileContentUacCampaign.id,
        )
        val campaignContents = listOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)

        createUacYdbDirectAd(bannerId, titleCampaignContent, textCampaignContent, null, null)

        val expectCampaignContentIdToStatus = mutableMapOf(
            titleCampaignContent.id to expectStatus,
            textCampaignContent.id to expectStatus,
        )

        callAndCheckUpdateCampaignContentsStatus(campaignContents, expectCampaignContentIdToStatus)
    }


    /**
     * Если у ассета еще не сгенерировались баннеры, и не включена пропертя UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS,
     * то для таких ассетов будет статус rejected
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_AssetWithoutBanners_Rejected(
    ) {
        val titleCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.TITLE,
        )
        val textCampaignContent = createTextCampaignContent(
            campaignId = mobileContentUacCampaign.id,
        )
        val campaignContents = listOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)


        val expectCampaignContentIdToStatus = mutableMapOf(
            titleCampaignContent.id to CampaignContentStatus.REJECTED,
            textCampaignContent.id to CampaignContentStatus.REJECTED,
        )

        callAndCheckUpdateCampaignContentsStatus(campaignContents, expectCampaignContentIdToStatus)
    }

    /**
     * Если у ассета еще не сгенерировались баннеры, и включена пропертя UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS,
     * то для таких ассетов будет статус moderating
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_AssetWithoutBanners_Moderating(
    ) {
        ppcPropertiesSupport.set(UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS, "true")
        val titleCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.TITLE,
        )
        val textCampaignContent = createTextCampaignContent(
            campaignId = mobileContentUacCampaign.id,
        )
        val campaignContents = listOf(titleCampaignContent, textCampaignContent)
        uacYdbCampaignContentRepository.addCampaignContents(campaignContents)


        val expectCampaignContentIdToStatus = mutableMapOf(
            titleCampaignContent.id to CampaignContentStatus.MODERATING,
            textCampaignContent.id to CampaignContentStatus.MODERATING,
        )

        callAndCheckUpdateCampaignContentsStatus(campaignContents, expectCampaignContentIdToStatus)
        ppcPropertiesSupport.set(UAC_MODERATING_STATUS_FOR_ASSETS_WITHOUT_BANNERS, "false")
    }

    @Suppress("unused")
    fun parametersForImageCampaignContentStatus() =
        listOf(
            Arguments.of("StatusBannerImageModerate=YES для image контента -> ACTIVE",
                StatusBannerImageModerate.YES, CampaignContentStatus.ACTIVE),
            Arguments.of("StatusBannerImageModerate=NO для image контента -> REJECTED",
                StatusBannerImageModerate.NO, CampaignContentStatus.REJECTED),
            Arguments.of("StatusBannerImageModerate=SENT для image контента -> MODERATING",
                StatusBannerImageModerate.SENT, CampaignContentStatus.MODERATING),
            Arguments.of("StatusBannerImageModerate=SENDING для image контента -> MODERATING",
                StatusBannerImageModerate.SENDING, CampaignContentStatus.MODERATING),
            Arguments.of("StatusBannerImageModerate=NEW для image контента -> CREATED",
                StatusBannerImageModerate.NEW, CampaignContentStatus.CREATED),
            Arguments.of("StatusBannerImageModerate=READY для image контента -> CREATED",
                StatusBannerImageModerate.READY, CampaignContentStatus.CREATED),
        )

    /**
     * Проверяем изменение статуса image контента content_campaign при разных параметрах
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForImageCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByImageState(
        description: String,
        statusBannerImageModerate: StatusBannerImageModerate,
        expectStatus: CampaignContentStatus,
    ) {
        val bannerId = createMobileContentBanner(statusBannerImageModerate = statusBannerImageModerate)

        val imageCampaignContent = createImageCampaignContent(
            campaignId = mobileContentUacCampaign.id,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(imageCampaignContent))

        createUacYdbDirectAd(bannerId, null, null, imageCampaignContent, null)

        val expectCampaignContentIdToStatus = mutableMapOf(
            imageCampaignContent.id to expectStatus
        )

        callAndCheckUpdateCampaignContentsStatus(listOf(imageCampaignContent), expectCampaignContentIdToStatus)
    }

    @Suppress("unused")
    fun parametersForVideoCampaignContentStatus() =
        listOf(
            Arguments.of("BannerCreativeStatusModerate=YES для video контента -> ACTIVE",
                BannerCreativeStatusModerate.YES, CampaignContentStatus.ACTIVE),
            Arguments.of("BannerCreativeStatusModerate=NO для video контента -> REJECTED",
                BannerCreativeStatusModerate.NO, CampaignContentStatus.REJECTED),
            Arguments.of("BannerCreativeStatusModerate=SENT для video контента -> MODERATING",
                BannerCreativeStatusModerate.SENT, CampaignContentStatus.MODERATING),
            Arguments.of("BannerCreativeStatusModerate=SENDING для video контента -> MODERATING",
                BannerCreativeStatusModerate.SENDING, CampaignContentStatus.MODERATING),
            Arguments.of("BannerCreativeStatusModerate=NEW для video контента -> CREATED",
                BannerCreativeStatusModerate.NEW, CampaignContentStatus.CREATED),
            Arguments.of("BannerCreativeStatusModerate=READY для video контента -> CREATED",
                BannerCreativeStatusModerate.READY, CampaignContentStatus.CREATED),
        )

    @Suppress("unused")
    fun parametersForSitelinksSetCampaignContentStatus() =
        listOf(
            Arguments.of("BannerStatusSitelinksModerate=YES для sitelinkSet -> ACTIVE",
                BannerStatusSitelinksModerate.YES, CampaignContentStatus.ACTIVE),
            Arguments.of("BannerStatusSitelinksModerate=NO для sitelinkSet -> REJECTED",
                BannerStatusSitelinksModerate.NO, CampaignContentStatus.REJECTED),
            Arguments.of("BannerStatusSitelinksModerate=SENT для sitelinkSet -> MODERATING",
                BannerStatusSitelinksModerate.SENT, CampaignContentStatus.MODERATING),
            Arguments.of("BannerStatusSitelinksModerate=SENDING для sitelinkSet -> MODERATING",
                BannerStatusSitelinksModerate.SENDING, CampaignContentStatus.MODERATING),
            Arguments.of("BannerStatusSitelinksModerate=NEW для sitelinkSet -> CREATED",
                BannerStatusSitelinksModerate.NEW, CampaignContentStatus.CREATED),
            Arguments.of("BannerStatusSitelinksModerate=READY для sitelinkSet -> CREATED",
                BannerStatusSitelinksModerate.READY, CampaignContentStatus.CREATED),
        )

    /**
     * Проверяем изменение статуса video контента content_campaign при разных параметрах
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForVideoCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusByVideoState(
        description: String,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate,
        expectStatus: CampaignContentStatus,
    ) {
        val bannerId = createMobileContentBanner(bannerCreativeStatusModerate = bannerCreativeStatusModerate)

        val videoCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.VIDEO,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(videoCampaignContent))

        createUacYdbDirectAd(bannerId, null, null, null, videoCampaignContent)

        val expectCampaignContentIdToStatus = mutableMapOf(
            videoCampaignContent.id to expectStatus
        )

        callAndCheckUpdateCampaignContentsStatus(listOf(videoCampaignContent), expectCampaignContentIdToStatus)
    }


    /**
     * Проверяем изменение статуса сайтлинков в content_campaign при разных параметрах
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForSitelinksSetCampaignContentStatus")
    fun updateCampaignContentsAndCampaignFlags_CheckStatusBySitelinksSetState(
        description: String,
        siteLinksSetStatusModerate: BannerStatusSitelinksModerate,
        expectStatus: CampaignContentStatus,
    ) {
        val bannerId = createTextBanner(textCampaignInfo, textAdGroupInfo, bannerSiteLinksSetStatusModerate = siteLinksSetStatusModerate)

        val siteLinksCampaignContent1 = createCampaignContent(
            campaignId = textUacCampaign.id,
            type = MediaType.SITELINK,
        )
        val siteLinksCampaignContent2 = createCampaignContent(
            campaignId = textUacCampaign.id,
            type = MediaType.SITELINK,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(siteLinksCampaignContent1, siteLinksCampaignContent2))

        createUacYdbDirectAd(bannerId, adGroup = textUacDirectAdGroup)

        val expectCampaignContentIdToStatus = mutableMapOf(
            siteLinksCampaignContent1.id to expectStatus,
            siteLinksCampaignContent2.id to expectStatus,
        )

        callAndCheckUpdateCampaignContentsStatus(listOf(siteLinksCampaignContent1, siteLinksCampaignContent2), expectCampaignContentIdToStatus, campaign = textUacCampaign, adGroup = textUacDirectAdGroup)
    }

    /**
     * Если контент был удален -> статус DELETED
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_DeletedCampaignContents_DeletedStatus() {
        val bannerId = createMobileContentBanner()

        val titleCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.TITLE,
            removedAt = LocalDateTime.now(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(titleCampaignContent))

        val textCampaignContent = createTextCampaignContent(
            campaignId = mobileContentUacCampaign.id,
        )

        createUacYdbDirectAd(bannerId, titleCampaignContent, textCampaignContent, null, null)

        val expectCampaignContentIdToStatus = mutableMapOf(
            titleCampaignContent.id to CampaignContentStatus.DELETED
        )

        callAndCheckUpdateCampaignContentsStatus(listOf(titleCampaignContent), expectCampaignContentIdToStatus)
    }

    /**
     * Если у контента нет данных по статусам (например, нет баннеров) -> статус REJECTED
     */
    @Test
    fun updateCampaignContentsAndCampaignFlags_NoBanners_RejectedStatus() {
        val titleCampaignContent = createCampaignContent(
            campaignId = mobileContentUacCampaign.id,
            type = MediaType.TITLE,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(titleCampaignContent))

        createUacYdbDirectAd(55L, titleCampaignContent, null, null, null)

        val expectCampaignContentIdToStatus = mutableMapOf(
            titleCampaignContent.id to CampaignContentStatus.REJECTED
        )

        callAndCheckUpdateCampaignContentsStatus(listOf(titleCampaignContent), expectCampaignContentIdToStatus)
    }


    private fun createMobileContentBanner(
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.YES,
        statusBannerImageModerate: StatusBannerImageModerate = StatusBannerImageModerate.YES,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate = BannerCreativeStatusModerate.YES,
    ): Long {
        val creativeInfo = steps.creativeSteps().createCreative(clientInfo)
        val imageAdImageFormat = steps.bannerSteps().createWideImageFormat(clientInfo)

        val banner = TestNewMobileAppBanners
            .fullMobileAppBanner(mobileContentCampaignInfo.id, mobileContentAdGroupInfo.adGroupId)
            .withStatusModerate(bannerStatusModerate)
            .withImageStatusModerate(statusBannerImageModerate)
            .withBsBannerId(0L)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withCreativeId(creativeInfo.creativeId)
            .withImageHash(imageAdImageFormat.imageHash)
            .withImageStatusShow(true)
            .withBsBannerId(0L)
            .withImageBsBannerId(0L)
            .withImageDateAdded(LocalDateTime.now())


        return steps.mobileAppBannerSteps()
            .createMobileAppBanner(NewMobileAppBannerInfo()
                .withClientInfo(clientInfo)
                .withAdGroupInfo(mobileContentAdGroupInfo)
                .withImageFormat(imageAdImageFormat)
                .withBanner(banner)).bannerId
    }


    private fun createTextBanner(
        campaignInfo: TypedCampaignInfo = mobileContentCampaignInfo,
        asGroupInfo: AdGroupInfo = mobileContentAdGroupInfo,
        bannerStatusModerate: BannerStatusModerate = BannerStatusModerate.YES,
        statusBannerImageModerate: StatusBannerImageModerate = StatusBannerImageModerate.YES,
        bannerCreativeStatusModerate: BannerCreativeStatusModerate = BannerCreativeStatusModerate.YES,
        bannerSiteLinksSetStatusModerate: BannerStatusSitelinksModerate = BannerStatusSitelinksModerate.YES,
    ): Long {
        val creativeInfo = steps.creativeSteps().createCreative(clientInfo)

        val banner = TestNewTextBanners
            .fullTextBanner(campaignInfo.id, asGroupInfo.adGroupId)
            .withStatusModerate(bannerStatusModerate)
            .withImageStatusModerate(statusBannerImageModerate)
            .withBsBannerId(0L)
            .withCreativeStatusModerate(bannerCreativeStatusModerate)
            .withCreativeId(creativeInfo.creativeId)
            .withBsBannerId(0L)
            .withImageBsBannerId(0L)
            .withImageDateAdded(LocalDateTime.now())
            .withStatusSitelinksModerate(bannerSiteLinksSetStatusModerate)


        return steps.textBannerSteps()
            .createBanner(NewTextBannerInfo()
                .withClientInfo(clientInfo)
                .withAdGroupInfo(textAdGroupInfo)
                .withBanner(banner)).bannerId
    }

    private fun createUacYdbDirectAd(
        bannerId: Long,
        titleCampaignContent: UacYdbCampaignContent? = null,
        textCampaignContent: UacYdbCampaignContent? = null,
        imageCampaignContent: UacYdbCampaignContent? = null,
        videoCampaignContent: UacYdbCampaignContent? = null,
        adGroup: UacYdbDirectAdGroup = mobileContentUacDirectAdGroup,
    ) {
        if (titleCampaignContent != null && textCampaignContent != null) {
            uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
                titleContentId = titleCampaignContent.id,
                textContentId = textCampaignContent.id,
                directAdGroupId = adGroup.id,
                directAdId = bannerId,
            ))
        }
        if (imageCampaignContent != null) {
            uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
                directImageContentId = imageCampaignContent.id,
                directAdGroupId = adGroup.id,
                directAdId = bannerId,
            ))
        }
        if (videoCampaignContent != null) {
            uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
                directVideoContentId = videoCampaignContent.id,
                directAdGroupId = adGroup.id,
                directAdId = bannerId,
            ))
        }
        if (titleCampaignContent == null && textCampaignContent == null && imageCampaignContent == null && videoCampaignContent == null) {
            uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
                directAdGroupId = adGroup.id,
                directAdId = bannerId,
            ))
        }
    }

    private fun callAndCheckUpdateCampaignContentsStatus(
        uacCampaignContents: List<UacYdbCampaignContent>,
        expectCampaignContentIdToStatus: Map<String, CampaignContentStatus>,
        campaign: UacYdbCampaign = mobileContentUacCampaign,
        adGroup: UacYdbDirectAdGroup = mobileContentUacDirectAdGroup,
    ) {
        campaignContentUpdateService.updateCampaignContentsAndCampaignFlags(campaign.id,
            userInfo.clientId, listOf(adGroup), uacCampaignContents)

        val actualCampaignContentIdToStatus = uacYdbCampaignContentRepository.getCampaignContents(campaign.id)
            .associateBy({ it.id }, { it.status })

        Assertions.assertThat(actualCampaignContentIdToStatus)
            .`as`("статус campaign_contents")
            .isEqualTo(expectCampaignContentIdToStatus)
    }
}
