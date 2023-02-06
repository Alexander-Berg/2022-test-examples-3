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
class UacTrackingUrlControllerAppsflyerImpressionTest {

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

        private const val APPSFLYER_IMPRESSION_URL =
            "https://impression.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&af_c_id={campaign_id}&af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&clickid={TRACKID}&advertising_id={GOOGLE_AID_LC}&oaid={OAID_UC}&idfa={IDFA_UC}"

        private val baseExpected = mapOf(
            "skad_network_integrated" to true,
            "system" to "APPSFLYER",
            "parameters" to APPSFLYER_IMPRESSION_PARAMS,
            "has_impression" to false,
            "tracker_id" to "com.im30.ROE.gp"
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
                URL_STR to APPSFLYER_IMPRESSION_URL,
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR
            ),
            baseExpected + mapOf(
                "url" to "https://impression.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&advertising_id={GOOGLE_AID_LC}&oaid={OAID_UC}&idfa={IDFA_UC}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
            )
        )
    }

    @Test
    fun testWithAndroidAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to APPSFLYER_IMPRESSION_URL,
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "com.yandex.some"
            ),
            baseExpected + mapOf(
                "url" to "https://impression.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&idfa={IDFA_UC}&advertising_id={google_aid}&oaid={oaid}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
            )
        )
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
                URL_STR to APPSFLYER_IMPRESSION_URL,
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://impression.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&idfa={IDFA_UC}&advertising_id={google_aid}&oaid={oaid}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
            )
        )
        testUacYdbAppInfoRepository.clean()
    }

    @Test
    fun testWithIosAppId() {
        checkCorrectRequest(
            mockMvc,
            mapOf(
                URL_STR to APPSFLYER_IMPRESSION_URL,
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_ID_STR to "id123"
            ),
            baseExpected + mapOf(
                "url" to "https://impression.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&advertising_id={GOOGLE_AID_LC}&oaid={OAID_UC}&idfa={ios_ifa}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
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
                URL_STR to APPSFLYER_IMPRESSION_URL,
                VALIDATION_TYPE_STR to IMPRESSION_URL_STR,
                APP_INFO_ID_STR to appInfo.id
            ),
            baseExpected + mapOf(
                "url" to "https://impression.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&advertising_id={GOOGLE_AID_LC}&oaid={OAID_UC}&idfa={ios_ifa}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}&af_ip={client_ip}&af_ua={user_agent}&af_lang={device_lang}",
            )
        )
        testUacYdbAppInfoRepository.clean()
    }
}
