package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.SoftAssertions
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
import ru.yandex.direct.core.entity.uac.model.AppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp
import ru.yandex.direct.web.core.entity.mobilecontent.service.WebCoreMobileAppService
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateDirectAppRequest
import ru.yandex.direct.web.entity.uac.model.UacDirectAppResponse

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacDirectAppsControllerTest {
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
    private lateinit var webCoreMobileAppService: WebCoreMobileAppService

    private lateinit var userInfo: UserInfo
    private lateinit var appInfo: AppInfo
    private lateinit var appInfoUs: AppInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        val ydbAppInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        val ydbAppInfoUs = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA, region = "us"
        )
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        uacAppInfoRepository.saveAppInfo(ydbAppInfoUs)
        appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)
        appInfoUs = uacAppInfoService.getAppInfo(ydbAppInfoUs)
    }

    @Test
    fun testCreateAndGet() {
        val request = CreateDirectAppRequest(id = appInfo.id!!)
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        createDirectApp(request)
        val soft = SoftAssertions()

        val mobileApps = webCoreMobileAppService.getAppList(subjectUser.clientId, null, null)
        soft.assertThat(mobileApps).hasSize(1)
        soft.assertThat(mobileApps[0].mobileContent.storeContentId).isEqualTo(appInfo.appId)
        soft.assertThat(mobileApps[0].storeHref).isEqualTo(appInfo.url)

        checkDirectApp(soft, appInfo.id!!, mobileApps[0])

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/direct_apps?app_info.id=${appInfoUs.id!!}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
        soft.assertAll()
    }

    @Test
    fun testWithTwoApps() {
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        createDirectApp(CreateDirectAppRequest(id = appInfo.id!!))
        createDirectApp(CreateDirectAppRequest(id = appInfoUs.id!!))
        val soft = SoftAssertions()

        val mobileApps = webCoreMobileAppService.getAppList(subjectUser.clientId, null, null)
        soft.assertThat(mobileApps).hasSize(2)
        soft.assertThat(mobileApps[0].mobileContent.storeContentId).isEqualTo(appInfo.appId)
        soft.assertThat(mobileApps[0].storeHref).isEqualTo(appInfo.url)

        soft.assertThat(mobileApps[1].mobileContent.storeContentId).isEqualTo(appInfoUs.appId)
        soft.assertThat(mobileApps[1].storeHref).isEqualTo(appInfoUs.url)

        checkDirectApp(soft, appInfo.id!!, mobileApps[0])
        checkDirectApp(soft, appInfoUs.id!!, mobileApps[1])
        soft.assertAll()
    }

    private fun createDirectApp(request: CreateDirectAppRequest): String {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/direct_apps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
    }

    private fun checkDirectApp(soft: SoftAssertions, appInfoId: String, mobileApp: WebMobileApp) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/direct_apps?app_info.id=$appInfoId")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val actual = JsonUtils.fromJson(result, UacDirectAppResponse::class.java)
        soft.assertThat(actual.result.id).isEqualTo(mobileApp.id.toString())
        soft.assertThat(actual.result.hasVerification).isEqualTo(mobileApp.hasVerification)
        soft.assertThat(actual.result.busySlots).isEqualTo(mobileApp.busySkadNetworkSlotsCount)
        soft.assertThat(actual.result.slotsCount).isEqualTo(mobileApp.skadNetworkSlotsCount)
    }
}
