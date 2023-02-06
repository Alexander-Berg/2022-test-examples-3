package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveStoriesResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.stories.StoriesScrollboxSection
import ru.yandex.market.mapi.section.common.stories.StoriesSnippetAssembler

/**
 * @author Ilya Kislitsyn / ilyakis@ / 16.03.2022
 */
class StoriesScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = StoriesSnippetAssembler()
    private val resolver = ResolveStoriesResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/stories/fapiResponse.json"),
            expected = "/section/common/stories/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/stories/fapiResponse.json"),
            expected = "/section/common/stories/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/stories/fapiResponse.json"),
            expected = "/section/common/stories/contentResult.json"
        )
    }

    private fun buildWidget(): StoriesScrollboxSection {
        return StoriesScrollboxSection().apply {
            addDefParams()
            minCountToShow = 2
        }
    }

    private fun buildResolver() = buildResolver(
        "resolveStories", mapOf(
            "isLive" to true
        )
    )
}