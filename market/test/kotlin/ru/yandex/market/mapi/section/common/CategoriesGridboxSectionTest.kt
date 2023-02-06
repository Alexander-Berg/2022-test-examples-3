package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveDJUniversalLinksResponse
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.mapi.core.util.mockRegion
import ru.yandex.market.mapi.model.section.ShowMoreLink
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.category.CategoriesGridboxSection
import ru.yandex.market.mapi.section.common.category.CategorySnippetAssembler

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.01.2022
 */
class CategoriesGridboxSectionTest : AbstractSectionTest() {
    private val assembler = CategorySnippetAssembler()
    private val resolver = ResolveDJUniversalLinksResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/category/fapiResponse.json"),
            expected = "/section/common/category/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/category/fapiResponse.json"),
            expected = "/section/common/category/sectionResult.json"
        )
    }

    @Test
    fun testSectionWithLogin() {
        mockRegion(42)
        mockOauth("test")

        testSectionResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/category/fapiResponse.json"),
            expected = "/section/common/category/sectionResultLogin.json"
        )
    }

    @Test
    fun testSectionShowMore() {
        testSectionResult(
            buildWidget().apply {
                showMore = ShowMoreLink()
                showMore?.url = "default url"
                showMore?.text = "Custom text"
            },
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/category/fapiResponse.json"),
            expected = "/section/common/category/sectionResultShowMore.json"
        )
    }

    @Test
    fun testContentResult() {
        mockRegion(42)

        testContentResult(
            buildWidget(),
            assembler,
            buildDjResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/category/fapiResponse.json"),
            expected = "/section/common/category/contentResult.json"
        )
    }

    @Test
    fun testStaticSnippetInteractions() {
        testStaticContentResult(
            buildWidget(),
            assembler,
            staticContentFile = "/section/common/category/staticContent.json",
            expected = "/section/common/category/staticContentResult.json"
        )
    }

    private fun buildWidget(): CategoriesGridboxSection {
        return CategoriesGridboxSection().apply {
            addDefParams()
            minCountToShow = 2
            columnsCount = 3
            showLargeImages = false
            showMore = null
        }
    }

    private fun buildDjResolver() = buildResolver(
        "resolveDJUniversalLinks", mapOf(
            "djPlace" to "somePlace",
            "page" to "11111",
            "other" to "invalid",
        )
    )
}