package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingUrlCreateImpressionUrlControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun testInvalidTrackingUrl() {
        checkBadRequest(
            mockMvc,
            mapOf(
                "url" to "htps://example.com",
            ),
            "INVALID_TRACKING_URL"
        )
    }

    @Test
    fun testUrlFromSystemWithoutImpressionUrls() {
        checkBadRequest(
            mockMvc,
            mapOf(
                "url" to "http://hastrk3.com/pub_c?adgroup_id=3601",
            ),
            "CANNOT_MAKE_IMPRESSION_URL"
        )
    }

    @Test
    fun testUrlFromSystemWithoutAlgorithmForCreatingImpressionUrls() {
        checkBadRequest(
            mockMvc,
            mapOf(
                "url" to "https://nike.sng.link/Dstn5/dxeu?cp=NA",
            ),
            "CANNOT_MAKE_IMPRESSION_URL"
        )
    }

    @Test
    fun testAdjustUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                "url" to "https://app.adjust.com/nnaes87?campaign={campaign_id}_{campaign_name_lat}",
            ),
            mapOf(
                "url" to "https://view.adjust.com/impression/nnaes87?campaign={campaign_id}_{campaign_name_lat}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}",
                "external_link" to "https://www.adjust.com",
            )
        )
    }

    @Test
    fun testAdjustUniversalUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                "url" to "http://app.adj.st?adjust_t=nnaes88&adjust_first=value1&adj_second=value2&ordinary=value3",
                "platform" to null
            ),
            mapOf(
                "url" to "http://view.adjust.com/impression/nnaes88?first=value1&second=value2&ordinary=value3&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}",
                "external_link" to "https://www.adjust.com",
            )
        )
    }

    @Test
    fun testAdjustUniversalUrl_WithAdjTParam() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                "url" to "http://app.adj.st?adj_t=nnaes88&adjust_first=value1&adj_second=value2&ordinary=value3",
                "platform" to null
            ),
            mapOf(
                "url" to "http://view.adjust.com/impression/nnaes88?first=value1&second=value2&ordinary=value3&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}",
                "external_link" to "https://www.adjust.com",
            )
        )
    }

    @Test
    fun testAdjustUniversalUrl_WithoutTrackerId() {
        checkBadRequest(
            mockMvc,
            mapOf(
                "url" to "http://app.adj.st?adjust_first=value1&adj_second=value2&ordinary=value3",
                "platform" to null
            ),
            "CANNOT_MAKE_IMPRESSION_URL"
        )
    }

    @Test
    fun testAppsflyerUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                "url" to "http://app.appsflyer.com/com.greatapp?af_click_lookback=value",
                "platform" to 1
            ),
            mapOf(
                "url" to "http://impression.appsflyer.com/com.greatapp?af_viewthrough_lookback=1d&advertising_id={google_aid}&oaid={oaid}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
                "external_link" to "https://www.appsflyer.com",
            )
        )
    }

    @Test
    fun testMyTrackerUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                "url" to "https://trk.mail.ru/c/uh0jz9",
                "platform" to 2
            ),
            mapOf(
                "url" to "https://trk.mail.ru/i/uh0jz9?mt_idfa={ios_ifa}&clickId={logid}&regid={logid}",
                "external_link" to "https://tracker.my.com",
            )
        )
    }

    private fun checkBadRequest(mockMvc: MockMvc, request: Map<String, Any?>, expectedDefect: String) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/create_impression_url")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        val resultContent = result.andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val realDefect = JsonUtils.fromJson(resultContent)["validation_result"]["errors"][0]["code"].asText()
        realDefect.split('.').last().checkEquals(expectedDefect)
    }

    private fun checkCorrectRequest(mockMvc: MockMvc, request: Map<String, Any?>, expectedResponse: Map<String, Any?>) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/create_impression_url")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        Assertions.assertThat(JsonUtils.MAPPER.readTree(resultContent)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expectedResponse)
            )
        )
    }
}

