package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveFenekBannersResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.banner.BannerSection
import ru.yandex.market.mapi.section.common.banner.BannerSnippetAssembler
import ru.yandex.market.mapi.section.common.banner.BannersCarouselSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.03.2022
 */
class BannerSectionTest : AbstractSectionTest() {
    private val assembler = BannerSnippetAssembler()
    private val resolver = ResolveFenekBannersResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/bannersCarousel/fapiResponse.json"),
            expected = "/section/common/banner/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/banner/fapiResponse.json"),
            expected = "/section/common/banner/sectionResult.json"
        )
    }

    @Test
    fun testCarouselWidgetInteractions() {
        testSectionResult(
            buildCarouselWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/banner/fapiResponse.json"),
            expected = "/section/common/banner/sectionCarouselResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/bannersCarousel/fapiResponse.json"),
            expected = "/section/common/banner/contentResult.json"
        )
    }

    private fun buildWidget(): BannerSection {
        return BannerSection().apply {
            addDefParams()
        }
    }

    private fun buildCarouselWidget(): BannersCarouselSection {
        return BannersCarouselSection().apply {
            addDefParams()
            looped = true
            autoplay = false
        }
    }
}