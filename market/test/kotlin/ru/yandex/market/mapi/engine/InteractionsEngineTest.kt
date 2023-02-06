package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.util.mockFlags

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.03.2022
 */
class InteractionsEngineTest : AbstractEngineTest() {
    @Test
    fun testBasicPageWithResponse() {
        templatorMocker.mockPageResponse("/engine/interactions/simpleResponse.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOk.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOther.json", "resolveOther")

        assertScreenWithInteractions(
            "/engine/interactions/simpleResponseOk.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicSectionsPageWithResponse() {
        templatorMocker.mockPageResponse("/engine/interactions/simpleSectionsResponse.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOk.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOther.json", "resolveOther")

        assertScreenWithInteractions(
            "/engine/interactions/simpleSectionsResponseOk.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageNoAnalytics() {
        mockFlags(MapiHeaders.FLAG_INT_HIDE_ANALYTICS)

        templatorMocker.mockPageResponse("/engine/interactions/simpleResponse.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOk.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOther.json", "resolveOther")

        assertScreenWithInteractions(
            "/engine/interactions/simpleResponseNoAnalytics.json",
            getScreenAny()
        )
    }

    @Test
    fun testFailedSnippetInteractions() {
        templatorMocker.mockPageResponse("/engine/interactions/staticResponseSnippetFail.json")

        assertScreenWithInteractions(
            "/engine/interactions/staticResponseSnippetFailResult.json",
            getScreenAny()
        )
    }

    @Test
    fun testFailedSectionInteractions() {
        templatorMocker.mockPageResponse("/engine/interactions/staticResponseWidgetFail.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverFail.json")

        assertScreenWithInteractions(
            "/engine/interactions/staticResponseWidgetFailResult.json",
            getScreenAny()
        )
    }

    @Test
    fun testRecomContext() {
        templatorMocker.mockPageResponse("/engine/interactions/recomContextCheck.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOk.json")

        assertScreenWithInteractions("/engine/interactions/recomContextCheckResponse.json", getScreenAny())
    }

    @Test
    fun testRecomMultipleSections() {
        templatorMocker.mockPageResponse("/engine/interactions/multipleWidgetCheck.json")
        fapiMocker.mockFapiResponse("/engine/interactions/resolverOk.json")

        assertScreenWithInteractions("/engine/interactions/multipleWidgetCheckResponse.json", getScreenAny())
    }

    @Test
    fun testHideSection() {
        templatorMocker.mockPageResponse("/engine/interactions/hideSectionCheck.json")

        assertScreen("/engine/interactions/hideSectionCheckResponse.json", getScreenAny())
        assertErrorTrace("""
            tskv	timestamp=_CRTIME_	requestId=req_id	pageId=testId	context=section	place=InteractionsTestSection	placeId=123	app_platform=ISO	app_version=1.42	error=section_parse_error	reason=Test error to log in metrics
        """.trimIndent(), isInFile = false)
    }
}
