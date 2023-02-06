package ru.yandex.direct.web.entity.uac.controller

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

abstract class UacContentDeleteControllerTestBase {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    protected lateinit var userInfo: UserInfo

    protected abstract fun saveContent(userInfo: UserInfo): String
    protected abstract fun contentExists(id: String): Boolean

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun deleteNonExistentTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/сontent/1231231")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteNonValidStringIdTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/content/uac")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteNonValidMoreThanUint64IdTest() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/content/109467396721177068399")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun deleteSuccessfulTest() {
        val contentId = saveContent(userInfo)

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/content/${contentId}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        assertThatKt(contentExists(contentId)).isFalse()
    }

    @Test
    fun deleteNoRightsTest() {
        val anotherUserInfo = testAuthHelper.createDefaultUser()

        val contentId = saveContent(userInfo)

        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/uac/content/${contentId}?ulogin=" + anotherUserInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)

        assertThatKt(contentExists(contentId)).isTrue()
    }
}
