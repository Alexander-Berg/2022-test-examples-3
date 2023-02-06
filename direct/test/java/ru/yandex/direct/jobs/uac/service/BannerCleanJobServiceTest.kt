package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbDirectAd
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.TypedCampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.model.createUpdateAdsContainers
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerCleanJobServiceTest : AbstractUacRepositoryJobTest() {
    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var bannerCleanJobService: BannerCleanJobService

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var campaignInfo: TypedCampaignInfo
    private var campaignId = 0L
    private lateinit var adGroupInfo: AdGroupInfo
    private var bannerId = 0L

    private val campaignStatuses = CampaignStatuses(Status.STARTED, TargetStatus.STARTED)

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        campaignInfo = typedCampaignStepsUnstubbed.createDefaultMobileContentCampaign(userInfo, clientInfo)
        campaignId = campaignInfo.id

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())

        bannerId = createBanner()
    }

    private fun createBanner(): Long {
        val banner = TestNewMobileAppBanners
            .fullMobileAppBanner(campaignId, adGroupInfo.adGroupId)
            .withBsBannerId(0L)

        return steps.mobileAppBannerSteps().createMobileAppBanner(NewMobileAppBannerInfo()
            .withClientInfo(clientInfo)
            .withAdGroupInfo(adGroupInfo)
            .withBanner(banner)).bannerId
    }

    @Suppress("unused")
    fun parametersForDeleteAds() =
        listOf(
            Arguments.of("Удаление title баннера",
                MediaType.TITLE, true, false, false, false, false, true),
            Arguments.of("Удаление text баннера",
                MediaType.TEXT, false, true, false, false, false, true),
            Arguments.of("Удаление картинки",
                MediaType.IMAGE, false, false, true, false, false, true),
            Arguments.of("Удаление видео",
                MediaType.VIDEO, false, false, false, true, false, true),
            Arguments.of("Удаление староформатного баннера с картинкой",
                MediaType.IMAGE, false, false, false, false, true, true),
            Arguments.of("Удаление староформатного баннера с видео",
                MediaType.VIDEO, false, false, false, false, true, true),
            Arguments.of("Не связанный title с campaign_content не удаляется",
                MediaType.TITLE, false, false, false, true, false, false),
            Arguments.of("Не связанный text с campaign_content не удаляется",
                MediaType.TEXT, false, false, false, true, false, false),
            Arguments.of("Не связанная картинка с campaign_content не удаляется",
                MediaType.IMAGE, false, false, false, true, false, false),
            Arguments.of("Не связанное видео с campaign_content не удаляется",
                MediaType.VIDEO, true, false, false, false, false, false),
        )

    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForDeleteAds")
    fun clean_TestDeleteDirectAds(
        @Suppress("UNUSED_PARAMETER")
        description: String,
        mediaType: MediaType,
        toTitleContentId: Boolean,
        toTextContentId: Boolean,
        toImageContentId: Boolean,
        toVideoContentId: Boolean,
        toDirectContentId: Boolean,
        deleted: Boolean,
    ) {
        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = mediaType,
            removedAt = LocalDateTime.now(),
        )

        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        val uacDirectAd = createDirectAd(
            titleContentId = if (toTitleContentId) uacCampaignContent.id else UacYdbUtils.generateUniqueRandomId(),
            textContentId = if (toTextContentId) uacCampaignContent.id else UacYdbUtils.generateUniqueRandomId(),
            directImageContentId = if (toImageContentId) uacCampaignContent.id else null,
            directVideoContentId = if (toVideoContentId) uacCampaignContent.id else null,
            directContentId = if (toDirectContentId) uacCampaignContent.id else null,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)

        runCleanFunction(uacCampaign, listOf(uacCampaignContent))

        if (deleted) {
            checkThatBannerIsRemoved(uacDirectAd)
        } else {
            checkThatBannerIsNotRemoved(uacDirectAd)
        }
    }

    /**
     * Если контент кампании не в статусе удаленного -> баннеры не удалятся
     */
    @Test
    fun clean_UacCampaignContentNotRemoved_BannerNotRemoved() {
        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.IMAGE,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val uacDirectAd = createDirectAd(
            directImageContentId = uacCampaignContent.id,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)

        runCleanFunction(uacCampaign, listOf(uacCampaignContent))

        checkThatBannerIsNotRemoved(uacDirectAd)
    }

    @Test
    fun clean_UacCampaignContentRemoved_AllBannersInMySQLAlreadyRemoved_NoFail() {
        steps.bannerSteps().deleteBanners(clientInfo.shard, listOf(bannerId))

        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.IMAGE,
            removedAt = LocalDateTime.now(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val uacDirectAd = createDirectAd(
            directImageContentId = uacCampaignContent.id,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)

        runCleanFunction(uacCampaign, listOf(uacCampaignContent))

        checkThatBannerIsRemoved(uacDirectAd, DirectAdStatus.ERROR_UNKNOWN)
    }

    @Test
    fun clean_UacCampaignContentRemoved_SomeBannersInMySQLAlreadyRemoved_OtherBannersRemoved() {
        steps.bannerSteps().deleteBanners(clientInfo.shard, listOf(bannerId))
        val bannerId2 = createBanner()

        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.IMAGE,
            removedAt = LocalDateTime.now(),
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val uacDirectAd = createDirectAd(
            directImageContentId = uacCampaignContent.id,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        val uacDirectAd2 = createDirectAd(
            directImageContentId = uacCampaignContent.id,
            directAdId = bannerId2,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd2)

        runCleanFunction(uacCampaign, listOf(uacCampaignContent))

        checkThatBannerIsRemoved(uacDirectAd, DirectAdStatus.ERROR_UNKNOWN)
        checkThatBannerIsRemoved(uacDirectAd2)
    }

    /**
     * Если добавляется контент с новым типом, которого раньше не было у кампании, баннера удаляются
     */
    @Test
    fun clean_NewUacCampaignContentType_BannerRemoved() {
        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val textContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.TEXT,
        )
        val imageContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.IMAGE,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(textContent, imageContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val uacDirectAd = createDirectAd(
            textContentId = textContent.id,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)

        runCleanFunction(uacCampaign, listOf(textContent, imageContent))

        checkThatBannerIsRemoved(uacDirectAd)
    }

    /**
     * Если добавляется новый контент с типом "сайтлинк", баннера не удаляются
     */
    @Test
    fun clean_NewUacCampaignContentTypeSitelink_BannerNotRemoved() {
        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val textContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.TEXT,
        )
        val sitelinkContent = createCampaignContent(
            campaignId = uacCampaign.id,
            type = MediaType.SITELINK,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(textContent, sitelinkContent))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId,
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)

        val uacDirectAd = createDirectAd(
            textContentId = textContent.id,
            directAdId = bannerId,
            directAdGroupId = uacDirectAdGroup.id,
        )
        uacYdbDirectAdRepository.saveDirectAd(uacDirectAd)

        runCleanFunction(uacCampaign, listOf(textContent, sitelinkContent))

        checkThatBannerIsNotRemoved(uacDirectAd)
    }

    /**
     * Если есть два баннера и один из них нельзя удалять -> в базе останется только один архивный баннер
     */
    @Test
    fun clean_UacBannerCannotBeDeleted_BannerIsArchived() {
        val bannerForArchive = TestNewMobileAppBanners
            .fullMobileAppBanner(campaignId, adGroupInfo.adGroupId)

        val bannerIdForArchive = steps.mobileAppBannerSteps().createMobileAppBanner(NewMobileAppBannerInfo()
            .withClientInfo(clientInfo)
            .withAdGroupInfo(adGroupInfo)
            .withBanner(bannerForArchive)).bannerId

        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContents = listOf(1, 2).map {
            createCampaignContent(
                campaignId = uacCampaign.id,
                type = MediaType.IMAGE,
                removedAt = LocalDateTime.now(),
            )
        }
        uacYdbCampaignContentRepository.addCampaignContents(uacCampaignContents)
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        val uacDirectAds = listOf(
            createDirectAd(
                directImageContentId = uacCampaignContents[0].id,
                directAdId = bannerId,
                directAdGroupId = uacDirectAdGroup.id,
            ),
            createDirectAd(
                directImageContentId = uacCampaignContents[1].id,
                directAdId = bannerIdForArchive,
                directAdGroupId = uacDirectAdGroup.id,
            )
        )

        uacDirectAds.forEach { uacDirectAd -> uacYdbDirectAdRepository.saveDirectAd(uacDirectAd) }

        runCleanFunction(uacCampaign, uacCampaignContents)

        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard, listOf(adGroupInfo.adGroupId))
        val actualUacDirectAd = uacYdbDirectAdRepository.getByDirectAdGroupId(uacDirectAds.map { it.directAdGroupId!! }, 0L, 1000L)

        // В базе остался только один архивный баннер
        val soft = SoftAssertions()
        soft.assertThat(actualBanners.map { it.id })
            .`as`("id баннера")
            .isEqualTo(listOf(bannerIdForArchive))
        soft.assertThat((actualBanners[0] as BannerWithSystemFields).statusArchived)
            .`as`("статус архивности баннера")
            .isTrue

        soft.assertThat(actualUacDirectAd)
            .`as`("количество баннеров в ydb")
            .hasSize(2)
        soft.assertThat(actualUacDirectAd.map { it.status })
            .`as`("статус баннеров в ydb")
            .containsOnly(DirectAdStatus.DELETED)

        soft.assertAll()
    }

    private fun checkThatBannerIsNotRemoved(uacDirectAd: UacYdbDirectAd) {
        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard, listOf(adGroupInfo.adGroupId))
        val actualUacDirectAd = uacYdbDirectAdRepository.getByDirectAdGroupId(listOf(uacDirectAd.directAdGroupId!!), 0L, 1000L)

        val soft = SoftAssertions()
        soft.assertThat(actualBanners.map { it.id })
            .`as`("id баннера")
            .isEqualTo(listOf(bannerId))
        soft.assertThat((actualBanners[0] as BannerWithSystemFields).statusArchived)
            .`as`("статус архивности баннера")
            .isFalse

        soft.assertThat(actualUacDirectAd)
            .`as`("количество баннеров в ydb")
            .hasSize(1)
        soft.assertThat(actualUacDirectAd[0].status)
            .`as`("статус баннера в ydb")
            .isEqualTo(DirectAdStatus.CREATED)

        soft.assertAll()
    }

    private fun checkThatBannerIsRemoved(
        uacDirectAd: UacYdbDirectAd,
        expectedStatus: DirectAdStatus = DirectAdStatus.DELETED,
    ) {
        val actualBanners = bannerTypedRepository.getBannersByGroupIds(clientInfo.shard, listOf(adGroupInfo.adGroupId))
        val actualUacDirectAds = uacYdbDirectAdRepository.getByDirectAdGroupId(listOf(uacDirectAd.directAdGroupId!!), 0L, 1000L)

        val soft = SoftAssertions()
        soft.assertThat(actualBanners)
            .`as`("в mysql нет баннеров")
            .isEmpty()

        soft.assertThat(actualUacDirectAds.find { it.id == uacDirectAd.id }!!.status)
            .`as`("статус баннера в ydb")
            .isEqualTo(expectedStatus)

        soft.assertAll()
    }

    private fun runCleanFunction(
        uacCampaign: UacYdbCampaign,
        uacCampaignContents: List<UacYdbCampaignContent>,
    ) {
        val containers = createUpdateAdsContainers(
            userInfo.uid,
            clientInfo.client!!,
            uacCampaign,
            uacAdGroupBrief = null,
            campaign = campaignInfo.campaign,
        )
        bannerCleanJobService.clean(
            userInfo.clientId,
            uacCampaign,
            uacAssetsByGroupBriefId = mapOf(null as Long? to uacCampaignContents),
            containers,
            isItCampaignBrief = true,
            migrateToNewBackend = false
        )
    }
}
