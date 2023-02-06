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
import ru.yandex.adv.direct.banner.resources.LeadButton
import ru.yandex.direct.core.entity.banner.model.BannerWithLeadButtonForBsExport
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat

class BannerLeadButtonLoaderTest {
    private val shard = 1

    private lateinit var bannerTypedRepository: BannerTypedRepository
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator
    private lateinit var bannerLeadButtonLoader: BannerLeadButtonLoader

    @BeforeEach
    fun before() {
        bannerTypedRepository = mock()
        bsOrderIdCalculator = mock()
        bannerLeadButtonLoader = BannerLeadButtonLoader(
            BannerResourcesLoaderContext(bannerTypedRepository, BannerResourcesHelper(bsOrderIdCalculator))
        )

        doReturn(mapOf(3L to 5L))
            .`when`(bsOrderIdCalculator)
            .calculateOrderIdIfNotExist(any(), any())
    }

    @Test
    fun regularUpdate() {
        val banner = createBanner()
            .withLeadformHref("http://yandex.ru")
            .withLeadformButtonText("Text")

        var leadButton = LeadButton.newBuilder()
            .setHref("http://yandex.ru")
            .setText("Text")
            .build();

        testLoadResources(
            banner,
            isDeleted = false,
            result = leadButton,
            sent = 1
        )
    }

    @Test
    fun nullFieldsBanner() {
        val banner = createBanner()
            .withLeadformHref(null)
            .withLeadformButtonText(null)

        testLoadResources(
            banner,
            isDeleted = false,
            result = null,
            sent = 1
        )
    }

    @Test
    fun deleted() {
        val banner = createBanner()

        testLoadResources(
            banner = banner,
            isDeleted = true,
            result = null,
            sent = 1
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

    private fun testLoadResources(
        banner: TextBanner?,
        isDeleted: Boolean,
        result: LeadButton?,
        sent: Int
    ) {
        val obj = BsExportBannerResourcesObject.Builder()
            .setBid(1L)
            .setResourceType(BannerResourceType.BANNER_LEAD_BUTTON)
            .setDeleted(isDeleted)
            .build()

        whenever(
            bannerTypedRepository.getSafely(
                eq(shard),
                argWhere<Collection<Long>> { 1L in it },
                eq(BannerWithLeadButtonForBsExport::class.java)
            ),
        ) doReturn if (banner != null) listOf(banner) else listOf()

        val actualResult = bannerLeadButtonLoader.loadResources(shard, listOf(obj))

        val expectedStat = BannerResourcesStat()
            .setSent(sent)
            .setCandidates(1)

        val expectedResource = createResource()
            .apply { if (result != null) setResource(result) }
            .build()

        assertThat(actualResult.stat).isEqualToComparingFieldByField(expectedStat)
        if (sent == 1) {
            assertThat(actualResult.resources).usingFieldByFieldElementComparator().containsExactly(expectedResource)
        } else {
            assertThat(actualResult.resources).isEmpty()
        }
    }

    private fun createResource() = BannerResource.Builder<LeadButton>()
        .setBid(1L)
        .setPid(2L)
        .setCid(3L)
        .setBsBannerId(4L)
        .setOrderId(5L)

    private fun createBanner() = TextBanner()
        .withId(1L)
        .withAdGroupId(2L)
        .withCampaignId(3L)
        .withBsBannerId(4L)
}
