package ru.yandex.market.mapi.controller.custom

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.client.uaas.UaasClient
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.response.MapiResponseDto
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.mock.FapiMocker
import ru.yandex.market.mapi.mock.TemplatorMocker
import java.util.function.Supplier

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.04.2022
 */
class CustomTestScreenControllerTest : AbstractMapiTest() {
    @Autowired
    lateinit var templatorMocker: TemplatorMocker

    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Autowired
    lateinit var uaasClient: UaasClient

    @Test
    fun testCmsScreen() {
        templatorMocker.mockPageResponse(
            "/engine/basicCmsTestPageWithResolver.json",
            "test_cms_page",
            "mobile_scheme_product_card"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(
            mvcCall(get("/api/screen/test/cms")),
            "/controller/basicScreenWithResolverResut.json"
        )
    }

    @Test
    fun testCmsScreenWithForceExp() {
        templatorMocker.mockPageResponse(
            "/engine/basicCmsTestPageWithResolver.json",
            "test_cms_page",
            "mobile_scheme_product_card"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(
            mvcCall(
                get("/api/screen/test/cms")
                    .header(MapiHeaders.HEADER_X_FORCED_TEST_IDS, "test1,test2")
                    .header(MapiHeaders.HEADER_X_FORCED_REARR, "+someRearr;+otherRearr;-market_white_cpa_on_blue=2")
            ),
            "/controller/basicScreenResutWithExp.json"
        )
    }

    @Test
    fun testCmsScreenWithForceExpBroken() {
        templatorMocker.mockPageResponse(
            "/engine/basicCmsTestPageWithResolver.json",
            "test_cms_page",
            "mobile_scheme_product_card"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        // break uaas
        whenever(uaasClient.resolveExps(any(), any())).thenReturn(Supplier { throw RuntimeException("some message") })

        // expect only forced+default rearrs to be used
        assertJson(
            mvcCall(
                get("/api/screen/test/cms")
                    .header(MapiHeaders.HEADER_X_FORCED_REARR, "+someRearr;+otherRearr")
            ),
            "/controller/basicScreenResutWithExpBroken.json"
        )
    }

    @Test
    fun testCmsScreenWithForceExpBrokenTimeout() {
        templatorMocker.mockPageResponse(
            "/engine/basicCmsTestPageWithResolver.json",
            "test_cms_page",
            "mobile_scheme_product_card"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        // break uaas (null returned on timeout)
        whenever(uaasClient.resolveExps(any(), any())).thenReturn(Supplier { null })

        // expect only forced+default rearrs to be used
        assertJson(
            mvcCall(
                get("/api/screen/test/cms")
                    .header(MapiHeaders.HEADER_X_FORCED_REARR, "+someRearr;+otherRearr")
            ),
            "/controller/basicScreenResutWithExpTimeout.json"
        )
    }

    @Test
    fun testFileScreen() {
        templatorMocker.mockFailedCallPageResponse()
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(
            mvcCall(get("/api/screen/test/static")),
            "/controller/staticScreenTest.json"
        )
    }

    @Test
    fun testCustomScreen() {
        templatorMocker.mockPageResponse(
            "/engine/basicCmsTestPageWithResolver.json",
            "cms_page"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(
            mvcCall(
                get("/api/screen/test/custom")
                    .param(MapiConstants.SKU_ID, "skuIdCustom")
            ),
            "/controller/customScreenTest.json"
        )

        checkResolverCalls(
            "resolveCustom",
            mapOf(
                "key" to "value",
                "sku" to "skuIdCustom"
            )
        )
    }

    @Test
    fun testCustomScreenWithContext() {
        templatorMocker.mockPageResponse(
            "/controller/customScreenWithContext.json",
            "cms_page"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(
            mvcCall(
                post("/api/screen/test/custom/post")
                    .param(MapiConstants.SKU_ID, "skuIdCustom")
                    .header(MapiHeaders.HEADER_UUID, "test-uuid-1")
                    .header(MapiHeaders.HEADER_FLAGS, MapiHeaders.FLAG_INT_HIDE_ANALYTICS),
                body = MapiScreenRequestBody<Any>()
            ),
            "/controller/customScreenWithContextResult.json"
        )

        checkResolverCalls(
            "resolvePrime",
            mapOf(
                "paramCtx" to "valueT",
                "paramSku" to "skuIdCustom",
                "paramUuid" to "test-uuid-1",
            )
        )
    }

    @Test
    fun testCustomScreenWithContextUpdate() {
        templatorMocker.mockPageResponse(
            "/controller/customScreenWithContext.json",
            "cms_page"
        )
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        val firstCallStr = mvcCall(
            post("/api/screen/test/custom/post")
                .param(MapiConstants.SKU_ID, "skuIdCustom")
                .header(MapiHeaders.HEADER_FLAGS, MapiHeaders.FLAG_INT_HIDE_ANALYTICS),
            body = MapiScreenRequestBody<Any>()
        )

        assertJson(firstCallStr, "/controller/customScreenWithContextResult.json")

        val firstCall = JsonHelper.parse<MapiResponseDto>(firstCallStr)

        templatorMocker.mockPageErrorResponse("cms_page")
        val refreshBody = MapiScreenRequestBody<Any>().apply {
            sections = listOf(
                SectionToRefresh().apply {
                    raw = firstCall.shared?.sections?.get("111240982")
                }
            )
            context = firstCall.context
        }

        val refreshStr = mvcCall(
            post("/api/screen/test/custom/post")
                .param(MapiConstants.SKU_ID, "skuIdCustom")
                .header(MapiHeaders.HEADER_FLAGS, MapiHeaders.FLAG_INT_HIDE_ANALYTICS),
            body = refreshBody
        )

        assertJson(refreshStr, "/controller/customScreenWithContextRefresh.json")
    }

    private fun checkResolverCalls(name: String, item: Any) {
        fapiMocker.verifyCall(1, name = name) { _, resolver ->
            assertJson(
                resolver.params?.let { JsonHelper.toString(it) } ?: "{}",
                JsonHelper.toString(item),
                isExpectedInFile = false
            )
        }
    }
}