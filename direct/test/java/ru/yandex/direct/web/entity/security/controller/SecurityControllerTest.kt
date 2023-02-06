package ru.yandex.direct.web.entity.security.controller

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.direct.core.testing.MockMvcCreator
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.security.service.WebSecurityService

private const val METHOD_PATH = "/security/get_csrf_token"

@DirectWebTest
@RunWith(SpringRunner::class)
internal class SecurityControllerTest {

    @Autowired
    private lateinit var webSecurityService: WebSecurityService

    @Autowired
    private lateinit var mockMvcCreator: MockMvcCreator

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var steps: Steps

    private lateinit var securityController: SecurityController
    private lateinit var mockMvc: MockMvc

    @Before
    fun setUp() {
        securityController = SecurityController(directWebAuthenticationSource, webSecurityService)
        mockMvc = mockMvcCreator.setup(securityController).build()
    }

    @Test
    fun getCsrfToken_success() {
        val clientInfo = steps.clientSteps().createDefaultClient()

        val response = sendRequest(clientInfo.uid)

        val answer = JsonUtils.fromJson(response.contentAsString)
        val success = answer["success"].asBoolean()
        val sk = answer["sk"].textValue()
        val cacheControlHeader = response.getHeader("Cache-Control")
        SoftAssertions.assertSoftly {
            it.assertThat(success).`as`("Success").isTrue
            it.assertThat(sk).`as`("Csrf-token").isNotEmpty
            it.assertThat(cacheControlHeader).`as`("Cache-Control").contains("no-cache")
        }
    }

    private fun sendRequest(uid: Long): MockHttpServletResponse {
        testAuthHelper.setSubjectUser(uid)
        testAuthHelper.setOperator(uid)
        testAuthHelper.setSecurityContext()
        val requestBuilder = MockMvcRequestBuilders.get(METHOD_PATH)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        val perform = mockMvc.perform(requestBuilder)
        val result = perform.andReturn()
        return result.response
    }

}
