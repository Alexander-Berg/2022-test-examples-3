package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveLiveStreamEntrypointsByStatusesResponse
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.livestream.LiveStreamSnippetAssembler
import ru.yandex.market.mapi.section.common.livestream.VideosScrollboxSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.03.2022
 */
class VideosScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = LiveStreamSnippetAssembler()
    private val resolver = ResolveLiveStreamEntrypointsByStatusesResponse.RESOLVER

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/livestream/fapiResponse.json"),
            expected = "/section/common/livestream/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/livestream/fapiResponse.json"),
            expected = "/section/common/livestream/sectionResult.json"
        )
    }

    @Test
    fun testSectionLive() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/livestream/fapiResponse.json"),
            expected = "/section/common/livestream/sectionResultLive.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/livestream/fapiResponse.json"),
            expected = "/section/common/livestream/contentResult.json"
        )
    }

    private fun buildWidget(): VideosScrollboxSection {
        return VideosScrollboxSection().apply {
            addDefParams()
            minCountToShow = 2

        }
    }

    private fun buildResolver(): ResourceResolver {
        return buildResolver(
            "someResolver", mapOf(
                "key" to "value",
                "param" to "target",
                "isLive" to true,
            )
        )
    }
}