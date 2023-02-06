package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolvePopularBrandsResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.brand.BrandSnippetAssembler
import ru.yandex.market.mapi.section.common.brand.BrandsScrollboxSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.03.2022
 */
class BrandsScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = BrandSnippetAssembler()
    private val resolver = ResolvePopularBrandsResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/brand/fapiResponse.json"),
            expected = "/section/common/brand/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/brand/fapiResponse.json"),
            expected = "/section/common/brand/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/brand/fapiResponse.json"),
            expected = "/section/common/brand/contentResult.json"
        )
    }

    private fun buildWidget(): BrandsScrollboxSection {
        return BrandsScrollboxSection().apply {
            addDefParams()
            minCountToShow = 0
        }
    }
}