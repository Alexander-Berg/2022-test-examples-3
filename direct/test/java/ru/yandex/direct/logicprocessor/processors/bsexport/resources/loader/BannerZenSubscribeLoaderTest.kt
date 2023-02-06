package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.MetrikaSnippet
import ru.yandex.direct.core.entity.banner.model.BannerWithZenPublisherIdForBsExport
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.ess.common.utils.TablesEnum
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.ZenSubscribeInfo

const val COUNTER = 123L
const val GOAL = "yazen-subscribe"
const val PUBLISHER_ITEM_ID = "123abc"

class BannerZenSubscribeLoaderTest {

    private val shard = 1

    private lateinit var bannerTypedRepository: BannerTypedRepository
    private lateinit var bannerRelationsRepository: BannerRelationsRepository
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator
    private lateinit var bannerZenSubscribeLoader: BannerZenSubscribeLoader

    @BeforeEach
    fun before() {
        bannerTypedRepository = mock()
        bsOrderIdCalculator = mock()
        bannerRelationsRepository = mock()
        bannerZenSubscribeLoader = BannerZenSubscribeLoader(
            BannerResourcesLoaderContext(bannerTypedRepository, BannerResourcesHelper(bsOrderIdCalculator)),
            bannerRelationsRepository
        )

        doReturn(mapOf(3L to 5L))
            .`when`(bsOrderIdCalculator)
            .calculateOrderIdIfNotExist(any(), any())
    }

    @Test
    fun regularUpdate() {
        val banner = createBanner()
            .withZenPublisherId(PUBLISHER_ITEM_ID)

        val snippet = MetrikaSnippet.newBuilder()
            .setCounter(COUNTER)
            .setGoal(GOAL)
            .build()

        val zenSubscribeInfo = ZenSubscribeInfo(PUBLISHER_ITEM_ID, snippet)

        testLoadResources(
            banner,
            isDeleted = false,
            result = zenSubscribeInfo,
            sent = 1
        )
    }

    @Test
    fun campaignWithoutMetrikaCounters() {
        val banner = createBanner()
            .withZenPublisherId(PUBLISHER_ITEM_ID)

        val snippet = MetrikaSnippet.newBuilder()
            .setCounter(0L)
            .setGoal(GOAL)
            .build()

        val zenSubscribeInfo = ZenSubscribeInfo(PUBLISHER_ITEM_ID, snippet)

        testLoadResourcesWithoutMetrikaCounters(
            banner,
            isDeleted = false,
            result = zenSubscribeInfo,
            sent = 1
        )
    }

    @Test
    fun deleted() {
        testLoadResources(
            banner = null,
            isDeleted = true,
            result = null,
            sent = 0,
        )
    }

    @Test
    fun notDeletedButAbsent() {
        testLoadResources(
            banner = null,
            isDeleted = false,
            result = null,
            sent = 0,
        )
    }

    @Test
    fun testWithOnlyAdditionalData() {
        val obj1 = BsExportBannerResourcesObject.Builder()
            .setAdditionalTable(TablesEnum.CAMP_METRIKA_COUNTERS)
            .setAdditionalId(1L)
            .build()

        val obj2 = BsExportBannerResourcesObject.Builder()
            .setAdditionalTable(TablesEnum.CAMP_METRIKA_COUNTERS)
            .setAdditionalId(2L)
            .build()

        whenever(
            bannerRelationsRepository.getBannerIdsByCampaignIds(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
            ),
        ) doReturn listOf(1L, 2L, 3L)

        whenever(
            bannerRelationsRepository.filterBannersWithPublisherId(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
            ),
        ) doReturn listOf(3L)

        whenever(
            bannerTypedRepository.getSafely(
                eq(shard),
                eq(listOf(3L)),
                eq(BannerWithZenPublisherIdForBsExport::class.java)
            ),
        ) doReturn listOf(TextBanner()
            .withId(3L)
            .withCampaignId(2L)
            .withAdGroupId(2L)
            .withBsBannerId(10L)
            .withZenPublisherId("123"))

        doReturn(mapOf(2L to 3L))
            .`when`(bsOrderIdCalculator)
            .calculateOrderIdIfNotExist(any(), any())

        val actualResult = bannerZenSubscribeLoader.loadResources(shard, listOf(obj1, obj2))

        // Отправлена информация всего по одному баннеру, т.к. только он содержал zenPublisherId
        assertThat(actualResult.stat.sent).isEqualTo(1)

        // Кандидата два т.к. было два ресурса по метрике
        assertThat(actualResult.stat.candidates).isEqualTo(2)
        assertThat(actualResult.resources.size).isEqualTo(1)
        assertThat(actualResult.resources[0].bid).isEqualTo(3L)
        assertThat(actualResult.resources[0].resource?.publisherItemId).isEqualTo("123")
    }

