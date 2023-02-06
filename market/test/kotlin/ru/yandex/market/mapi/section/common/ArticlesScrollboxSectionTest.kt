package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveArticlesAndModelsResponse
import ru.yandex.market.mapi.core.util.mockRegion
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.article.ArticleSnippetAssembler
import ru.yandex.market.mapi.section.common.article.ArticlesScrollboxSection

/**
 * @author Madina Alieva / gahara@ / 28.01.2022
 */
class ArticlesScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = ArticleSnippetAssembler()
    private val resolver = ResolveArticlesAndModelsResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/article/fapiResponse.json"),
            expected = "/section/common/article/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(resolver to "/section/common/article/fapiResponse.json"),
            expected = "/section/common/article/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        mockRegion(42)

        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/article/fapiResponse.json"),
            expected = "/section/common/article/contentResult.json"
        )
    }

    private fun buildWidget(): ArticlesScrollboxSection {
        return ArticlesScrollboxSection().apply {
            addDefParams()
            minCountToShow = 2
        }
    }
}
