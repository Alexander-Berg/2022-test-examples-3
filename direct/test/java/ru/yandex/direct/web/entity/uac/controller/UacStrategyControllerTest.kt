package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.model.TargetType
import ru.yandex.direct.core.entity.uac.model.UacStrategyName
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CounterType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.TrackerAppEvent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbTrackerUrlStatRepository
import ru.yandex.direct.core.testing.repository.UacYdbTrackerUrlStat
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.UacAvailableStrategyGoals
import ru.yandex.direct.web.entity.uac.model.UacAvailableStrategySettings
import java.time.LocalDateTime

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class
UacStrategyControllerTest {

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
    private lateinit var testUacYdbTrackerUrlStatRepository: TestUacYdbTrackerUrlStatRepository

    @Autowired
    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    @Autowired
    private lateinit var steps: Steps

    private lateinit var userInfo: UserInfo
    private lateinit var appInfo: UacYdbAppInfo

    companion object {
        val WITHOUT_CONVERSIONS: UacAvailableStrategyGoals = UacAvailableStrategyGoals(
            goals = mapOf(
                TargetType.INSTALL to setOf(UacStrategyName.AUTOBUDGET_AVG_CPI),
                TargetType.GOAL to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.LAUNCH to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.BASKET to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.WISH_LIST to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.REGISTER to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.TRAIN to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.ORDER to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.RATING to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.SEARCH to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.CREDITS to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.LEVELUP to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.WATCH to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.TIMESPENT to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.SHARE to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.CPC to setOf(UacStrategyName.AUTOBUDGET_AVG_CLICK),
                TargetType.PAYMENT to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.BUY to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.USER_1 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.USER_2 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.USER_3 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
            )
        )

        val WITH_CONVERSIONS = WITHOUT_CONVERSIONS.copy(
            goals = WITHOUT_CONVERSIONS.goals + mapOf(
                TargetType.PAYMENT to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA, UacStrategyName.AUTOBUDGET_CRR),
                TargetType.BUY to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA, UacStrategyName.AUTOBUDGET_CRR),
                TargetType.USER_1 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA, UacStrategyName.AUTOBUDGET_CRR),
                TargetType.USER_2 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA, UacStrategyName.AUTOBUDGET_CRR),
            )
        )

        val WITHOUT_CRR = WITHOUT_CONVERSIONS.copy(
            goals = WITHOUT_CONVERSIONS.goals + mapOf(
                TargetType.PAYMENT to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.BUY to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.USER_1 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
                TargetType.USER_2 to setOf(UacStrategyName.AUTOBUDGET_AVG_CPA),
            )
        )

        val SETTING_WITHOUT_FIX_CPA = mapOf(
            UacStrategyName.AUTOBUDGET_CRR to UacAvailableStrategySettings(setOf(false, true)),
            UacStrategyName.AUTOBUDGET_AVG_CPA to UacAvailableStrategySettings(setOf(false)),
            UacStrategyName.AUTOBUDGET_AVG_CPI to UacAvailableStrategySettings(setOf(false, true)),
            UacStrategyName.AUTOBUDGET_AVG_CLICK to UacAvailableStrategySettings(setOf(false))
        )

        val WITH_SKAD_NETWORK_ENABLED = UacAvailableStrategyGoals(
            mapOf(TargetType.CPC to setOf(UacStrategyName.AUTOBUDGET_AVG_CLICK)),
            mapOf(UacStrategyName.AUTOBUDGET_AVG_CLICK to UacAvailableStrategySettings(setOf(false)))
        )

        val TRACKER_URL_STAT = UacYdbTrackerUrlStat(
            updateTime = UacYdbUtils.toEpochSecond(LocalDateTime.now()),
            trackerUrl = "https://adidas.app.link/abcde",
            counterType = CounterType.CLICK,
            hitCount = 42,
            conversionsByEvent = mapOf(
                TrackerAppEvent.INSTALLED to 100,
                TrackerAppEvent.EVENT_1 to 100,
                TrackerAppEvent.EVENT_2 to 100,
                TrackerAppEvent.PURCHASED to 100
            )
        )
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        appInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)

        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_ROAS_STRATEGY, true)
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_FIX_CPA_STRATEGY_ENABLED, true)
    }

    @After
    fun after() {
        testUacYdbTrackerUrlStatRepository.clean()
    }

    @Test
    fun testWithoutTrackerUrl() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy?app_info_id=${appInfo.id}")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val realDefect = JsonUtils.fromJson(result)["validation_result"]["errors"][0]["code"]
            .asText()
            .split(".")
            .last()
        Assertions.assertThat(realDefect).isEqualTo("INVALID_TRACKING_URL")
    }

    @Test
    fun testTrackingUrlNotFound() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy?app_info_id=com.some.app}")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val realDefect = JsonUtils.fromJson(result)["validation_result"]["errors"][0]["code"]
            .asText()
            .split(".")
            .last()
        Assertions.assertThat(realDefect).isEqualTo("INVALID_TRACKING_URL")
    }

    @Test
    fun testWithoutAppInfo() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy?url=https://adidas.app.link/abcde")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val realDefect = JsonUtils.fromJson(result)["validation_result"]["errors"][0]["code"]
            .asText()
            .split(".")
            .last()
        Assertions.assertThat(realDefect).isEqualTo("APP_INFO_NOT_FOUND")
    }

    @Test
    fun testWithConversions() {
        Mockito.doReturn(
            listOf(
                MobileGoalConversions(TargetType.PAYMENT.goalId!!, 0, 100, 100),
                MobileGoalConversions(TargetType.BUY.goalId!!, 0, 100, 100),
                MobileGoalConversions(TargetType.USER_1.goalId!!, 0, 100, 100),
                MobileGoalConversions(TargetType.USER_2.goalId!!, 0, 100, 100)
            )
        ).`when`(mobileAppConversionStatisticRepository).getConversionStats(
            "com.yandex.some","android", TargetType.values().mapNotNull { it.goalId },30
        )
        testUacYdbTrackerUrlStatRepository.insertStat(TRACKER_URL_STAT)
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy?url=https://adidas.app.link/abcde&app_info_id=${appInfo.id}")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        val result = JsonUtils.MAPPER.readTree(response)["result"]
        Assertions.assertThat(result).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(WITH_CONVERSIONS))
        )
    }

    @Test
    fun testWithoutConversions() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val result = JsonUtils.MAPPER.readTree(response)["result"]
        Assertions.assertThat(result).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(WITHOUT_CONVERSIONS))
        )
    }

    @Test
    fun testWithSkadNetworkEnabled() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy?isSkadNetworkEnabled=true")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val result = JsonUtils.MAPPER.readTree(response)["result"]
        Assertions.assertThat(result).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(WITH_SKAD_NETWORK_ENABLED))
        )
    }

    @Test
    fun testWithoutCrrEnabled() {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_ROAS_STRATEGY, false)
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val result = JsonUtils.MAPPER.readTree(response)["result"]
        Assertions.assertThat(result).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(WITHOUT_CRR))
        )
    }

    @Test
    fun testWithoutFixCpaEnabled() {
        steps.featureSteps()
            .addClientFeature(userInfo.clientId, FeatureName.UAC_FIX_CPA_STRATEGY_ENABLED, false)
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/strategy")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val result = JsonUtils.MAPPER.readTree(response)["result"]["strategies_data_settings"]
        Assertions.assertThat(result).isEqualTo(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(SETTING_WITHOUT_FIX_CPA))
        )
    }
}