    private fun testLoadResources(
        banner: TextBanner?,
        isDeleted: Boolean,
        result: ZenSubscribeInfo?,
        sent: Int
    ) {
        val obj = BsExportBannerResourcesObject.Builder()
            .setBid(1L)
            .setResourceType(BannerResourceType.BANNER_ZEN_SUBSCRIBE)
            .setDeleted(isDeleted)
            .build()

        whenever(
            bannerTypedRepository.getSafely(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
                eq(BannerWithZenPublisherIdForBsExport::class.java)
            ),
        ) doReturn if (banner != null) listOf(banner) else listOf()

        whenever(
            bannerRelationsRepository.getMetrikaCountersByBids(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
            ),
        ) doReturn if (banner != null) mapOf(1L to listOf(COUNTER)) else mapOf()

        val actualResult = bannerZenSubscribeLoader.loadResources(shard, listOf(obj))

        val expectedStat = BannerResourcesStat()
            .setSent(sent)
            .setCandidates(1)

        val expectedResource = createResource()
            .apply { if (result != null) setResource(result) }
            .build()

        assertThat(actualResult.stat)
            .usingRecursiveComparison()
            .isEqualTo(expectedStat)
        if (sent == 1) {
            assertThat(actualResult.resources)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(listOf(expectedResource))
        } else {
            assertThat(actualResult.resources).isEmpty()
        }
    }

    private fun testLoadResourcesWithoutMetrikaCounters(
        banner: TextBanner?,
        isDeleted: Boolean,
        result: ZenSubscribeInfo?,
        sent: Int
    ) {
        val obj = BsExportBannerResourcesObject.Builder()
            .setBid(1L)
            .setResourceType(BannerResourceType.BANNER_ZEN_SUBSCRIBE)
            .setDeleted(isDeleted)
            .build()

        whenever(
            bannerTypedRepository.getSafely(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
                eq(BannerWithZenPublisherIdForBsExport::class.java)
            ),
        ) doReturn if (banner != null) listOf(banner) else listOf()

        val actualResult = bannerZenSubscribeLoader.loadResources(shard, listOf(obj))

        val expectedStat = BannerResourcesStat()
            .setSent(sent)
            .setCandidates(1)

        val expectedResource = createResource()
            .apply { if (result != null) setResource(result) }
            .build()

        assertThat(actualResult.stat)
            .usingRecursiveComparison()
            .isEqualTo(expectedStat)
        if (sent == 1) {
            assertThat(actualResult.resources)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(listOf(expectedResource))
        } else {
            assertThat(actualResult.resources).isEmpty()
        }
    }

    private fun createBanner() = TextBanner()
        .withId(1L)
        .withAdGroupId(2L)
        .withCampaignId(3L)
        .withBsBannerId(4L)

    private fun createResource() = BannerResource.Builder<ZenSubscribeInfo>()
        .setBid(1L)
        .setPid(2L)
        .setCid(3L)
        .setBsBannerId(4L)
        .setOrderId(5L)
}
