package ru.yandex.direct.web.entity.uac.controller.tracking_url

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(Parameterized::class)
class UacTrackingUrlControllerAppsflyerTest(
    private val url: String,
    private val expectedUrl: String,
    private val expectedTrackerId: String,
    private val expectedSystem: String,
) {
    @get:Rule
    var springMethodRule = SpringMethodRule()

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    private lateinit var userInfo: UserInfo

    companion object {

        private val baseExpected = mapOf(
            "skad_network_integrated" to true,
            "parameters" to APPSFLYER_PARAMS,
            "has_impression" to true,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "http://app.appsflyer.com/com.greatapp?pid=chartboost_int&c=christmas_sale&af_adset_id=54822",
                "http://app.appsflyer.com/com.greatapp?c=christmas_sale&af_adset_id=54822&pid=yandexdirect_int&clickid={logid}&af_c_id={campaign_id}",
                "com.greatapp",
                "APPSFLYER"
            ),
            arrayOf(
                "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&af_c_id={campaign_id}&af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&clickid={logid}&google_aid={googleaid}&android_id={androidid}&ya_click_id={logid}",
                "https://app.appsflyer.com/com.im30.ROE.gp?af_ad={gbid}&af_sub1={ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}&af_click_lookback=7d&google_aid={googleaid}&android_id={androidid}&ya_click_id={logid}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}",
                "com.im30.ROE.gp",
                "APPSFLYER"
            ),
            arrayOf(
                 "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&clickid={logid}&idfa={ios_ifa}",
                 "https://app.appsflyer.com/com.im30.ROE.gp?idfa={ios_ifa}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}",
                 "com.im30.ROE.gp",
                 "APPSFLYER"
            ),
            arrayOf(
                "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&clickid={logid}&idfa={ios_ifa}&c=",
                "https://app.appsflyer.com/com.im30.ROE.gp?idfa={ios_ifa}&pid=yandexdirect_int&clickid={logid}&c={campaign_name}&af_c_id={campaign_id}",
                "com.im30.ROE.gp",
                "APPSFLYER"
            ),
            arrayOf(
                "https://app.appsflyer.com/com.im30.ROE.gp?pid=yandexdirect_int&clickid={logid}&idfa={ios_ifa}&c=whatever",
                "https://app.appsflyer.com/com.im30.ROE.gp?idfa={ios_ifa}&c=whatever&pid=yandexdirect_int&clickid={logid}&af_c_id={campaign_id}",
                "com.im30.ROE.gp",
                "APPSFLYER"
            ),
            arrayOf(
                "https://kupivip.onelink.me/305004079?idfa={ios_ifa}&c=kupivip",
                "https://kupivip.onelink.me/305004079?idfa={ios_ifa}&c=kupivip&pid=yandexdirect_int&clickid={logid}&af_c_id={campaign_id}",
                "305004079",
                "APPSFLYERUL"
            ),
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
    fun testTrackingUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to TRACKING_URL_STR),
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId, "system" to expectedSystem)
        )
    }

    @Test
    fun testRedirectUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to REDIRECT_URL_STR),
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId, "system" to expectedSystem)
        )
    }
}



