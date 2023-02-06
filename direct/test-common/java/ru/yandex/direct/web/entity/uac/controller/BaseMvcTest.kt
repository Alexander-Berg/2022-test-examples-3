package ru.yandex.direct.web.entity.uac.controller

import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

abstract class BaseMvcTest {

    @Autowired
    protected lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    protected lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var webApplicationContext: WebApplicationContext

    protected lateinit var mockMvc: MockMvc

    @Before
    fun initBaseMvcTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }


    fun doRequest(
        url: String,
        method: HttpMethod,
        expectedStatus: Int,
        contentType: MediaType = MediaType.APPLICATION_JSON,
        params: Map<String, Any>? = null,
        body: Any? = null,
    ): ResultActions {
        val builder = MockMvcRequestBuilders.request(method, url)
        if (params != null) buildParams(builder, params)
        if (body != null) builder.content(JsonUtils.toJson(body))
        builder.contentType(contentType)

        return mockMvc.perform(builder)
            .andExpect(MockMvcResultMatchers.status().`is`(expectedStatus))
    }

    fun getRequest(
        url: String,
        expectedStatus: Int,
        params: Map<String, Any>? = null,
        contentType: MediaType = MediaType.APPLICATION_JSON
    ): ResultActions {
        return doRequest(
            url,
            HttpMethod.GET,
            expectedStatus,
            contentType,
            params
        )
    }

    private fun buildParams(
        builder: MockHttpServletRequestBuilder,
        params: Map<String, Any>
    ) {
        params.entries.forEach {
            builder.param(it.key, it.value.toString())
        }
    }
}
