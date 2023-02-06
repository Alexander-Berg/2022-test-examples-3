package ru.yandex.market.mapi.client.fapi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import ru.yandex.market.common.retrofit.ahc.CustomAsyncHttpClientCall
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MapiContextRw
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.contract.ClientConfigProvider
import ru.yandex.market.mapi.core.model.Location
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.mockExpInfo
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.request.trace.Module
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.01.2022
 */
class FapiClientTest : AbstractClientTest() {
    private val clientConfigProvider = mock<ClientConfigProvider>()
    private val fapiClient = FapiClient(mockedRetrofit(Module.MARKET_FRONT), clientConfigProvider)

    @BeforeEach
    fun setup() {
        whenever(clientConfigProvider.getFapiTimeout()).thenReturn(null)
    }

    @Test
    fun testSimpleFapiCall() {
        val context = MapiContext.get() as MapiContextRw
        context.location = Location.parse("12.12;66.666")
        context.deviceId = "some-device-id"
        context.uuid = "some-uuid"

        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        assertResponse(
            fapiClient.callResolver(resolver, reuseResponse = false)
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery(
                "name" to "resolvername",
                "gps" to "66.666%2C12.12"
            )
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000",
                "X-AppMetrica-DeviceId" to "some-device-id",
                "uuid" to "some-uuid"
            )
        }
    }

    @Test
    fun testFapiCallWithExp() {
        val context = MapiContext.get() as MapiContextRw
        context.location = Location.parse("12.12;66.666")
        mockExpInfo(UserExpInfo(
            version = "test",
            uaasRearrs = linkedSetOf("someRearr=0")
        ))

        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        assertResponse(
            fapiClient.callResolver(resolver, reuseResponse = false)
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery(
                "name" to "resolvername",
                "gps" to "66.666%2C12.12",
                "rearr-factors" to "someRearr%3D0%3Bmarket_support_market_sku_in_product_redirects%3D1%3Bmarket_white_cpa_on_blue%3D2%3Bmarket_enable_foodtech_offers%3Deda_retail"
            )
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                "X-Market-Rearrfactors" to "someRearr=0;market_support_market_sku_in_product_redirects=1;market_white_cpa_on_blue=2;market_enable_foodtech_offers=eda_retail",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }

    @Test
    fun testFapiCallWithCfgTimeout() {
        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        whenever(clientConfigProvider.getFapiTimeout()).thenReturn(12345)

        assertResponse(
            fapiClient.callResolver(resolver, reuseResponse = false)
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery(
                "name" to "resolvername",
            )
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "12345"
            )
        }
    }

    @Test
    fun testFapiCallWithCfgTimeoutNull() {
        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        // bad integer - should not be sent to client
        whenever(clientConfigProvider.getFapiTimeout()).thenReturn(null)

        assertResponse(
            fapiClient.callResolver(resolver, reuseResponse = false)
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }

    @Test
    fun testFapiBatchCall() {
        mockClientResponse("/client/fapi/testResponse.json")

        assertResponse(
            fapiClient.callResolverBatch(
                "bachTest",
                listOf(buildRequest("firstRes"), buildRequest("secondRes")),
                reuseResponse = false
            )
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery("name" to "firstRes%2CsecondRes")
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }

    @Test
    fun testSimpleFapiCallMultipleNoReuse() {
        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        val future = fapiClient.callResolver(resolver, reuseResponse = false)
        assertResponse(
            future.thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        // fine (cached)
        assertResponse(
            future.thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        // should fail (new parsing to new class)
        try {
            future.thenApply { response -> response.parse(TestOtherFapiResponse::class) }.get()
            fail("Should fail above")
        } catch (e: ExecutionException) {
            assertNotNull(e.cause)
            assertTrue { e.cause is IOException }
            assertEquals("Stream closed", e.cause?.message)
        }
    }

    @Test
    fun testSimpleFapiCallMultipleWithReuse() {
        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        val future = fapiClient.callResolver(resolver, reuseResponse = true)
        assertResponse(
            future.thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        // now second parse is fine
        assertResponse(
            future.thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall(times = 1) { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery("name" to "resolvername")
        }
    }

    @Test
    fun testSimpleFapiCallWithQueryParams() {
        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest(queryParams = hashMapOf("param_key" to "param_value"))

        val future = fapiClient.callResolver(resolver, reuseResponse = false)
        assertResponse(
            future.thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall(times = 1) { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery("name" to "resolvername", "param_key" to "param_value")
        }
    }

    @Test
    fun testFapiBatchCallWithQueryParams() {
        mockClientResponse("/client/fapi/testResponse.json")

        val queryParams: HashMap<String, String> = hashMapOf("param_key" to "param_value")
        assertResponse(
            fapiClient.callResolverBatch(
                "bachTest",
                listOf(
                    buildRequest("firstRes", queryParams),
                    buildRequest("secondRes", queryParams)),
                reuseResponse = false
            )
                .thenApply { response -> response.parse(TestFapiResponse::class) },
            "/client/fapi/testResponseParsed.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery("name" to "firstRes%2CsecondRes", "param_key" to "param_value")
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }

    @Test
    fun testCallWithDebug() {
        val context = MapiContext.get() as MapiContextRw
        context.location = Location.parse("12.12;66.666")
        context.deviceId = "some-device-id"
        context.uuid = "some-uuid"
        mockFlags(MapiHeaders.FLAG_INT_DEBUG_REQUESTS)

        mockClientResponse("/client/fapi/testResponse.json")
        val resolver = buildRequest()

        val actualResponse = fapiClient.callResolver(resolver, reuseResponse = false).get()

        assertResponse(
            actualResponse.parse(TestFapiResponse::class),
            "/client/fapi/testResponseParsed.json"
        )

        assertEquals(
            "curl -X POST 'http://someurl-market_front/api/v5?name=resolvername&gps=66.666%2C12.12' -H 'accept: application/json' -H 'accept-encoding: gzip' -H 'api-platform: IOS' -H 'sec_header: sec_value' -H 'sec_header2: sec_value2' -H 'uuid: some-uuid' -H 'x-appmetrica-deviceid: some-device-id' -H 'x-device-type: SMARTPHONE' -H 'x-market-req-id: req_id/1' -H 'x-request-timeout-ms: 1000' -d '{\"params\":[{\"key\":\"value\"}]}'",
            actualResponse.getDebugInfo()
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/api/v5")
            request.assertQuery(
                "name" to "resolvername",
                "gps" to "66.666%2C12.12"
            )
            request.assertHeaders(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                HttpHeaders.ACCEPT_ENCODING to "gzip",
                "api-platform" to "IOS",
                "X-Device-Type" to "SMARTPHONE",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000",
                "X-AppMetrica-DeviceId" to "some-device-id",
                "uuid" to "some-uuid"
            )
        }
    }

    private fun buildRequest(resolverName: String? = null, queryParams: HashMap<String, String>? = null): ResourceResolver {
        val resolver = ResourceResolver.simple(resolverName ?: "resolvername")
        resolver.version = "v5"
        resolver.params = JsonHelper.parseTree(""" {"key":"value"} """.trim())
        resolver.queryParams = queryParams
        return resolver
    }

    class TestFapiResponse {
        var results: List<TestFapiResult>? = null
    }

    class TestFapiResult(val result: String, val theAnswer: Int?) {
        var error: ResultError? = null
    }

    class ResultError(
        val kind: String,
        val message: String? = null
    )

    class TestOtherFapiResponse {
        var results: List<TestFapiResult>? = null
    }
}
