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
class UacTrackingUrlControllerTuneTest(
    private val url: String,
    private val expectedUrl: String,
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
            "system" to "TUNE",
            "parameters" to TUNE_PARAMS,
            "has_impression" to false,
            "tracker_id" to null
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "http://0nz-d.tlnk.io/serve?action=click&publisher_id=347530&site_id=109914&my_campaign=ym&my_keyword={keyword}&my_placement={source}&my_site={position_type}&my_ad={ad_id}",
                "http://0nz-d.tlnk.io/serve?action=click&publisher_id=347530&site_id=109914&my_campaign=ym&my_keyword={keyword}&my_placement={source}&my_site={position_type}&my_ad={ad_id}&publisher_ref_id={logid}",

            ),
            arrayOf(
                "http://336140.measurementapi.com/serve?action=click&publisher_id=336140&site_id=129211&destination_id=438087&my_site={source}&my_ad={ad_id}&my_keyword={keyword}&publisher_ref_id={logid}",
                "http://336140.measurementapi.com/serve?action=click&publisher_id=336140&site_id=129211&destination_id=438087&my_site={source}&my_ad={ad_id}&my_keyword={keyword}&publisher_ref_id={logid}",
            ),
            arrayOf(
                "http://hastrk3.com/pub_c?adgroup_id=3601",
                "http://hastrk3.com/pub_c?adgroup_id=3601&publisher_ref_id={logid}",
            ),
            arrayOf(
                "http://147344.api-01.com/serve?action=click&publisher_id=147344&site_id=15130&my_campaign=fuwenyi",
                "http://147344.api-01.com/serve?action=click&publisher_id=147344&site_id=15130&my_campaign=fuwenyi&publisher_ref_id={logid}",
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
            baseExpected + mapOf("url" to expectedUrl)
        )
    }

    @Test
    fun testRedirectUrl() {
        checkCorrectRequest(
            mockMvc,
            mapOf(URL_STR to url, VALIDATION_TYPE_STR to REDIRECT_URL_STR),
            baseExpected + mapOf("url" to expectedUrl)
        )
    }
}
