package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.CampaignStatuses
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.RecommendedCostType
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbRecommendedCost
import ru.yandex.direct.core.entity.uac.service.UacCampaignService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbRecommendedCostRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps.UacCampaignInfo
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignServiceTest {

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAccountSteps: UacAccountSteps

    @Autowired
    private lateinit var uacCampaignService: UacCampaignService

    @Autowired
    private lateinit var ydbUacCampaignWebService: YdbUacCampaignWebService

    @Autowired
    private lateinit var ydbUacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacYdbRecommendedCostRepository: TestUacYdbRecommendedCostRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    private lateinit var uacCampaignInfo: UacCampaignInfo
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacAccount: UacYdbAccount

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        uacAccount = uacAccountSteps.createAccount(clientInfo)
    }

    @Test
    fun fillCampaignWithInvalidImpressionUrl() {
        val impressionUrl = "https://control.kochava.com/v1/cpi/click?" +
            "campaign_id=koyandex-music-m3svtsg41de342a2d66d47&network_id=3573&ko_exchange=true&site_id=apmetrix" +
            "&device_id=device_id&click_id={logid}&adid={google_aid}"

        uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo, impressionUrl = impressionUrl)

        val uacCampaign = ydbUacCampaignWebService.fillCampaign(
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            uacCampaignInfo.uacCampaign,
            uacCampaignInfo.uacDirectCampaign.directCampaignId,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        uacCampaign.impressionUrl
            .checkEquals(impressionUrl)
    }

    @Test
    fun fillCampaignWithRecommendedCostTest() {
        val recommendedCpa = UacYdbRecommendedCost(
            id = generateUniqueRandomId(),
            platform = Platform.ANDROID,
            recommendedCost = 100,
            type = RecommendedCostType.CPA,
            category = "application",
        )
        val recommendedCpi = UacYdbRecommendedCost(
            id = generateUniqueRandomId(),
            platform = Platform.ANDROID,
            recommendedCost = 120,
            type = RecommendedCostType.CPI,
            category = "application",
        )
        uacYdbRecommendedCostRepository.addRecommendedCost(recommendedCpa)
        uacYdbRecommendedCostRepository.addRecommendedCost(recommendedCpi)

        uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo)

        val uacCampaign = ydbUacCampaignWebService.fillCampaign(
            clientInfo.chiefUserInfo?.user!!,
            clientInfo.chiefUserInfo?.user!!,
            uacCampaignInfo.uacCampaign,
            uacCampaignInfo.uacDirectCampaign.directCampaignId,
            CampaignStatuses(Status.DRAFT, TargetStatus.STOPPED),
        )

        uacCampaign.appInfo?.recommendedCpa.checkEquals(100)
        uacCampaign.appInfo?.recommendedCpi.checkEquals(120)
    }

    @Test
    fun getAppIdsByAccountIdEmptyTest() {
        uacCampaignService.getAppIdsByAccountId(uacAccount.id).checkSize(0)
    }

    @Test
    fun getAppIdsByAccountIdTest() {
        val appInfo1 = defaultAppInfo()
        val appInfo2 = defaultAppInfo()
        val appInfo3 = defaultAppInfo()

        ydbUacAppInfoRepository.saveAppInfo(appInfo1)
        ydbUacAppInfoRepository.saveAppInfo(appInfo2)
        ydbUacAppInfoRepository.saveAppInfo(appInfo3)

        uacCampaignSteps.createMobileAppCampaign(clientInfo, accountId = uacAccount.id, appId = appInfo1.id)
        uacCampaignSteps.createMobileAppCampaign(clientInfo, accountId = uacAccount.id, appId = appInfo1.id)
        uacCampaignSteps.createMobileAppCampaign(clientInfo, accountId = uacAccount.id, appId = appInfo2.id)
        uacCampaignSteps.createTextCampaign(clientInfo, accountId = uacAccount.id)

        val appIds = uacCampaignService.getAppIdsByAccountId(uacAccount.id)
        assertThat(appIds).containsExactlyInAnyOrder(appInfo1.id, appInfo2.id)
    }

    @Test
    fun getAppIdsByAccountIdBigIdTest() {
        uacAccount = uacAccountSteps.createAccount(clientInfo, ULong.MAX_VALUE.toString())

        val appInfo = defaultAppInfo()
        uacCampaignSteps.createMobileAppCampaign(clientInfo, accountId = uacAccount.id, appId = appInfo.id)

        val appIds = uacCampaignService.getAppIdsByAccountId(uacAccount.id)
        assertThat(appIds).containsExactly(appInfo.id)
    }

    @Test
    fun getMinBannerIdForCampaign_withoutBanners_GetNull() {
        val uacCampaignInfo = createDefaultUacCampaign()

        val minDirectBannerId = uacCampaignService.getMinBannerIdForCampaign(uacCampaignInfo.uacCampaign.id)

        assertThat(minDirectBannerId)
            .isNull()
    }

    @Test
    fun getMinBannerIdForCampaign_withOneBanner_GetItId() {
        val uacCampaignInfo = createDefaultUacCampaign()

        val directAdId = 10L

        val uacAdGroup = createDirectAdGroup(directCampaignId = uacCampaignInfo.uacCampaign.id, directAdGroupId = 5L)
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacAdGroup)
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId, directAdGroupId = uacAdGroup.id))

        val minDirectBannerId = uacCampaignService.getMinBannerIdForCampaign(uacCampaignInfo.uacCampaign.id)

        assertThat(minDirectBannerId)
            .isEqualTo(directAdId)
    }

    @Test
    fun getMinBannerIdForCampaign_WithNullBanner_GetMin() {
        val uacCampaignInfo = createDefaultUacCampaign()

        val directAdId1: Long? = null
        val directAdId2 = 15L

        val uacAdGroup = createDirectAdGroup(directCampaignId = uacCampaignInfo.uacCampaign.id, directAdGroupId = 5L)
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacAdGroup)
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId1, directAdGroupId = uacAdGroup.id))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId2, directAdGroupId = uacAdGroup.id))

        val minDirectBannerId = uacCampaignService.getMinBannerIdForCampaign(uacCampaignInfo.uacCampaign.id)

        assertThat(minDirectBannerId)
            .isEqualTo(directAdId2)
    }

    @Test
    fun getMinBannerIdForCampaign_WithOnlyDeletedBanner_GetNull() {
        val uacCampaignInfo = createDefaultUacCampaign()

        val uacAdGroup = createDirectAdGroup(directCampaignId = uacCampaignInfo.uacCampaign.id, directAdGroupId = 5L)
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacAdGroup)
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdId = 5551L,
            directAdGroupId = uacAdGroup.id,
            status = DirectAdStatus.DELETED,
        ))

        val minDirectBannerId = uacCampaignService.getMinBannerIdForCampaign(uacCampaignInfo.uacCampaign.id)

        assertThat(minDirectBannerId)
            .isNull()
    }

    @Test
    fun getMinBannerIdForCampaign_WithManyBanner_GetMin() {
        val uacCampaignInfo = createDefaultUacCampaign()

        val directAdId1 = 5_555_555L
        val directAdId2 = 15L
        val directAdId3: Long? = null
        val deletedDirectAdId4 = 3L
        val directAdId5 = Long.MAX_VALUE

        val uacAdGroup = createDirectAdGroup(directCampaignId = uacCampaignInfo.uacCampaign.id, directAdGroupId = 5L)
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacAdGroup)
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId1, directAdGroupId = uacAdGroup.id))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId2, directAdGroupId = uacAdGroup.id))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId3, directAdGroupId = uacAdGroup.id))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(directAdId = directAdId5, directAdGroupId = uacAdGroup.id))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdId = deletedDirectAdId4,
            directAdGroupId = uacAdGroup.id,
            status = DirectAdStatus.DELETED,
        ))

        val minDirectBannerId = uacCampaignService.getMinBannerIdForCampaign(uacCampaignInfo.uacCampaign.id)

        assertThat(minDirectBannerId)
            .isEqualTo(directAdId2)
    }

    private fun createDefaultUacCampaign(
        uacYdbAppInfo: UacYdbAppInfo = defaultAppInfo()
    ): UacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(
        clientInfo,
        accountId = uacAccount.id,
        appId = uacYdbAppInfo.id
    )
}
