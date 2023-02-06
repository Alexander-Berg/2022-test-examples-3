package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.entity.uac.service.YdbUacClientService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

abstract class UacMyAppsControllerTestBase {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    @Autowired
    protected lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    protected lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var ydbUacClientService: YdbUacClientService

    protected lateinit var mockMvc: MockMvc

    protected lateinit var userInfo: UserInfo

    protected lateinit var accountId: String

    protected lateinit var uacAppInfo1: UacYdbAppInfo

    protected lateinit var uacAppInfo2: UacYdbAppInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()

        val operator = userInfo.clientInfo?.chiefUserInfo?.user!!
        val subjectUser = userInfo.clientInfo?.chiefUserInfo?.user!!
        accountId = ydbUacClientService.getOrCreateClient(operator, subjectUser)

        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        uacAppInfo1 = defaultAppInfo(platform = Platform.IOS, source = Store.ITUNES, data = IOS_APP_INFO_DATA)
        uacAppInfo2 = defaultAppInfo(platform = Platform.ANDROID, source = Store.GOOGLE_PLAY, data = ANDROID_APP_INFO_DATA)

        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo1)
        uacYdbAppInfoRepository.saveAppInfo(uacAppInfo2)

        uacCampaignSteps.createMobileAppCampaign(userInfo.clientInfo!!, accountId = accountId, appId = uacAppInfo1.id)
        uacCampaignSteps.createMobileAppCampaign(userInfo.clientInfo!!, accountId = accountId, appId = uacAppInfo2.id)
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun myAppsTest() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/app_info/my_apps")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val expectedAppInfos = listOf(uacAppInfo1, uacAppInfo2)
            .map { uacAppInfoService.getAppInfo(it) }
            .sortedBy { it.title?.lowercase() ?: "" }

        assertThat(JsonUtils.MAPPER.readTree(result)["result"]).isEqualTo(
            JsonUtils.MAPPER.readTree(
                JsonUtils.toJson(
                    expectedAppInfos
                )
            )
        )
    }
}
