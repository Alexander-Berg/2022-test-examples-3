package ru.yandex.direct.web.entity.uac.controller.tracking_url

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.asynchttpclient.Response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringMethodRule
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
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.zorafetcher.ZoraResponse

@DirectWebTest
@RunWith(Parameterized::class)
class UacTrackingUrlControllerInvalidTest(
    private val request: Map<String, String?>,
    private val defectName: String?
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
    private lateinit var trustedRedirectsRepository: TrustedRedirectsRepository

    @Autowired
    private lateinit var trustedRedirectsService: TrustedRedirectsService

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var zoraService: ZoraService

    private lateinit var userInfo: UserInfo

    companion object {

        private const val INVALID_TRACKING_URL_STR = "INVALID_TRACKING_URL"
        private const val INVALID_IMPRESSION_URL_STR = "INVALID_IMPRESSION_URL"
        private const val TRACKING_URL_WRONG_REDIRECT_STR = "TRACKING_URL_WRONG_REDIRECT"

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = arrayOf(
            arrayOf(
                mapOf(URL_STR to "http://www.google.com/", VALIDATION_TYPE_STR to TRACKING_URL_STR),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "http://www.google.com/", VALIDATION_TYPE_STR to IMPRESSION_URL_STR),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "http://www.google.com/", VALIDATION_TYPE_STR to REDIRECT_URL_STR),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),

            arrayOf(
                mapOf(URL_STR to "https://www.google.com/", VALIDATION_TYPE_STR to TRACKING_URL_STR),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "https://www.google.com/", VALIDATION_TYPE_STR to IMPRESSION_URL_STR),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "https://www.google.com/", VALIDATION_TYPE_STR to REDIRECT_URL_STR),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),

            arrayOf(
                mapOf(URL_STR to "http://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to TRACKING_URL_STR),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "http://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to IMPRESSION_URL_STR),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "http://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to REDIRECT_URL_STR),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),

            arrayOf(
                mapOf(URL_STR to "https://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to TRACKING_URL_STR),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "https://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to IMPRESSION_URL_STR),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(URL_STR to "https://clicks.bluetrackmedia.com/", VALIDATION_TYPE_STR to REDIRECT_URL_STR),
                null
            ),

            arrayOf(
                mapOf(
                    URL_STR to "http://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to TRACKING_URL_STR
                ),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "http://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to IMPRESSION_URL_STR
                ),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "http://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR
                ),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),

            arrayOf(
                mapOf(
                    URL_STR to "https://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to TRACKING_URL_STR
                ),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to IMPRESSION_URL_STR
                ),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR
                ),
                null
            ),

            arrayOf(
                mapOf(
                    URL_STR to "https://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR,
                    APP_ID_STR to "ru.yandex"
                ),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://play.google.com/store/apps/details?id=ru.auto.ara",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR,
                    APP_ID_STR to "ru.auto.ara"
                ),
                null
            ),

            arrayOf(
                mapOf(
                    URL_STR to "http://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to TRACKING_URL_STR
                ),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "http://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to IMPRESSION_URL_STR
                ),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "http://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR
                ),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),

            arrayOf(
                mapOf(
                    URL_STR to "https://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to TRACKING_URL_STR
                ),
                INVALID_TRACKING_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to IMPRESSION_URL_STR
                ),
                INVALID_IMPRESSION_URL_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR
                ),
                null
            ),

            arrayOf(
                mapOf(
                    URL_STR to "https://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR,
                    APP_ID_STR to "id1234567890"
                ),
                TRACKING_URL_WRONG_REDIRECT_STR
            ),
            arrayOf(
                mapOf(
                    URL_STR to "https://apps.apple.com/ru/app/id1185435679",
                    VALIDATION_TYPE_STR to REDIRECT_URL_STR,
                    APP_ID_STR to "id1185435679"
                ),
                null
            ),
        )
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

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
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
                "https://play.google.com/store/apps/details?id=ru.auto.ara",
                ""
            )
        )
            .`when`(bannerUrlCheckService).getRedirect(
                eq("https://play.google.com/store/apps/details?id=ru.auto.ara"),
                anyOrNull(),
                anyBoolean(),
            )
        Mockito.doReturn(RedirectCheckResult.createSuccessResult("https://apps.apple.com/ru/app/id1185435679", ""))
            .`when`(bannerUrlCheckService).getRedirect(
                eq("https://apps.apple.com/ru/app/id1185435679"),
                anyOrNull(),
                anyBoolean(),
            )
        Mockito.doReturn(zoraEmptyResponse())
            .`when`(zoraService).fetchByUrl(
                eq("https://play.google.com/store/apps/details?id=ru.auto.ara"),
                any(),
                any()
            )
        Mockito.doReturn(zoraEmptyResponse())
            .`when`(zoraService).fetchByUrl(
                eq("https://apps.apple.com/ru/app/id1185435679"),
                any(),
                any()
            )
    }

    @Test
    fun testInvalid() {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/tracking_url/validate")
                .content(JsonUtils.toJson(request))
                .contentType(MediaType.APPLICATION_JSON)
        )
        if (defectName != null) {
            val resultContent = result.andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()
                .response
                .contentAsString
            val realDefect = JsonUtils.fromJson(resultContent)["validation_result"]["errors"][0]["code"].asText()
            realDefect.split('.').last().checkEquals(defectName)
        } else {
            val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response
                .contentAsString
            JsonUtils.fromJson(resultContent)["result"].isNull.checkEquals(true)
        }
    }
}
