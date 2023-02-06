package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.doReturn
import java.util.Locale
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.appmetrika.AppMetrikaClient
import ru.yandex.direct.appmetrika.model.response.Application
import ru.yandex.direct.appmetrika.model.response.BundleId
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacAppMetrikaAppsControllerTest {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var appMetrikaClient: AppMetrikaClient

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo
    private lateinit var applications: List<AppInfo>

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        val appMetrikaApplications: List<Application> = listOf(
            Application().apply {
                bundleIds = listOf(
                    BundleId().apply {
                        bundleId = "com.yandex.some"
                        platform = "android"
                    },
                    BundleId().apply {
                        bundleId = "com.yandex.some"
                        platform = "ios"
                    }
                )
            },
            Application().apply {
                bundleIds = listOf(
                    BundleId().apply {
                        bundleId = "com.yandex.other"
                        platform = "android"
                    }
                )
            },
            Application().apply {
                bundleIds = listOf(
                    BundleId().apply {
                        bundleId = "com.yandex.another"
                        platform = "ios"
                    }
                )
            }
        )
        doReturn(appMetrikaApplications).`when`(appMetrikaClient).getApplications(
            userInfo.uid, null, null, null, 100, null
        )

        val firstAppInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        val secondAppInfo = defaultAppInfo(
            appId = "com.yandex.other", bundleId = "com.yandex.other",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        val thirdAppInfo = defaultAppInfo(
            appId = "id123", bundleId = "com.yandex.another",
            platform = Platform.IOS, source = Store.ITUNES,
            data = IOS_APP_INFO_DATA
        )
        for (uacYdbAppInfo in listOf(firstAppInfo, secondAppInfo, thirdAppInfo)) {
            uacAppInfoRepository.saveAppInfo(uacYdbAppInfo)
        }
        applications = listOf(firstAppInfo, secondAppInfo, thirdAppInfo).map { uacAppInfoService.getAppInfo(it) }
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun getApps() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/appmetrika/applications?ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(applications)
            )
        )
    }

    @Test
    fun getAppsWithText() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/appmetrika/applications?text=arkin&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        Assertions.assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(listOf(applications[2]))
            )
        )
    }
}
