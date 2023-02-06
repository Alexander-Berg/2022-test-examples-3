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
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacPropertiesControllerTest {
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

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
    fun testGetWithDefaultValues() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/properties")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response["max_texts"].asInt()).isEqualTo(4)
        Assertions.assertThat(response["max_titles"].asInt()).isEqualTo(4)
    }

    @Test
    fun testGetWithProperties() {
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_TEXTS_IN_UAC, "5")
        ppcPropertiesSupport.set(PpcPropertyNames.MAX_TITLES_IN_UAC, "6")
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/properties")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        Assertions.assertThat(response["max_texts"].asInt()).isEqualTo(5)
        Assertions.assertThat(response["max_titles"].asInt()).isEqualTo(6)
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_TEXTS_IN_UAC)
        ppcPropertiesSupport.remove(PpcPropertyNames.MAX_TITLES_IN_UAC)
    }
}
