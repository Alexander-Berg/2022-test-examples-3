package ru.yandex.direct.web.entity.uac.controller.tracking_url

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
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
class UacTrackingUrlControllerAdjustTest(
    private val url: String,
    private val appId: String,
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

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    private lateinit var userInfo: UserInfo

    companion object {

        private val baseExpected = mapOf(
            "skad_network_integrated" to true,
            "system" to "ADJUST",
            "parameters" to ADJUST_PARAMS,
            "has_impression" to true,
        )

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                "https://app.adjust.com/nnaes87?campaign={campaign_id}_{campaign_name_lat}&idfa={IDFA_UC}&gps_adid={GOOGLE_AID_LC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&ya_click_id={TRACKID}",
                "net.idt.um.android.bossrevapp",
                "https://app.adjust.com/nnaes87?campaign={campaign_id}_{campaign_name_lat}&idfa={IDFA_UC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&gps_adid={google_aid}&oaid={oaid}&ya_click_id={logid}",
                "nnaes87"
            ),
            arrayOf(
                "https://app.adjust.com/nnaes88?campaign={campaign_id}_{campaign_name_lat}&idfa={IDFA_UC}&gps_adid={GOOGLE_AID_LC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&ya_click_id={TRACKID}",
                "id1185435679",
                "https://app.adjust.com/nnaes88?campaign={campaign_id}_{campaign_name_lat}&gps_adid={GOOGLE_AID_LC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&idfa={ios_ifa}&ya_click_id={logid}",
                "nnaes88"
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
        Mockito.doReturn(RedirectCheckResult.createSuccessResult("https://play.google.com/store/apps/details?id=net.idt.um.android.bossrevapp&referrer=utm_source%3Dko_acec604b607d0f857%26utm_medium%3Dp277-s322-%26utm_campaign%3Dkoidtandroidus205652e7fe894452f471b27363e9ea%26utm_term%3D%26utm_content%3D%26", ""))
            .`when`(bannerUrlCheckService).getRedirect(
                Mockito.eq("https://app.adjust.com/nnaes87?campaign=&idfa=&gps_adid=&campaign_id=&creative_id=&publisher_id=&ya_click_id="),
                Mockito.anyString(),
                ArgumentMatchers.anyBoolean()
            )
        Mockito.doReturn(RedirectCheckResult.createSuccessResult("https://apps.apple.com/ru/app/id1185435679?mt=8", ""))
            .`when`(bannerUrlCheckService).getRedirect(
                Mockito.eq("https://app.adjust.com/nnaes88?campaign=&idfa=&gps_adid=&campaign_id=&creative_id=&publisher_id=&ya_click_id="),
                Mockito.anyString(),
                ArgumentMatchers.anyBoolean()
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

