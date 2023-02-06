package ru.yandex.direct.web.entity.uac.controller.tracking_url

import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(Parameterized::class)
class UacTrackingUrlControllerBranchTest(
    private val url: String,
    private val appId: String,
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

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    private lateinit var userInfo: UserInfo

    companion object {

        private val baseExpected = mapOf(
            "skad_network_integrated" to false,
            "system" to "BRANCH",
            "parameters" to BRANCH_PARAMS,
            "has_impression" to false,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "https://adidas.app.link/abcde",
                "net.idt.um.android.bossrevapp",
                "https://adidas.app.link/abcde?%24aaid={google_aid}&~click_id={logid}&%243p=a_yandex_direct",
                "abcde"
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
        whenever(bannerUrlCheckService.getRedirect(anyString(), anyString(), anyBoolean()))
            .thenReturn(
                RedirectCheckResult.createSuccessResult(
                    "https://play.google.com/store/apps/details?id=net.idt.um.android.bossrevapp", ""
                )
            )
    }

    @Test
    fun testTrackingUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to TRACKING_URL_STR, APP_ID_STR to appId),
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId)
        )
    }

    @Test
    fun testRedirectUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to REDIRECT_URL_STR, APP_ID_STR to appId),
            baseExpected + mapOf("url" to expectedUrl, "tracker_id" to expectedTrackerId)
        )
    }
}
