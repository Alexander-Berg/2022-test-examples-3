package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.05.2022
 */
class ContextEngineTest : AbstractEngineTest() {

    @Test
    fun testSectionRender() {
        val templatorClient = templatorMocker.getClient()
        templatorMocker.mockPageResponse("/engine/context/screenWithContext.json", "any")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        val screen = getScreenAny()
        assertScreen("/engine/context/screenWithContextResult.json", screen)

        // only one interaction with templator
        verify(templatorClient, times(1)).getCmsPageTemplate(any(), anyOrNull(), anyOrNull())

        // call single section manually
        val body = MapiScreenRequestBody<Any>().also { body ->
            body.sections = screen.sections.subList(1, 2).mapNotNull { SectionToRefresh.simple(it.rawSection) }
            body.context = screen.context
        }

        // check response by single section
        assertScreen("/engine/context/screenWithContextBySectionResult.json", getScreenAny(body = body))

        // still only one interaction with templator
        verify(templatorClient, times(1)).getCmsPageTemplate(any(), anyOrNull(), anyOrNull())
        verifyNoMoreInteractions(templatorClient)

        fapiMocker.verifyCall(2, "resolvePrime") { num, resolver ->
            assertJson(
                resolver.params!!, """
                    {
                      "sendParam": "someValue",
                      "page": 1
                    },
                """.trimIndent(), isExpectedInFile = false
            )
        }
    }

    @Test
    fun testMultipleSectionsRender() {
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        val responseFromFile = JsonHelper.parse<ScreenResponse>("/engine/context/screenWithContext.json".asResource())
        val body = MapiScreenRequestBody<Any>().also { body ->
            // refresh all sections maunually
            body.sections = responseFromFile.sections.mapNotNull { SectionToRefresh.simple(JsonHelper.toTree(it)) }
            body.context = responseFromFile.context
        }

        // check multiple sections with pager - should not affect results (batch update could be for 10+ sections)
        val screen = mapiScreenProcessor.getScreen(ScreenRequest().also { req ->
            req.pageSize = 1
        }, body = body)

        assertScreen("/engine/context/screenWithContextAllSectionResult.json", screen)

        // still only one interaction with templator
        val templatorClient = templatorMocker.getClient()
        verify(templatorClient, times(0)).getCmsPageTemplate(any(), anyOrNull(), anyOrNull())
        verifyNoMoreInteractions(templatorClient)
    }

    @Test
    fun testPagerContextMerge() {
        templatorMocker.mockPageResponse("/engine/context/screenWithContext.json", "any")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/resolverTestData2.json", "resolvePrimeOther")

        // load first page with its own context
        val screenPage1 = mapiScreenProcessor.getScreen(ScreenRequest().also { req ->
            req.pageSize = 1
            req.cmsPageType = "any"
        })

        assertScreen("/engine/context/pagedScreenResult1.json", screenPage1)

        // between pages cms-page changed, so there are new aliases and more sections
        templatorMocker.mockPageResponse("/engine/context/pagedScreenChanged.json", "any")

        val body = MapiScreenRequestBody<Any>().also { body ->
            body.context = screenPage1.context
        }

        val screenPage2 = mapiScreenProcessor.getScreen(ScreenRequest(pageToken = screenPage1.pageToken).also { req ->
            req.cmsPageType = "any"
        }, body = body)

        assertScreen("/engine/context/pagedScreenResult2.json", screenPage2)
    }
}
