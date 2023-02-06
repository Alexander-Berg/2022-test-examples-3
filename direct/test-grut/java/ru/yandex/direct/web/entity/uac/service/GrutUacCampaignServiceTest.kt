package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutUacCampaignServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var ydbUacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacCampaignService: GrutUacCampaignService

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)
    }

    @Test
    fun getAppIdsByAccountIdEmptyTest() {
        uacCampaignService.getAppIdsByAccountId(clientInfo.clientId.toString()).checkSize(0)
    }

    @Test
    fun getAppIdsByAccountIdTest() {
        val appInfo1 = defaultAppInfo()
        val appInfo2 = defaultAppInfo()
        val appInfo3 = defaultAppInfo()

        ydbUacAppInfoRepository.saveAppInfo(appInfo1)
        ydbUacAppInfoRepository.saveAppInfo(appInfo2)
        ydbUacAppInfoRepository.saveAppInfo(appInfo3)

        grutSteps.createMobileAppCampaign(clientInfo, appId = appInfo1.id)
        grutSteps.createMobileAppCampaign(clientInfo, appId = appInfo1.id)
        grutSteps.createMobileAppCampaign(clientInfo, appId = appInfo2.id)
        grutSteps.createTextCampaign(clientInfo)

        val appIds = uacCampaignService.getAppIdsByAccountId(clientInfo.clientId.toString())
        assertThat(appIds).containsExactlyInAnyOrder(appInfo1.id, appInfo2.id)
    }

    @Test
    fun getMinBannerIdEmptyTest() {
        val appInfo = defaultAppInfo()
        val campaignId = grutSteps.createMobileAppCampaign(clientInfo, appId = appInfo.id).toIdString()

        assertThat(uacCampaignService.getMinBannerIdForCampaign(campaignId))
            .isNull()
    }

    @Test
    fun getMinBannerIdTest() {
        val appInfo = defaultAppInfo()
        val campaignId = grutSteps.createMobileAppCampaign(clientInfo, appId = appInfo.id)
        val adGroupId = grutSteps.createAdGroup(campaignId)

        grutSteps.createBanner(
            campaignId,
            adGroupId,
            bannerId = 1 + UacYdbUtils.generateUniqueRandomId().substring(1).toIdLong(),
            status = EBannerStatus.BSS_DELETED
        )

        val expectedBannerId = grutSteps.createBanner(
            campaignId,
            adGroupId,
            bannerId = 2 + UacYdbUtils.generateUniqueRandomId().substring(1).toIdLong()
        )
        grutSteps.createBanner(
            campaignId,
            adGroupId,
            bannerId = 3 + UacYdbUtils.generateUniqueRandomId().substring(1).toIdLong()
        )

        assertThat(uacCampaignService.getMinBannerIdForCampaign(campaignId.toIdString()))
            .isEqualTo(expectedBannerId)
    }

    @Test
    fun getCampaignsByFeedIdsTest() {
        val feedId = steps.feedSteps().createDefaultFeed().feedId
        grutSteps.createAndGetTextCampaign(clientInfo, true, feedId)
        val camps = uacCampaignService.getCampaignsByFeedIds(clientInfo.clientId.toString(), listOf(feedId))
        assertThat(camps.keys).containsOnly(feedId)
    }
}
