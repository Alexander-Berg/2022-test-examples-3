package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.jobs.uac.model.UacBanner
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateAdGroupRequest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateBannerRequest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateCampaignRequest
import ru.yandex.grut.objects.proto.Banner

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUacBannerJobServiceTest {
    @Autowired
    private lateinit var grutUacBannerJobService: GrutUacBannerJobService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private val clientId: Long = generateUniqueRandomId().toIdLong()
    private val campaignId: Long = generateUniqueRandomId().toIdLong()
    private val campaignId2: Long = generateUniqueRandomId().toIdLong()

    private val adGroupId: Long = generateUniqueRandomId().toIdLong()
    private val adGroupId2: Long = generateUniqueRandomId().toIdLong()

    private val bannerId1: Long = generateUniqueRandomId().toIdLong()
    private val bannerId2: Long = generateUniqueRandomId().toIdLong()
    private val bannerId3: Long = generateUniqueRandomId().toIdLong()
    private val bannerId4: Long = generateUniqueRandomId().toIdLong()
    private val bannerId5: Long = generateUniqueRandomId().toIdLong()
    private val bannerId6: Long = generateUniqueRandomId().toIdLong()
    private val bannerId7: Long = generateUniqueRandomId().toIdLong()

    private val assetId1: String = generateUniqueRandomId()
    private val assetId2: String = generateUniqueRandomId()
    private val assetId3: String = generateUniqueRandomId()
    private val assetId4: String = generateUniqueRandomId()
    private val assetId5: String = generateUniqueRandomId()
    private val assetId6: String = generateUniqueRandomId()

    @BeforeAll
    fun before() {
        grutApiService.clientGrutDao.createOrUpdateClient(
                ClientGrutModel(
                    client = Client().withId(clientId).withCreateDate(LocalDateTime.now()),
                    ndsHistory = listOf()))
        grutApiService.briefGrutApi.createBrief(buildCreateCampaignRequest(clientId, campaignId))
        grutApiService.briefGrutApi.createBrief(buildCreateCampaignRequest(clientId, campaignId2))

        grutApiService.briefAdGroupGrutApi.createOrUpdateBriefAdGroup(buildCreateAdGroupRequest(campaignId, adGroupId))
        grutApiService.briefAdGroupGrutApi.createOrUpdateBriefAdGroup(buildCreateAdGroupRequest(campaignId, adGroupId2))

        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId1,
            assetIds = listOf(assetId1, assetId2)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId2,
            assetIds = listOf(assetId3, assetId4)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId7,
            assetIds = listOf(assetId1, assetId2, assetId3, assetId4)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId3,
            status = Banner.TBannerSpec.EBannerStatus.BSS_DELETED, assetIds = listOf(assetId1, assetId5)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId4,
            assetIds = listOf(assetId6)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId, campaignId, bannerId5,
            assetIds = listOf(assetId6)))
        grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(adGroupId2, campaignId2, bannerId6,
            assetIds = listOf(assetId1, assetId2, assetId3, assetId4)))
    }

    @Test
    fun updateStatusByDirectAdIdsSuccess() {
        val directIdToUacBanner: Map<Long, UacBanner> = mapOf(
            bannerId4 to UacBanner(uacBannerId = bannerId4.toIdString(), bid = bannerId4, assetLinkIds = setOf(assetId6)),
            bannerId5 to UacBanner(uacBannerId = bannerId5.toIdString(), bid = bannerId5, assetLinkIds = setOf(assetId6))
        )
        grutUacBannerJobService.updateStatusByDirectAdIds(
            directIdToUacBanner.keys,
            DirectAdStatus.MODERATING
        )

        val actualBanners = grutApiService.briefBannerGrutApi.getBanners(directIdToUacBanner.keys.toList())

        val soft = SoftAssertions()

        soft.assertThat(actualBanners).size().isEqualTo(2)
        actualBanners.forEach {
            soft.assertThat(it.spec.status).isEqualTo(Banner.TBannerSpec.EBannerStatus.BSS_MODERATING)
        }

        soft.assertAll()
    }
}
