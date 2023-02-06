package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.yandex.direct.core.entity.banner.model.BannerWithGreenUrlTextsForBsExport
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.GreenUrlTexts

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerGreenUrlLoaderTest {

    private val shard = 1

    private lateinit var bannerTypedRepository: BannerTypedRepository
    private lateinit var bsOrderIdCalculator: BsOrderIdCalculator
    private lateinit var bannerGreenUrlTextsLoader: BannerGreenUrlTextsLoader

    @BeforeAll
    fun before() {
        bannerTypedRepository = mock()
        bsOrderIdCalculator = mock()
        bannerGreenUrlTextsLoader = BannerGreenUrlTextsLoader(
            BannerResourcesLoaderContext(bannerTypedRepository, BannerResourcesHelper(bsOrderIdCalculator))
        )

        doReturn(mapOf(3L to 5L))
            .`when`(bsOrderIdCalculator)
            .calculateOrderIdIfNotExist(any(), any())
    }

    @Test
    fun regularUpdate() {
        val banner = createBanner()
            .withDisplayHrefPrefix("Prefix")
            .withDisplayHrefSuffix("Suffix")

        testLoadResources(
            banner,
            isDeleted = false,
            expectedResource = createResource()
                .setResource(GreenUrlTexts("Prefix", "Suffix"))
                .build(),
            sent = 1
        )
    }

    @Test
    fun onlyPrefix() {
        val banner = createBanner()
            .withDisplayHrefPrefix("Prefix")
            .withDisplayHrefSuffix(null)

        testLoadResources(
            banner,
            isDeleted = false,
            expectedResource = createResource()
                .setResource(GreenUrlTexts("Prefix", null))
                .build(),
            sent = 1
        )
    }

    @Test
    fun bothEmpty() {
        val banner = createBanner()
            .withDisplayHrefPrefix(null)
            .withDisplayHrefSuffix(null)

        testLoadResources(
            banner,
            isDeleted = false,
            expectedResource = createResource()
                .setResource(GreenUrlTexts(null, null))
                .build(),
            sent = 1,
        )
    }

    @Test
    fun deleted() {
        testLoadResources(
            banner = null,
            isDeleted = true,
            expectedResource = null,
            sent = 0,
        )
    }

    @Test
    fun notDeletedButAbsent() {
        testLoadResources(
            banner = null,
            isDeleted = false,
            expectedResource = null,
            sent = 0,
        )
    }

    private fun testLoadResources(
        banner: TextBanner?,
        isDeleted: Boolean,
        expectedResource: BannerResource<GreenUrlTexts>?,
        sent: Int
    ) {
        val obj = BsExportBannerResourcesObject.Builder()
            .setBid(1L)
            .setResourceType(BannerResourceType.BANNER_GREEN_URL_TEXTS)
            .setDeleted(isDeleted)
            .build()

        doReturn(if (banner != null) listOf(banner) else listOf())
            .`when`(bannerTypedRepository)
            .getSafely(eq(shard), any<Collection<Long>>(), any<Class<BannerWithGreenUrlTextsForBsExport>>())

        val actualResult = bannerGreenUrlTextsLoader.loadResources(shard, listOf(obj))

        val expectedStat = BannerResourcesStat()
            .setSent(sent)
            .setCandidates(1)

        val expectedResources = if (expectedResource != null) listOf(expectedResource) else listOf()

        assertThat(actualResult.stat).isEqualToComparingFieldByField(expectedStat)
        assertThat(actualResult.resources).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedResources)
    }

    private fun createResource() = BannerResource.Builder<GreenUrlTexts>()
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
