package ru.yandex.direct.jobs.internal.utils

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.anyCollection
import org.mockito.Mockito.anyList
import org.mockito.Mockito.anySet
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.jobs.internal.model.StructureOfBannerIds

class InfoForUrlNotificationsGetterTest {

    private val SHARD = 0

    @Mock
    private lateinit var campaignRepository: CampaignRepository

    @Mock
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Spy
    @InjectMocks
    private lateinit var infoForUrlNotificationsGetter: InfoForUrlNotificationsGetter

    private lateinit var openMocks: AutoCloseable

    @BeforeEach
    private fun beforeEach() {
        openMocks = MockitoAnnotations.openMocks(this)

        doAnswer { invocation ->
            val targetIds = invocation.getArgument<Set<Long>>(1)
            bannersTestData.filterKeys(targetIds::contains).values.toList()
        }.`when`(bannerTypedRepository).getInternalBannersByIds(eq(SHARD), anySet())

        doAnswer { invocation ->
            val targetIds = invocation.getArgument<List<Long>>(1)
            campaignsTestData.filterKeys(targetIds::contains)
        }.`when`(campaignRepository).getCampaignsSimple(eq(SHARD), anyCollection())
    }

    @AfterEach
    private fun afterEach() {
        openMocks.close()
    }

    @Test
    fun checkStructureBanners() {
        val expected = structuredData

        val actualStructure = InfoForUrlNotificationsGetter.structureBanners(
            bannersTestData.values.toList(),
            campaignsTestData.mapValues { it.value.name },
        )

        assertThat(actualStructure).containsExactlyElementsOf(expected)
    }

    @Test
    fun checkGetAdditionalInfoByBannerIds() {
        val bannerIds = setOf(1L, 2L, 3L, 4L)
        val expected = structuredData

        val actualBanners = infoForUrlNotificationsGetter.getAdditionalInfoByBannerIds(SHARD, bannerIds)

        verify(infoForUrlNotificationsGetter).getAdditionalInfoByInternalBanners(eq(SHARD), anyList())
        verify(infoForUrlNotificationsGetter).getCampaignNamesFromItsBanners(eq(SHARD), anyList())
        assertThat(actualBanners).containsExactlyElementsOf(expected)
    }

    private val campaignsTestData: Map<Long, CampaignSimple> = listOf(
        createCampaign(11L, "camp11"),
        createCampaign(22L, "camp22"),
        createCampaign(33L, "camp33"),
    ).associateBy(CampaignSimple::getId)

    private val bannersTestData: Map<Long, InternalBanner> = listOf(
        createBanner(1L, 11L, 1L),
        createBanner(2L, 11L, 1L),
        createBanner(3L, 11L, 2L),
        createBanner(4L, 22L, 3L),
    ).associateBy(InternalBanner::getId)

    private val structuredData: List<StructureOfBannerIds> = listOf(
        StructureOfBannerIds(
            11L, "camp11", mapOf(
                1L to listOf(1L, 2L),
                2L to listOf(3L),
            )
        ),
        StructureOfBannerIds(
            22L, "camp22", mapOf(
                3L to listOf(4L),
            )
        ),
    )

    private fun createCampaign(id: Long, name: String): CampaignSimple =
        Campaign().withId(id).withName(name)

    private fun createBanner(id: Long, campaignId: Long, teplateId: Long): InternalBanner =
        InternalBanner().withId(id).withCampaignId(campaignId).withTemplateId(teplateId)
}
