package ru.yandex.direct.web.entity.uac.controllerb

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.i18n.Language
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacRegionNameControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    private val expectedRegionsRu = mapOf(
        212L to "",
        213L to "Москва",
        225L to "Россия",
        10174L to "Санкт-Петербург"
    )

    private fun testGet(regionsQuery: String, lang: Language, expected: Map<Long, String>) {
        userInfo.user!!.lang = lang
        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/region_names?$regionsQuery")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertThat(JsonUtils.MAPPER.readTree(res)).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(expected)
            )
        )
    }

    @Test
    fun testGetCsvRu() {
        testGet("region=212,213,225,10174",
            Language.RU,
            expectedRegionsRu)
    }

    @Test
    fun testGetQueryStringRu() {
        testGet("region=212&region=213&region=225&region=10174",
            Language.RU,
            expectedRegionsRu)
    }

    @Test
    fun testNoRegions() {
        val res = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/region_names")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
