package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh

class SectionIdReplacementTest : AbstractEngineTest() {
    @Test
    fun testReplacedSectionId() {
        templatorMocker.mockPageResponse("/engine/replacingId/cmsPageReplaceSectionId.json")
        fapiMocker.mockFapiResponse("/engine/replacingId/resolverReplaceId.json", "resolvePrime")

        assertScreen(
            "/engine/replacingId/cmsPageReplaceSectionIdOk.json",
            getScreenAny()
        )
    }

    @Test
    fun testSingleSectionNotReplacingId() {
        templatorMocker.mockPageResponse("/engine/replacingId/cmsPageReplaceSectionId.json")

        val screen = getScreenAny()

        val body = MapiScreenRequestBody<Any>().also { body ->
            body.sections = screen.sections.take(1).mapNotNull { SectionToRefresh.simple(it.rawSection) }
            body.context = screen.context
        }

        fapiMocker.mockFapiResponse("/engine/replacingId/resolverReplaceId.json", "resolvePrime")

        assertScreen(
            "/engine/replacingId/cmsPageReplaceSectionIdSingleOk.json",
            getScreenAny(body = body)
        )
    }
}
