package ru.yandex.direct.web.entity.uac.controller.tracking_url

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.asynchttpclient.Response
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.asynchttp.Result
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.trustedredirects.model.Opts
import ru.yandex.direct.core.entity.trustedredirects.model.RedirectType
import ru.yandex.direct.core.entity.trustedredirects.model.TrustedRedirects
import ru.yandex.direct.core.entity.trustedredirects.repository.TrustedRedirectsRepository
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService
import ru.yandex.direct.core.entity.zora.ZoraService
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rotor.client.RotorClient
import ru.yandex.direct.rotor.client.model.RotorJobResourceResponse
import ru.yandex.direct.rotor.client.model.RotorLogEventResponse
import ru.yandex.direct.rotor.client.model.RotorResponse
import ru.yandex.direct.rotor.client.model.RotorTraceResponse
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.zorafetcher.ZoraResponse

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacTrackingUrlControllerGorotorTest {
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var trustedRedirectsRepository: TrustedRedirectsRepository

    @Autowired
    private lateinit var trustedRedirectsService: TrustedRedirectsService

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var rotorTrackingUrlAndroidClient: RotorClient

    @Autowired
    private lateinit var zoraService: ZoraService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var userInfo: UserInfo
    private lateinit var request: Map<String, String>
    private lateinit var trackingUrl: String
    private lateinit var shortTrackingUrl: String

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        trackingUrl =
            "https://app.adjust.com/nnaes87?campaign={campaign_id}_{campaign_name_lat}&idfa={IDFA_UC}&gps_adid={GOOGLE_AID_LC}&campaign_id={campaign_id}&creative_id={ad_id}&publisher_id={gbid}&ya_click_id={TRACKID}"
        shortTrackingUrl =
            "https://app.adjust.com/nnaes87?campaign=&idfa=&gps_adid=&campaign_id=&creative_id=&publisher_id=&ya_click_id="
        listOf("clicks.bluetrackmedia.com", "play.google.com", "apps.apple.com").forEach {
            trustedRedirectsRepository.addTrustedDomain(
                TrustedRedirects()
                    .withDomain(it)
                    .withRedirectType(RedirectType.MOBILE_APP_COUNTER)
                    .withOpts(setOf(Opts.https_only))
            )
        }
        trustedRedirectsService.invalidateCache()

        Mockito.doReturn(
            RedirectCheckResult.createSuccessResult(
                "https://play.google.com/store/apps/details?id=net.idt.um.android.bossrevapp",
                ""
            )
        )
            .`when`(bannerUrlCheckService).getRedirect(
                Mockito.eq(shortTrackingUrl),
                Mockito.anyString(),
                ArgumentMatchers.anyBoolean()
            )

        val response = mapOf(
            "Url" to trackingUrl,
            "ResultUrl" to trackingUrl,
            "UrlTrace" to listOf(trackingUrl),
            "Trace" to jacksonObjectMapper().convertValue(mapOf(
                "LogEvents" to listOf(jacksonObjectMapper().convertValue(mapOf(
                    "JobResource" to jacksonObjectMapper().convertValue(mapOf(
                        "Url" to trackingUrl
                    ), RotorJobResourceResponse::class.java)
                ), RotorLogEventResponse::class.java), jacksonObjectMapper().convertValue(mapOf(
                    "JobResource" to jacksonObjectMapper().convertValue(mapOf(
                        "Url" to "https://play.google.com/store/apps/details?id=ru.auto.ara"
                    ), RotorJobResourceResponse::class.java)
                ), RotorLogEventResponse::class.java))
            ), RotorTraceResponse::class.java)
        )
        val rotorResponse = jacksonObjectMapper().convertValue(response, RotorResponse::class.java)
        Mockito.doReturn(rotorResponse)
            .`when`(rotorTrackingUrlAndroidClient).get(
                Mockito.eq(shortTrackingUrl),
            )
        Mockito.doReturn(zoraEmptyResponse())
            .`when`(zoraService).fetchByUrl(
                eq(shortTrackingUrl),
                any(),
                any()
            )
        request = mapOf(
            URL_STR to trackingUrl,
            VALIDATION_TYPE_STR to TRACKING_URL_STR,
            APP_ID_STR to "ru.auto.ara"
        )
    }

    @After
    fun after() {
        Mockito.reset(bannerUrlCheckService)
        Mockito.reset(rotorTrackingUrlAndroidClient)
        Mockito.reset(zoraService)
    }

    @Test
    fun test_WithoutFlag() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/validate")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun test_WithFlag() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.VALIDATE_TRACKING_URLS_GOROTOR, true)
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/validate")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    private fun zoraEmptyResponse(): Result<ZoraResponse> {
        val resp = mock<Response> {
        }
        val zoraResponse = mock<ZoraResponse> {
            on { isOk } doReturn true
            on { response } doReturn resp
        }
        val result = Result<ZoraResponse>(0)
        result.success = zoraResponse
        return result
    }
}
