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
class UacTrackingUrlControllerFlurryTest(
    private val url: String,
    private val expectedUrl: String,
    private val expectedTrackerId: String
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
            "system" to "FLURRY",
            "parameters" to FLURRY_PARAMS,
            "has_impression" to false,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "https://ad.apps.fm/t0MOILM-olslPmrs8T?reqid={logid}&event-link-type=install&tracker=flurry",
                "https://ad.apps.fm/t0MOILM-olslPmrs8T?event-link-type=install&tracker=flurry&reqid={logid}",
                "t0MOILM-olslPmrs8T"
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

