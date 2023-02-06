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
class UacTrackingUrlControllerMyTrackerTest(
    private val url: String,
    private val expectedUrl: String,
    private val expectedTrackerId: String,
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
            "skad_network_integrated" to false,
            "system" to "MAIL_RU",
            "parameters" to MY_TRACKER_PARAMS,
            "has_impression" to true,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "https://trk.mail.ru/c/smt7g7?_1lpb_id=4702&_1lpb_clickId={logid}&mt_gaid={google_aid}&mt_sub1={banner_id}&mt_sub2={phrase_id}&mt_sub3={keyword}&mt_sub4={position_type}&mt_sub5={region_id}&mt_network={source_type}&mt_campaign={campaign_id}&mt_adset={gbid}&mt_creative={ad_id}",
                "https://trk.mail.ru/c/smt7g7?_1lpb_id=4702&_1lpb_clickId={logid}&mt_gaid={google_aid}&mt_sub1={banner_id}&mt_sub2={phrase_id}&mt_sub3={keyword}&mt_sub4={position_type}&mt_sub5={region_id}&mt_network={source_type}&mt_campaign={campaign_id}&mt_adset={gbid}&mt_creative={ad_id}&clickId={logid}&regid={logid}",
                "smt7g7"
            ),
            arrayOf(
                "https://trk.mail.ru/c/a12345?clickId={logid}",
                "https://trk.mail.ru/c/a12345?clickId={logid}&regid={logid}",
                "a12345"
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
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId)
        )
    }

    @Test
    fun testRedirectUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to REDIRECT_URL_STR),
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId)
        )
    }
}
