package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.createTextCampaignContent
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed
import ru.yandex.direct.jobs.configuration.JobsTest

@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YdbUacBannerJobServiceTest {
    @Autowired
    private lateinit var ydbUacBannerJobService: YdbUacBannerJobService

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var typedCampaignStepsUnstubbed: TypedCampaignStepsUnstubbed

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private var campaignId = 0L
    private lateinit var adGroupInfo: AdGroupInfo
    private var bannerId = 0L

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!

        val campaignInfo = typedCampaignStepsUnstubbed.createDefaultMobileContentCampaign(userInfo, clientInfo)
        campaignId = campaignInfo.id

        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo.toCampaignInfo())

        val banner = TestNewMobileAppBanners
            .fullMobileAppBanner(campaignInfo.id, adGroupInfo.adGroupId)
            .withBsBannerId(0L)

        bannerId = steps.mobileAppBannerSteps().createMobileAppBanner(
            NewMobileAppBannerInfo()
            .withClientInfo(clientInfo)
            .withAdGroupInfo(adGroupInfo)
            .withBanner(banner)).bannerId
    }

    @Test
    fun getNotDeletedDirectAdsByContentIdsWithArchivedAdSuccess() {
        val bannerForArchive = TestNewMobileAppBanners
            .fullMobileAppBanner(campaignId, adGroupInfo.adGroupId)

        val bannerIdForArchive = steps.mobileAppBannerSteps().createMobileAppBanner(NewMobileAppBannerInfo()
            .withClientInfo(clientInfo)
            .withAdGroupInfo(adGroupInfo)
            .withBanner(bannerForArchive)).bannerId

        val uacCampaign = createYdbCampaign()
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        val uacCampaignContentImage1 = createImageCampaignContent(campaignId = uacCampaign.id)
        val uacCampaignContentImage2 = createImageCampaignContent(campaignId = uacCampaign.id)
        val uacCampaignContentText = createTextCampaignContent()
        val uacCampaignContentTitle = createTextCampaignContent(mediaType = MediaType.TITLE)

        uacYdbCampaignContentRepository.addCampaignContents(listOf(
            uacCampaignContentImage1, uacCampaignContentImage2, uacCampaignContentText, uacCampaignContentTitle))
        val uacDirectAdGroup = createDirectAdGroup(
            directCampaignId = uacCampaign.id,
            directAdGroupId = adGroupInfo.adGroupId
        )
        uacYdbDirectAdGroupRepository.saveDirectAdGroup(uacDirectAdGroup)
        val uacDirectAds = listOf(
            createDirectAd(
                directImageContentId = uacCampaignContentImage1.id,
                directAdId = bannerId,
                directAdGroupId = uacDirectAdGroup.id,
            ),
            createDirectAd(
                directImageContentId = uacCampaignContentImage2.id,
                directAdId = bannerIdForArchive,
                directAdGroupId = uacDirectAdGroup.id,
                status = DirectAdStatus.DELETED,
            )
        )

        uacDirectAds.forEach { uacDirectAd -> uacYdbDirectAdRepository.saveDirectAd(uacDirectAd) }
        val actualBanners = ydbUacBannerJobService.getNotDeletedDirectAdsByContentIds(
            setOf(), setOf(), setOf(uacCampaignContentImage1.id, uacCampaignContentImage2.id), setOf(), uacCampaign.id
        )
        val soft = SoftAssertions()
        soft.assertThat(actualBanners).size().isEqualTo(1)
        soft.assertThat(actualBanners.map { it.bid }.toSet()).isEqualTo(setOf(bannerId))
    }
}
