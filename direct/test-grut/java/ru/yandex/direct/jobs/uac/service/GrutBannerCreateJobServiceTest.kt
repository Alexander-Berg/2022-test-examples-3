package ru.yandex.direct.jobs.uac.service

import java.time.LocalDateTime
import com.google.common.collect.Lists.cartesianProduct
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.jobs.configuration.GrutJobsTest
import ru.yandex.direct.jobs.uac.service.ContentsByType.Companion.emptyContents
import ru.yandex.direct.core.entity.uac.GrutTestHelpers.buildCreateCampaignRequest
import ru.yandex.direct.test.utils.checkContainsInAnyOrder

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutBannerCreateJobServiceTest {
    @Autowired
    private lateinit var grutBannerCreateJobService: GrutBannerCreateJobService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private var clientId: Long = generateUniqueRandomId().toIdLong()
    private var campaignId: Long = generateUniqueRandomId().toIdLong()
    private var adGroupId: Long = generateUniqueRandomId().toIdLong()

    private var bannerId1: Long = generateUniqueRandomId().toIdLong()
    private var bannerId2: Long = generateUniqueRandomId().toIdLong()
    private var bannerId3: Long = generateUniqueRandomId().toIdLong()
    private var bannerId4: Long = generateUniqueRandomId().toIdLong()

    private val assetId1: String = generateUniqueRandomId()
    private val assetId2: String = generateUniqueRandomId()
    private val assetId3: String = generateUniqueRandomId()
    private val assetId4: String = generateUniqueRandomId()

    private val titleCampaignContents = listOf(assetId1, assetId2)
        .map {
            UacYdbCampaignContent(
                campaignId = campaignId.toString(),
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.TITLE,
                contentId = it
            )
        }
    private val textCampaignContents = listOf(assetId3, assetId4)
        .map {
            UacYdbCampaignContent(
                campaignId = campaignId.toString(),
                order = 0,
                status = CampaignContentStatus.CREATED,
                type = MediaType.TEXT,
                contentId = it
            )
        }
    private val contentsByType = cartesianProduct(titleCampaignContents, textCampaignContents).map { (titleCampaignContent, textCampaignContent) ->
        emptyContents().copy(
            titleCampaignContent = titleCampaignContent,
            textCampaignContent = textCampaignContent,
        )
    }

    @BeforeEach
    fun before() {
        clientId = generateUniqueRandomId().toIdLong()
        campaignId = generateUniqueRandomId().toIdLong()
        adGroupId = generateUniqueRandomId().toIdLong()
        bannerId1 = generateUniqueRandomId().toIdLong()
        bannerId2 = generateUniqueRandomId().toIdLong()
        bannerId3 = generateUniqueRandomId().toIdLong()
        bannerId4 = generateUniqueRandomId().toIdLong()

        grutApiService.clientGrutDao.createOrUpdateClient(ClientGrutModel(
            client = Client().withId(clientId).withCreateDate(LocalDateTime.now()),
            ndsHistory = listOf()))
        grutApiService.briefGrutApi.createBrief(buildCreateCampaignRequest(clientId, campaignId))
    }

    @Test
    fun saveBannersAndAdGroupsBulkSuccess() {
        val banners = listOf(bannerId1, bannerId2, bannerId3, bannerId4).map { bannerId ->
            TextBanner().withId(bannerId).withAdGroupId(adGroupId)
        }
        val returnBanners = mutableListOf<BannerWithSystemFields>()
        val returnAdGroups = mutableSetOf<Long>()
        grutBannerCreateJobService.saveBannersAndAdGroupAndUpdateLists(
            contentsByType,
            banners,
            campaignId.toIdString(),
            returnBanners,
            returnAdGroups,
            true,
        )

        val actualAdGroupIds = grutApiService.briefAdGroupGrutApi.selectAdGroups(
            filter = "[/meta/campaign_id] = $campaignId"
        )
            .map { it.meta.id }
            .toSet()

        val actualBanners = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/ad_group_id] = $adGroupId"
        )

        val actualBannersIds = actualBanners
            .map { it.meta.id }
            .toSet()

        val bannerIdToAssetIds = actualBanners.associate { it.meta.id to it.spec.assetIdsList }
        val bannerIdToAssetLinkIds = actualBanners.associate { it.meta.id to it.spec.assetLinkIdsList }
        val assetIdToAssetLinkId = (titleCampaignContents + textCampaignContents).associate { it.contentId!! to it.id }
        val soft = SoftAssertions()

        soft.assertThat(actualAdGroupIds).isEqualTo(setOf(adGroupId))
        soft.assertThat(actualBannersIds).isEqualTo(setOf(bannerId1, bannerId2, bannerId3, bannerId4))
        actualBannersIds.forEach { bid ->
            val expectedAssetLinkIds = bannerIdToAssetIds[bid]!!
                .map { assetIdToAssetLinkId[it.toIdString()]!! }
            val actualAssetLinkIds = bannerIdToAssetLinkIds[bid]!!.map { it.toIdString() }

            actualAssetLinkIds.checkContainsInAnyOrder(*expectedAssetLinkIds.toTypedArray())
        }
        soft.assertThat(returnBanners).containsExactlyInAnyOrder(*returnBanners.toTypedArray())
        soft.assertThat(returnAdGroups).containsExactly(adGroupId)

        soft.assertAll()
    }

    @Test
    fun saveBannersAndAdGroupsBulk_NoAdGroupId() {
        val banners = listOf(bannerId1, bannerId2, bannerId3, bannerId4).map { bannerId ->
            TextBanner().withId(bannerId)
        }
        val returnBanners = mutableListOf<BannerWithSystemFields>()
        val returnAdGroups = mutableSetOf<Long>()
        grutBannerCreateJobService.saveBannersAndAdGroupAndUpdateLists(
            contentsByType,
            banners,
            campaignId.toIdString(),
            returnBanners,
            returnAdGroups,
            true,
        )

        val actualAdGroupIds = grutApiService.briefAdGroupGrutApi.selectAdGroups(
            filter = "[/meta/campaign_id] = $campaignId"
        )
            .map { it.meta.id }
            .toSet()

        val actualBannerIds = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/ad_group_id] = $adGroupId"
        ).map { it.meta.id }
            .toSet()

        val soft = SoftAssertions()
        soft.assertThat(actualAdGroupIds).isEmpty()
        soft.assertThat(actualBannerIds).isEmpty()
        soft.assertThat(returnBanners).isEmpty()
        soft.assertThat(returnAdGroups).isEmpty()
        soft.assertAll()
    }

    @Test
    fun saveBannersAndAdGroupsBulk_NoBannerId() {
        val banners = listOf(bannerId1, bannerId2, bannerId3, bannerId4).map { bannerId ->
            TextBanner()
                .withId(if (bannerId == bannerId1 || bannerId == bannerId2) bannerId else null)
                .withAdGroupId(if (bannerId == bannerId1 || bannerId == bannerId2) adGroupId else null)
        }
        val returnBanners = mutableListOf<BannerWithSystemFields>()
        val returnAdGroups = mutableSetOf<Long>()
        grutBannerCreateJobService.saveBannersAndAdGroupAndUpdateLists(
            contentsByType,
            banners,
            campaignId.toIdString(),
            returnBanners,
            returnAdGroups,
            true,
        )

        val actualAdGroupIds = grutApiService.briefAdGroupGrutApi.selectAdGroups(
            filter = "[/meta/campaign_id] = $campaignId"
        )
            .map { it.meta.id }
            .toSet()

        val actualBannerIds = grutApiService.briefBannerGrutApi.selectBanners(
            filter = "[/meta/ad_group_id] = $adGroupId"
        )
            .map { it.meta.id }
            .toSet()

        val soft = SoftAssertions()
        soft.assertThat(actualAdGroupIds).containsExactly(adGroupId)
        soft.assertThat(actualBannerIds).containsExactlyInAnyOrder(bannerId1, bannerId2)
        soft.assertThat(returnBanners).containsExactlyInAnyOrder(*(banners.filter { it.id != null }).toTypedArray())
        soft.assertThat(returnAdGroups).containsExactly(adGroupId)
        soft.assertAll()
    }
}
