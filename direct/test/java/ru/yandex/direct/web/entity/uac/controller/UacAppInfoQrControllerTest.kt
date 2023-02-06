package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
import org.junit.After
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
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.UacQr

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacAppInfoQrControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo

    private lateinit var qr: UacQr

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        val firstAppInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(firstAppInfo)
        qr = UacQr(uacAppInfoService.getAppInfo(firstAppInfo), "https://example.com?is_qr=1")
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testGetAppInfoWithLink() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/app_info/qr?bundle_id=com.yandex.some&count_link=https://example.com")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(qr)
            )
        )
    }

    @Test
    fun testNotFound() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/app_info/qr?bundle_id=notExist&count_link=https://example.com")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
