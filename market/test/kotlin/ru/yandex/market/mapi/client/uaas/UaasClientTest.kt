package ru.yandex.market.mapi.client.uaas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import ru.yandex.market.common.retrofit.ahc.CustomAsyncHttpClientCall
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MapiContextRw
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.getMockContextRw
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.request.trace.Module

class UaasClientTest : AbstractClientTest() {
    private val conditionChecker = UaasConditionChecker()
    private val uaasClient = UaasClient(mockedRetrofit(Module.EXP_AB_API), conditionChecker)

    @BeforeEach
    fun initOauth() {
        mockOauth("mocked_token")
        conditionChecker.clearCache()
    }

    @Test
    fun testSimpleUaasCall() {
        getMockContextRw()?.appVersionRaw = "3.5.1"
        getMockContextRw()?.appPlatform = MapiHeaders.PLATFORM_IOS
        getMockContextRw()?.userAgent = "Special app user-agent"

        val testHeaders = JsonHelper.parse<Map<String, String>>("/client/uaas/uaasResponseHeaders.json".asResource())
        mockClientResponse(headers = testHeaders)
        val response = uaasClient.resolveExps(MapiContext.get(), ip = "mocked_ip").get()!!

        log.info("Result json: ${JsonHelper.toString(response)}")
        val expectedInfo = JsonHelper.parse<UserExpInfo>("/client/uaas/uaasResponseHeadersParsed.json".asResource())
        assertEquals(expectedInfo, response)

        verifyClientCall(1) { _, request ->
            request.assertRequest(HttpMethod.GET, "/bluemarketapps")
            request.assertHeaders(
                MapiHeaders.HEADER_AUTHORIZATION to "mocked_token",
                "X-Forwarded-For-Y" to "mocked_ip",
                "X-Yandex-UAAS" to "production",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000",
                strict = false,
            )
        }
    }

    @Test
    fun testUaasCallConditionsAndroid() {
        getMockContextRw()?.appVersionRaw = "1.05"
        getMockContextRw()?.appPlatform = MapiHeaders.PLATFORM_ANDROID
        getMockContextRw()?.userAgent = "Special app user-agent"

        val testHeaders = JsonHelper.parse<Map<String, String>>("/client/uaas/uaasConditionHeaders.json".asResource())
        mockClientResponse(headers = testHeaders)
        val response = uaasClient.resolveExps(MapiContext.get(), ip = "mocked_ip").get()!!

        log.info("Result json: ${JsonHelper.toString(response)}")
        val expectedInfo = JsonHelper.parse<UserExpInfo>("/client/uaas/uaasConditionHeadersParsedAndroid.json".asResource())
        assertEquals(expectedInfo, response)
    }

    @Test
    fun testUaasCallConditionsIos() {
        getMockContextRw()?.appVersionRaw = "2.15.2"
        getMockContextRw()?.appPlatform = MapiHeaders.PLATFORM_IOS
        getMockContextRw()?.userAgent = "Special app user-agent"

        val testHeaders = JsonHelper.parse<Map<String, String>>("/client/uaas/uaasConditionHeaders.json".asResource())
        mockClientResponse(headers = testHeaders)
        val response = uaasClient.resolveExps(MapiContext.get(), ip = "mocked_ip").get()!!

        log.info("Result json: ${JsonHelper.toString(response)}")
        val expectedInfo = JsonHelper.parse<UserExpInfo>("/client/uaas/uaasConditionHeadersParsedIphone.json".asResource())
        assertEquals(expectedInfo, response)
    }

    @Test
    fun testIpIsRequire() {
        val thrown = assertThrows<IllegalArgumentException> {
            uaasClient.resolveExps(MapiContext.get(), ip = null).get()
        }
        verifyClientCall(times = 0)
        assertEquals("Can't call uaas without real IP", thrown.message)
    }

    @Test
    fun testNonRequiredParametersSent() {
        val context = MapiContext.get() as? MapiContextRw ?: fail("invalid context")
        context.regionId = 213
        context.userAgent = "mocked_agent"
        context.uaasLogin = "mocked_uaas_login"
        context.forcedTestIds = listOf("1", "2", "3")
        context.forcedRearrs = listOf(
            "+someTestRearr",
            "-market_blue_buybox_delivery_context_approx_use_shop_id=1",
            "moarr"
        )
        context.uuid = "mocked_uuid"

        val testHeaders = JsonHelper.parse<Map<String, String>>("/client/uaas/uaasResponseHeaders.json".asResource())
        mockClientResponse(headers = testHeaders)

        val response = uaasClient.resolveExps(
            MapiContext.get(),
            ip = "mocked_ip"
        ).get()!!

        log.info("Result json: ${JsonHelper.toString(response)}")
        val expectedInfo =
            JsonHelper.parse<UserExpInfo>("/client/uaas/uaasResponseHeadersParsedWithForce.json".asResource())
        assertEquals(expectedInfo, response)

        verifyClientCall(1) { _, request ->
            request.assertQuery(
                "uuid" to "mocked_uuid",
                "test-id" to "1_2_3"
            )

            request.assertHeaders(
                MapiHeaders.HEADER_AUTHORIZATION to "mocked_token",
                MapiHeaders.HEADER_USER_AGENT to "mocked_agent",
                "X-Forwarded-For-Y" to "mocked_ip",
                "X-Yandex-UAAS" to "testing",
                "X-Region-City-Id" to "213",
                "X-Yandex-Login-For-UAAS" to "mocked_uaas_login",
                CustomAsyncHttpClientCall.REQ_TIMEOUT_MS_HEADER to "1000"
            )
        }
    }
}
