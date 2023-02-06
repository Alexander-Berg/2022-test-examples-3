package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
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
import ru.yandex.direct.core.entity.mobileapp.model.MobileConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.CounterType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.TrackerAppEvent
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.core.testing.repository.TestUacYdbTrackerUrlStatRepository
import ru.yandex.direct.core.testing.repository.UacYdbTrackerUrlStat
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingUrlStatControllerTest {

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
    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    @Autowired
    private lateinit var testUacYdbTrackerUrlStatRepository: TestUacYdbTrackerUrlStatRepository

    private lateinit var userInfo: UserInfo
    private lateinit var appInfo: UacYdbAppInfo

    companion object {

        val EMPTY_ANS: Map<String, Any> = mapOf(
            "any_url_events" to "undefined",
            "url_events" to mapOf(
                "achieved_level" to "undefined",
                "added_payment_info" to "undefined",
                "added_to_cart" to "undefined",
                "added_to_wishlist" to "undefined",
                "app_launched" to "undefined",
                "completed_registration" to "undefined",
                "completed_tutorial" to "undefined",
                "custom_event" to "undefined",
                "event_1" to "undefined",
                "event_2" to "undefined",
                "event_3" to "undefined",
                "initiated_checkout" to "undefined",
                "installed" to "undefined",
                "purchased" to "undefined",
                "rated" to "undefined",
                "removed_from_cart" to "undefined",
                "searched" to "undefined",
                "shared" to "undefined",
                "spent_credits" to "undefined",
                "spent_time_in_app" to "undefined",
                "unlocked_achievement" to "undefined",
                "viewed_content" to "undefined"
            ),
            "any_package_events" to "undefined",
            "all_package_events" to "no",
        )
        val FULL_ANS: Map<String, Any> = mapOf(
            "any_url_events" to "yes",
            "url_events" to mapOf(
                "achieved_level" to "no",
                "added_payment_info" to "no",
                "added_to_cart" to "no",
                "added_to_wishlist" to "no",
                "app_launched" to "no",
                "completed_registration" to "no",
                "completed_tutorial" to "no",
                "custom_event" to "no",
                "event_1" to "no",
                "event_2" to "no",
                "event_3" to "no",
                "initiated_checkout" to "no",
                "installed" to "yes",
                "purchased" to "no",
                "rated" to "no",
                "removed_from_cart" to "no",
                "searched" to "no",
                "shared" to "no",
                "spent_credits" to "no",
                "spent_time_in_app" to "no",
                "unlocked_achievement" to "no",
                "viewed_content" to "no"
            ),
            "any_package_events" to "yes",
            "all_package_events" to "yes",
        )

        val APPLICABLE_UAC_YDB_TRACKER_URL_STAT = UacYdbTrackerUrlStat(
            updateTime = UacYdbUtils.toEpochSecond(LocalDateTime.now()),
            trackerUrl = "https://adidas.app.link/abcde",
            counterType = CounterType.CLICK,
            hitCount = 42,
            conversionsByEvent = mapOf(TrackerAppEvent.INSTALLED to 10)
        )

        val NOT_APPLICABLE_UAC_YDB_TRACKER_URL_STAT = UacYdbTrackerUrlStat(
            updateTime = 0,
            trackerUrl = "https://adidas.app.link/abcde",
            counterType = CounterType.CLICK,
            hitCount = 228,
            conversionsByEvent = mapOf(TrackerAppEvent.VIEWED_CONTENT to 5)
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
    }

    @After
    fun after() {
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testInvalidTrackingUrl() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_url/stat?url=https://example.com&app_info_id=${appInfo.id}")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val realDefect = JsonUtils.fromJson(result)["validation_result"]["errors"][0]["code"].asText()
        realDefect.split('.').last().checkEquals("INVALID_TRACKING_URL")
    }

    @Test
    fun testAppInfoNotFound() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_url/stat?url=https://adidas.app.link/abcde&app_info_id=12345")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString
        val realDefect = JsonUtils.fromJson(result)["validation_result"]["errors"][0]["code"].asText()
        realDefect.split('.').last().checkEquals("APP_INFO_NOT_FOUND")
    }

    @Test
    fun testEmptyStat() {
        Mockito.doReturn(null).`when`(mobileAppConversionStatisticRepository).getCommonConversionStats(
            "com.yandex.some","android", listOf(53),90
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_url/stat?url=https://adidas.app.link/abcde&app_info_id=${appInfo.id}")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        JsonUtils.MAPPER.readTree(result)["result"].checkEquals(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(EMPTY_ANS))
        )
    }

    @Test
    fun test() {
        testUacYdbTrackerUrlStatRepository.insertStat(APPLICABLE_UAC_YDB_TRACKER_URL_STAT)
        testUacYdbTrackerUrlStatRepository.insertStat(NOT_APPLICABLE_UAC_YDB_TRACKER_URL_STAT)
        Mockito.doReturn(
            MobileConversions(10, 20)
        ).`when`(mobileAppConversionStatisticRepository).getCommonConversionStats(
            "com.yandex.some","android", listOf(53),90
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/tracking_url/stat?url=https://adidas.app.link/abcde&app_info_id=${appInfo.id}")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        JsonUtils.MAPPER.readTree(result)["result"].checkEquals(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(FULL_ANS))
        )

        testUacYdbTrackerUrlStatRepository.clean()
    }

    @Test
    fun testImpressionUrl() {
        Mockito.doReturn(null).`when`(mobileAppConversionStatisticRepository).getCommonConversionStats(
            "com.yandex.some", "android", listOf(4), 90
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/tracking_url/stat?url=https://impression.appsflyer.com/abcde&app_info_id=${appInfo.id}"
                )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        JsonUtils.MAPPER.readTree(result)["result"].checkEquals(
            JsonUtils.MAPPER.readTree(JsonUtils.toJson(EMPTY_ANS))
        )
    }
}
