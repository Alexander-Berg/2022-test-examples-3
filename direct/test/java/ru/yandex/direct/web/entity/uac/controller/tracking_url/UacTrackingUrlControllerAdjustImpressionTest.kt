package ru.yandex.direct.web.entity.uac.controller.tracking_url

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingUrlControllerAdjustImpressionTest {
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
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    private lateinit var userInfo: UserInfo

    companion object {

        private val baseExpected = mapOf(
            "skad_network_integrated" to true,
            "system" to "ADJUST",
            "parameters" to ADJUST_IMPRESSION_PARAMS,
            "has_impression" to false,
            "tracker_id" to "xyz4567"
        )
    }

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun testWithoutApp() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR
            ),
            baseExpected + mapOf(
                "url" to "https://view.adjust.com/impression/xyz4567?gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}",
            )
        )
    }

    @Test
    fun testWithAndroidAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "com.yandex.some"
            ),
            baseExpected + mapOf(
                "url" to "https://view.adjust.com/impression/xyz4567?idfa={IDFA_LC}&gps_adid={google_aid}&oaid={oaid}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testWithAndroidAppInfoId() {
        val appInfo = defaultAppInfo(
            appId = "com.yandex.any", bundleId = "com.yandex.any",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://view.adjust.com/impression/xyz4567?idfa={IDFA_LC}&gps_adid={google_aid}&oaid={oaid}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testWithIosAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "id123"
            ),
            baseExpected + mapOf(
                "url" to "https://view.adjust.com/impression/xyz4567?gps_adid={GOOGLE_AID_LC}&idfa={ios_ifa}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}"
            )
        )
    }

    @Test
    fun testWithIosAppInfoId() {
        val appInfo = defaultAppInfo(
            appId = "id1234", bundleId = "com.yandex.yetanother",
            platform = Platform.IOS, source = Store.ITUNES,
            data = IOS_APP_INFO_DATA
        )
        uacAppInfoRepository.saveAppInfo(appInfo)
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to "https://view.adjust.com/impression/xyz4567?ya_click_id={TRACKID}&gps_adid={GOOGLE_AID_LC}&idfa={IDFA_LC}",
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://view.adjust.com/impression/xyz4567?gps_adid={GOOGLE_AID_LC}&idfa={ios_ifa}&ya_click_id={logid}&user_agent={user_agent}&ip_address={client_ip}&language={device_lang}"
            )
        )
        testUacYdbAppInfoRepository.clean()
    }
}
