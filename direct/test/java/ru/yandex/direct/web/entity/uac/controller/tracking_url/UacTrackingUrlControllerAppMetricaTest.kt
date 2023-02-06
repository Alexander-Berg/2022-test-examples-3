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
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(Parameterized::class)
class UacTrackingUrlControllerAppMetricaTest(
    private val url: String,
    private val expectedUrl: String,
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
            "parameters" to APP_METRICA_PARAMS,
            "has_impression" to false,
            "tracker_id" to "123"
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "https://redirect.appmetrica.yandex.com/serve/123/?click_id={logid}",
                "https://redirect.appmetrica.yandex.com/serve/123/?click_id={logid}",
                "APPMETRICA"
            ),
            arrayOf(
                "https://1.redirect.appmetrica.yandex.ru/?appmetrica_tracking_id=123",
                "https://1.redirect.appmetrica.yandex.ru/?appmetrica_tracking_id=123&click_id={logid}",
                "APPMETRICAUL"
            ),
            arrayOf(
                "https://12468.redirect.appmetrica.yandex.com/?click_id={logid}&appmetrica_tracking_id=123",
                "https://12468.redirect.appmetrica.yandex.com/?appmetrica_tracking_id=123&click_id={logid}",
                "APPMETRICAUL"
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
            baseExpected + mapOf("url" to expectedUrl, "system" to expectedSystem)
        )
    }

    @Test
    fun testRedirectUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to REDIRECT_URL_STR),
            baseExpected + mapOf("url" to expectedUrl, "system" to expectedSystem)
        )
    }
}

