package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateAdGroupRequest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateBannerRequest
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateCampaignRequest
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus.BSS_CREATED
import ru.yandex.grut.objects.proto.Banner.TBannerSpec.EBannerStatus.BSS_DELETED

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUacAdGroupJobServiceTest {
    companion object {
        private const val NOT_EXISTING_CAMPAIGN_ID: Long = 123456
        private const val DIRECT_CAMPAIGN_ID: Long = 123456
    }

    @Autowired
    private lateinit var grutUacAdGroupJobService: GrutUacAdGroupJobService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private val clientId: Long = generateUniqueRandomId().toIdLong()
    private val campaignId: Long = generateUniqueRandomId().toIdLong()

    private val adGroupId1: Long = generateUniqueRandomId().toIdLong()
    private val adGroupId2: Long = generateUniqueRandomId().toIdLong()
    private val adGroupId3: Long = generateUniqueRandomId().toIdLong()
    private val adGroupId4: Long = generateUniqueRandomId().toIdLong()

    private val bannerId1: Long = generateUniqueRandomId().toIdLong()
    private val bannerId2: Long = generateUniqueRandomId().toIdLong()
    private val bannerId3: Long = generateUniqueRandomId().toIdLong()
    private val deletedBannerId1: Long = generateUniqueRandomId().toIdLong()
    private val deletedBannerId2: Long = generateUniqueRandomId().toIdLong()

    private val bannerIdToAdGroupId: Map<Long, Long> = mapOf(
        bannerId1 to adGroupId1,
        bannerId2 to adGroupId1,
        deletedBannerId1 to adGroupId1,
        bannerId3 to adGroupId2,
        deletedBannerId2 to adGroupId4,
    )

    @BeforeAll
    fun before() {
        grutApiService.clientGrutDao.createOrUpdateClient(ClientGrutModel(
            client = Client().withId(clientId).withCreateDate(LocalDateTime.now()),
            ndsHistory = listOf()))
        grutApiService.briefGrutApi.createBrief(buildCreateCampaignRequest(clientId, campaignId))

        listOf(adGroupId1, adGroupId2, adGroupId3, adGroupId4)
            .forEach {
                grutApiService.briefAdGroupGrutApi.createOrUpdateBriefAdGroup(buildCreateAdGroupRequest(campaignId, it))
            }

        listOf(bannerId1, bannerId2, bannerId3, deletedBannerId1, deletedBannerId2)
            .forEach {
                grutApiService.briefBannerGrutApi.createObject(buildCreateBannerRequest(
                    bannerIdToAdGroupId[it]!!,
                    campaignId,
                    it,
                    status = if (setOf(deletedBannerId1, deletedBannerId2).contains(it)) BSS_DELETED else BSS_CREATED))
            }
    }

    @Test
    fun getAdGroupsByExistingCampaign() {
        val actualAdGroups = grutUacAdGroupJobService.getAdGroupsByUacCampaignId(campaignId.toIdString(), DIRECT_CAMPAIGN_ID)

        val soft = SoftAssertions()

        soft.assertThat(actualAdGroups.map { it.id.toIdLong() }.toSet())
            .isEqualTo(setOf(adGroupId1, adGroupId2, adGroupId3, adGroupId4))

        soft.assertThat(actualAdGroups.map { it.directAdGroupId }.toSet())
            .isEqualTo(setOf(adGroupId1, adGroupId2, adGroupId3, adGroupId4))

        actualAdGroups.forEach {
            soft.assertThat(it.directCampaignId).isEqualTo(DIRECT_CAMPAIGN_ID.toString())
        }

        soft.assertAll()
    }

    @Test
    fun getAdGroupsByNotExistingCampaign() {
        val actualAdGroups = grutUacAdGroupJobService.getAdGroupsByUacCampaignId(NOT_EXISTING_CAMPAIGN_ID.toIdString(), DIRECT_CAMPAIGN_ID)

        val soft = SoftAssertions()

        soft.assertThat(actualAdGroups).isEmpty()

        soft.assertAll()
    }

    @Test
    fun getAdGroupsIdWithBannersCnt() {
        val adGroupAndAdsCount = grutUacAdGroupJobService.getAdGroupsIdWithBannersCnt(
            listOf(adGroupId1.toString(), adGroupId2.toString(), adGroupId3.toString(), adGroupId4.toString()))

        val expectAdGroupIdToBannersCnt = setOf(
            AdGroupIdToBannersCnt(adGroupId1, 2),
            AdGroupIdToBannersCnt(adGroupId2, 1),
            AdGroupIdToBannersCnt(adGroupId3, 0),
            AdGroupIdToBannersCnt(adGroupId4, 0),
        )

        assertThat(adGroupAndAdsCount)
            .hasSize(expectAdGroupIdToBannersCnt.size)
            .containsAll(expectAdGroupIdToBannersCnt)
    }
}
