package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
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
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacRetargetingGoalsControllerTest {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppGoalsService: MobileAppGoalsService

    @Autowired
    private lateinit var uacAppInfoRepository: UacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo
    private lateinit var mockMvc: MockMvc

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun testGetWithOneApp() {
        testGet(appsCount = 1)
    }

    @Test
    fun testGetWithTwoApps() {
        testGet(appsCount = 2)
    }

    @Test
    fun testWithoutAppInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/goals?app_info.id=11111&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun testWithoutMobileApp() {
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/goals?app_info.id=${ydbAppInfo.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun testWithEmptyGoals() {
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApp = steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp
        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, listOf(mobileApp))

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/goals?app_info.id=${ydbAppInfo.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = JsonUtils.MAPPER.readTree(result)["result"]

        Assertions.assertThat(response["mobile_app_id"].asLong()).isEqualTo(mobileApp.id)
        Assertions.assertThat(response["goals"]).hasSize(0)
    }

    private fun testGet(appsCount: Int) {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.IN_APP_MOBILE_TARGETING, true)
        val ydbAppInfo = defaultAppInfo()
        uacAppInfoRepository.saveAppInfo(ydbAppInfo)
        val appInfo = uacAppInfoService.getAppInfo(ydbAppInfo)

        val mobileApps: MutableList<MobileApp> = mutableListOf()
        for (i in 0 until appsCount) {
            mobileApps.add(steps.mobileAppSteps().createMobileApp(userInfo.clientInfo!!, appInfo.url).mobileApp)
        }

        mobileAppGoalsService.updateMobileAppGoalsForExternalTracker(
            dslContextProvider.ppc(userInfo.shard), userInfo.clientId, mobileApps)

        val mobileAppGoalIds =  mobileAppGoalsService.getGoalsByApps(userInfo.clientId, mobileApps)
            .sortedBy { it.id }
            .map { it.id }

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/goals?app_info.id=${ydbAppInfo.id}&ulogin=" + userInfo.clientInfo!!.login)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = JsonUtils.MAPPER.readTree(result)["result"]
        val soft = SoftAssertions()
        soft.assertThat(response["mobile_app_id"].asLong()).isEqualTo(mobileApps[0].id)
        soft.assertThat(response["goals"].size()).isEqualTo(ExternalTrackerEventName.values().size * mobileApps.size)
        for (i in 0 until response["goals"].size()) {
            soft.assertThat(response["goals"][i]["goal_id"].asLong()).isEqualTo(mobileAppGoalIds[i])
            soft.assertThat(response["goals"][i]["goal_name"].asText()).isEqualTo(ExternalTrackerEventName.values()[i % 20].name)
        }
        soft.assertAll()
    }
}
