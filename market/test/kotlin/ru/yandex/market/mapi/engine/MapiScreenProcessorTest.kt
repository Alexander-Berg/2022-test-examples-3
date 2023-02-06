package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import ru.yandex.market.mapi.core.MapiEnvironment
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.model.section.EngineTestAssembler
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.mapi.util.mapiContextRw

/**
 * Screen processor engine tests
 * @author Ilya Kislitsyn / ilyakis@ / 28.01.2022
 */
class MapiScreenProcessorTest : AbstractEngineTest() {

    @Test
    fun testBasicPage() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json")
        assertScreen(
            "/engine/basicCmsTestPageOk.json",
            getScreenAny(),
            cleanSnippetType = false
        )
    }

    @Test
    fun testBasicPageWithUnimplementedSkipResolvers() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageCheckSkipUnknown.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/basicCmsTestPageCheckSkipUnknownResult.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testBasicPageInvalidContent() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageInvalidContent.json")
        assertScreen(
            "/engine/basicCmsTestPageInvalidContentFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiError() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiErrorResponse()

        assertScreen(
            "/engine/basicCmsTestPageWithResolverFallbackFapiError.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiOk() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithResolverOk.json", getScreenAny())
    }

    @Test
    fun testBasicPageFapiOkWithRes() {
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE)

        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithResolverOkRes.json", getScreenAny())
    }

    @Test
    fun testBasicPageFapiOkWithRaw() {
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE, MapiHeaders.FLAG_INT_KEEP_RAW_RESPONSE)

        // respource+raw
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithResolverOkRaw.json", getScreenAny())
    }

    @Test
    fun testBasicPageWithDebugRequests() {
        mockFlags(
            MapiHeaders.FLAG_INT_KEEP_RESOURCE,
            MapiHeaders.FLAG_INT_DEBUG_REQUESTS
        )

        // respource+raw
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithDebugInfo.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", debugInfo = "curl request")

        assertScreen("/engine/basicCmsTestPageWithResolverOkDebugRequest.json", getScreenAny())
    }

    @Test
    fun testBasicPageWithDebugRequestsInProd() {
        mapiContextRw().environment = MapiEnvironment.PRODUCTION
        mockFlags(
            MapiHeaders.FLAG_INT_KEEP_RESOURCE,
            MapiHeaders.FLAG_INT_DEBUG_REQUESTS
        )

        // respource+raw
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", debugInfo = "curl request")

        assertScreen("/engine/basicCmsTestPageWithResolverOk.json", getScreenAny())
    }

    @Test
    fun testBasicPageFapiOkWithOnlyRaw() {
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RAW_RESPONSE)

        // doesn't matters alone
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithResolverOkOnlyRaw.json", getScreenAny())
    }

    @Test
    fun testBasicPageFapiOkWithRawInProd() {
        mapiContextRw().environment = MapiEnvironment.PRODUCTION
        mockFlags(MapiHeaders.FLAG_INT_KEEP_RESOURCE, MapiHeaders.FLAG_INT_KEEP_RAW_RESPONSE)

        // respource+raw can't work in prod
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithResolverOk.json", getScreenAny())
    }

    @Test
    fun testBasicPageFapiParseError() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestDataFailTest.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverFallbackParsingError.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiAssemblyError() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestDataAssemblyCheck.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverFallbackAssemblyError.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiAssemblyFatalError() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestDataAssemblyException.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverFallbackAssemblyFatalError.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiInvalidAssembler() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolverInvalidAssembler.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverInvalidAssemblerFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiNoAssembler() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolverNoAssembler.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverNoAssemblerFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiNoParams() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolverNoParams.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverNoParamsFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiNoParamsStatic() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolverNoParamsStatic.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/basicCmsTestPageWithResolverNoParamsFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testBasicPageFapiTimeout() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiTimeoutFuture()

        assertScreen(
            "/engine/basicCmsTestPageWithResolverFallbackTimeout.json",
            getScreenAny()
        )

        assertErrorTrace("/engine/basicCmsTestPageWithResolverFallbackTimeoutTskvError.csv")
    }

    @Test
    fun testBasicPageFapiMultiErrors() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithMultiResolvers.json")

        fapiMocker.mockFapiTimeoutResponse("resolvePrimeTimeout")
        fapiMocker.mockFapiErrorResponse("resolvePrimeError")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime")

        assertScreen(
            "/engine/basicCmsTestPageWithMultiResolversFallback.json",
            getScreenAny()
        )

        assertErrorTrace("/engine/basicCmsTestPageWithMultiResolversTskvError.csv")
    }

    @Test
    fun testMockCmsError() {
        templatorMocker.mockPageErrorResponse()

        assertScreen(
            "/engine/unexpectedErrorFallbackOnTestPage.json",
            getScreen {
                cmsPageType = "test_page"
            }
        )
    }

    @Test
    fun testMockCmsError2() {
        templatorMocker.mockFailedCallPageResponse()

        assertScreen(
            "/engine/unexpectedErrorFallbackOnTestPage.json",
            getScreen {
                cmsPageType = "test_page"
            }
        )
    }

    @Test
    fun testMockCmsErrorSimpleFallback() {
        templatorMocker.mockPageErrorResponse()

        assertScreen(
            "/engine/generalFallback.json",
            getScreenAny()
        )
    }

    @Test
    fun testPaging() {
        templatorMocker.mockPageResponse("/engine/mainScreenPagedRawResponse.json")

        assertScreen(
            "/engine/mainScreenPage1Response.json",
            mapiScreenProcessor.getScreen(
                ScreenRequest(
                    pageSize = 2
                ).apply {
                    cmsPageType = "any"
                }
            )
        )

        assertScreen(
            "/engine/mainScreenPage2Response.json",
            mapiScreenProcessor.getScreen(
                ScreenRequest(
                    pageToken = "2;3", pageSize = 2
                ).apply {
                    cmsPageType = "any"
                }
            )
        )
    }

    @Test
    fun testCacheFallback() {
        templatorMocker.mockPageErrorResponse()
        templatorMocker.mockCachedPageResponse("/engine/basicCmsTestPage.json")

        assertScreen(
            "/engine/basicCmsTestPageOkCached.json",
            getScreenAny()
        )

        val client = templatorMocker.getClient()

        verify(client).getCmsPageTemplate("any")
        verify(client).getCmsPageTemplateSimple("any")
        verifyNoMoreInteractions(client)
    }

    @Test
    fun testFallbackToStaticThroughCache() {
        templatorMocker.mockPageErrorResponse()
        templatorMocker.mockCachedPageResponseError()

        assertScreen(
            "/engine/generalFallbackThroughCache.json",
            getScreenAny()
        )

        val client = templatorMocker.getClient()
        verify(client).getCmsPageTemplate("any")
        verify(client).getCmsPageTemplateSimple("any")
        verifyNoMoreInteractions(client)
    }

    @Test
    fun testCorrectCacheUsage() {
        templatorMocker.mockPageErrorResponse()
        templatorMocker.mockCachedPageResponse("/engine/basicCmsTestPage.json")

        // first request
        assertScreen(
            "/engine/basicCmsTestPageOkCached.json",
            getScreenAny()
        )

        val client = templatorMocker.getClient()

        verify(client, times(1)).getCmsPageTemplate("any")
        verify(client, times(1)).getCmsPageTemplateSimple("any")

        // set mapi context to initial state
        prepareMapiContext()

        // second request
        assertScreen(
            "/engine/basicCmsTestPageOkCached.json",
            getScreenAny()
        )

        verify(client, times(2)).getCmsPageTemplate("any")
        verify(client, times(1)).getCmsPageTemplateSimple("any")

        verifyNoMoreInteractions(client)
    }

    @Test
    fun testNeedAuthNoAuth() {
        templatorMocker.mockPageResponse("/engine/needAuthTestPage.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/needAuthTestPageNoAuth.json", getScreenAny())
    }

    @Test
    fun testNeedAuthWithAuth() {
        mockOauth("test token")

        templatorMocker.mockPageResponse("/engine/needAuthTestPage.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/needAuthTestPageWithAuth.json", getScreenAny())
    }

    @Test
    fun testWithGenerated() {
        mockFlags(EngineTestAssembler.FLAG_GENERATE_TEST)

        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithGenerated.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/basicCmsTestPageWithGeneratedResult.json", getScreenAny())
    }

    @Test
    fun testSectionShowCondition() {
        templatorMocker.mockPageResponse("/engine/showconditions/basicShowCondition.json")
        fapiMocker.mockFapiErrorResponse("resolvePrimeHidden")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrimeOK")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrimeNoneHide")

        assertScreen("/engine/showconditions/basicShowConditionResult.json", getScreenAny())
    }

    @Test
    fun testUnknownShowCondition() {
        templatorMocker.mockPageResponse("/engine/showconditions/unknownCondition.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen("/engine/showconditions/unknownConditionResult.json", getScreenAny())
    }
}
