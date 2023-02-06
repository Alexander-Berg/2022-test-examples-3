package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.BaseUacContentService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

abstract class UacContentGetControllerTestBase {
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    protected lateinit var userInfo: UserInfo

    protected abstract val uacContentService: BaseUacContentService

    protected abstract fun saveContent(content: UacYdbContent)

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun getContentSuccessfulTest() {
        val content = createDefaultImageContent(accountId = userInfo.clientId.toString())
        saveContent(content)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/content/${content.id}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val filledContent = uacContentService.fillContent(content)

        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    filledContent
                )
            )
        )
    }

    @Test
    fun getNonExistentTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/content/12345")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun getContentSuccessfulTest_WithImageHash() {
        val content = createDefaultImageContent(accountId = userInfo.clientId.toString())
        saveContent(content)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/content/hash/${content.directImageHash}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val filledContent = uacContentService.fillContent(content)

        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    filledContent
                )
            )
        )
    }

    @Test
    fun getNonExistentTest_WithImageHash() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/content/hash/12345")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
