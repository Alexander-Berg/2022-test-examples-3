package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import java.net.IDN
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.MARKETPLACE_DOMAINS
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.uac.APP_ID
import ru.yandex.direct.core.entity.uac.GOOGLE_PLAY_BAD_URL
import ru.yandex.direct.core.entity.uac.ITUNES_BAD_URL
import ru.yandex.direct.core.entity.uac.MARKET_ACCOUNT_URL
import ru.yandex.direct.core.entity.uac.MARKET_BUSINESS_SHOP_URL
import ru.yandex.direct.core.entity.uac.MARKET_DOMAIN
import ru.yandex.direct.core.entity.uac.MARKET_SHOP_URL
import ru.yandex.direct.core.entity.uac.MARKET_STORE_SHOP_URL
import ru.yandex.direct.core.entity.uac.OZON_DOMAIN
import ru.yandex.direct.core.entity.uac.OZON_SHOP_URL
import ru.yandex.direct.core.entity.uac.STORE_URL
import ru.yandex.direct.core.entity.uac.STORE_URL_FOR_APP_ID
import ru.yandex.direct.core.entity.uac.TRACKING_URL_WITH_UNICODE
import ru.yandex.direct.core.entity.uac.TRACKING_URL_WITH_UNICODE_ENCODED
import ru.yandex.direct.core.entity.uac.VALID_APP_ID
import ru.yandex.direct.core.entity.uac.VALID_REDIRECT_URL
import ru.yandex.direct.core.entity.uac.VALID_TRACKING_URL
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.ShopInShopBusiness
import ru.yandex.direct.core.entity.uac.model.Source
import ru.yandex.direct.core.entity.uac.repository.mysql.ShopInShopBusinessesRepository
import ru.yandex.direct.core.service.urlchecker.RedirectCheckResult
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.rotor.client.RotorClient
import ru.yandex.direct.rotor.client.model.RotorJobResourceResponse
import ru.yandex.direct.rotor.client.model.RotorLogEventResponse
import ru.yandex.direct.rotor.client.model.RotorResponse
import ru.yandex.direct.rotor.client.model.RotorTraceResponse
import ru.yandex.direct.utils.JsonUtils.fromJsonIgnoringErrors
import ru.yandex.direct.utils.UrlUtils
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.ValidationResult
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.entity.uac.model.links.LinkInfo

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacLinkControllerTest : BaseMvcTest() {

    @Autowired
    private lateinit var bannerUrlCheckService: BannerUrlCheckService

    @Autowired
    private lateinit var geminiClient: GeminiClient

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var rotorTrackingUrlIosClient: RotorClient

    @Autowired
    private lateinit var shopInShopBusinessesRepository: ShopInShopBusinessesRepository

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = steps.clientSteps().createDefaultClient()
        steps.campaignSteps().createWalletCampaign(clientInfo)

        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        testAuthHelper.setSecurityContext()
    }

    @Test
    fun getLinkInfo_regularOrdinaryWebLink() {
        val url = "https://some-site.com/"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(false))
    }

    @Test
    fun getLinkInfo_regularOrdinaryWebLinkWithUnicodeInQueryPart() {
        val url = "https://some-site.com/?param1=hello, world!"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(false))
    }

    @Test
    fun getLinkInfo_ecomLink() {
        val url = "https://some-ecom-domain.com"
        val domain = "some-ecom-domain.com"
        steps.ecomDomainsSteps().addEcomDomain(domain)
        doReturn(RedirectCheckResult.createSuccessResult(url, domain))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(true))
    }

    /**
     * Проверяет, что признак екомовсти `isEcom` учитывает наличие фидов у клиента и домен урла.
     *
     * Если фиды есть и домен не совпадает, то возвращаем `isEcom = false`.
     */
    @Test
    fun getLinkInfo_clientHasFeedsNotEcom() {
        steps.feedSteps().createFeed(clientInfo, "another-site.com")

        val url = "https://some-site.com/"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(false))
    }

    /**
     * Проверяет, что признак екомовсти `isEcom` учитывает наличие фидов у клиента и домен урла.
     *
     * Если фиды есть и домен совпадает, то возвращаем `isEcom = true`.
     */
    @Test
    fun getLinkInfo_clientHasFeedsIsEcom() {
        steps.feedSteps().createFeed(clientInfo, "some-site.com")

        val url = "https://some-site.com"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(true))
    }

    @Test
    fun getLinkInfo_clientHasFeedsIsEcom_urlWithWWW() {
        steps.feedSteps().createFeed(clientInfo, "www.some-site.com")

        val url = "https://some-site.com"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))
        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(false))
    }

    @Test
    fun getLinkInfo_clientHasFeedsIsEcom_punycodeUrl() {
        val punycodeDomain = IDN.toASCII("яндекс.рф")

        steps.feedSteps().createFeed(clientInfo, punycodeDomain)

        val url = "https://яндекс.рф/"
        doReturn(RedirectCheckResult.createSuccessResult(url, "яндекс.рф"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(true))
    }

    /**
     * Проверяет, что екомвость недоступна клиентам, у которых нет фидов в статусе Done
     */
    @Test
    fun getLinkInfo_clientHasNoDoneFeeds() {
        steps.feedSteps().createFeed(clientInfo, UpdateStatus.ERROR)

        val url = "https://some-site.com"
        doReturn(RedirectCheckResult.createSuccessResult(url, "some-site.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        doReturn(mapOf(url to url)).`when`(geminiClient).getMainMirrors(eq(listOf(url)))

        requestLinkInfo(url)
            .andExpect(jsonPath("$.ecom").value(false))
    }

    /**
     * Проверяет, что екомовость недоступна клиентам без общего счёта
     */
    @Test
    fun getLinkInfo_clientWithoutWallet() {
        val url = "https://some-ecom-domain.com/"
        val domain = "some-ecom-domain.com"

        val clientWithoutWallet = steps.clientSteps().createDefaultClient()
        testAuthHelper.setOperatorAndSubjectUser(clientWithoutWallet.uid)
        testAuthHelper.setSecurityContext()

        steps.ecomDomainsSteps().addEcomDomain(domain)
        doReturn(RedirectCheckResult.createSuccessResult(url, domain))
            .`when`(bannerUrlCheckService).getRedirect(url)

        requestLinkInfo(url)
            .andExpect(jsonPath("$.ecom").value(false))
    }

    @Test
    fun getLinkInfo_trackingLink() {
        doReturn(RedirectCheckResult.createSuccessResult(STORE_URL, "play.google.com"))
            .`when`(bannerUrlCheckService).getRedirect(TRACKING_URL_WITH_UNICODE_ENCODED)

        requestLinkInfo(URLEncoder.encode(TRACKING_URL_WITH_UNICODE, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.ecom").value(false))
            .andExpect(jsonPath("$.app_info").isNotEmpty)
    }

    @Test
    fun getLinkInfo_validationError() {
        requestLinkInfo("notavalidurl", HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getLinkInfo_marketplaceLink() {
        doReturn(mapOf(OZON_SHOP_URL to "https://$OZON_DOMAIN/"))
            .`when`(geminiClient).getMainMirrors(eq(listOf(OZON_SHOP_URL)))

        ppcPropertiesSupport.get(MARKETPLACE_DOMAINS).set(setOf(OZON_DOMAIN))

        requestLinkInfo(URLEncoder.encode(OZON_SHOP_URL, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.marketplace").value(true))
    }

    @Test
    fun getLinkInfo_notMarketplaceLink() {
        doReturn(mapOf(OZON_SHOP_URL to "https://$OZON_DOMAIN/"))
            .`when`(geminiClient).getMainMirrors(eq(listOf(OZON_SHOP_URL)))

        ppcPropertiesSupport.get(MARKETPLACE_DOMAINS).set(setOf(OZON_DOMAIN))
        requestLinkInfo(URLEncoder.encode(MARKET_SHOP_URL, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.marketplace").value(false))
            .andExpect(jsonPath("$.marketplace_info").value(nullValue()))
    }

    @Test
    fun getLinkInfo_marketplaceLinkWithAdditionalCommission() {
        doReturn(mapOf(MARKET_BUSINESS_SHOP_URL to "https://$MARKET_DOMAIN/"))
            .`when`(geminiClient).getMainMirrors(eq(listOf(MARKET_BUSINESS_SHOP_URL)))

        doReturn(
            ShopInShopBusiness()
                .withBusinessId(835158)
                .withFeedUrl(MARKET_BUSINESS_SHOP_URL)
                .withCounterId(2)
                .withSource(Source.MARKET)
        ).`when`(shopInShopBusinessesRepository).getBySourceAndBusinessId(any(), any())

        ppcPropertiesSupport.get(MARKETPLACE_DOMAINS).set(setOf(MARKET_DOMAIN))

        requestLinkInfo(URLEncoder.encode(MARKET_BUSINESS_SHOP_URL, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.marketplace").value(true))
            .andExpect(jsonPath("$.marketplace_info.additional_marketplace_commission")
                .value(true))
            .andExpect(jsonPath("$.marketplace_info.suggested_url_without_commission")
                .value(MARKET_STORE_SHOP_URL))
            .andExpect(jsonPath("$.marketplace_info.url_to_shop_in_shop_account")
                .value(MARKET_ACCOUNT_URL))
            .andExpect(jsonPath("$.ecom").value(true))
    }

    @Test
    fun getLinkInfo_marketplaceLinkWithoutAdditionalCommission() {
        doReturn(mapOf(MARKET_STORE_SHOP_URL to "https://$MARKET_DOMAIN/"))
            .`when`(geminiClient).getMainMirrors(eq(listOf(MARKET_STORE_SHOP_URL)))

        doReturn(
            ShopInShopBusiness()
                .withBusinessId(835158)
                .withFeedUrl(MARKET_STORE_SHOP_URL)
                .withCounterId(2)
                .withSource(Source.MARKET)
        ).`when`(shopInShopBusinessesRepository).getBySourceAndBusinessId(any(), any())

        ppcPropertiesSupport.get(MARKETPLACE_DOMAINS).set(setOf(MARKET_DOMAIN))

        requestLinkInfo(URLEncoder.encode(MARKET_STORE_SHOP_URL, StandardCharsets.UTF_8.name()))
            .andExpect(jsonPath("$.marketplace").value(true))
            .andExpect(jsonPath("$.marketplace_info.additional_marketplace_commission")
                .value(false))
            .andExpect(jsonPath("$.marketplace_info.suggested_url_without_commission")
                .value(MARKET_STORE_SHOP_URL))
            .andExpect(jsonPath("$.marketplace_info.url_to_shop_in_shop_account")
                .value(MARKET_ACCOUNT_URL))
            .andExpect(jsonPath("$.ecom").value(true))
    }

    @Test
    fun getLinkInfo_incorrectSymbolsInUrl() {
        val url = "https://wordstat.yandex.ru/#!/?words=%D1%81%D0%B0%D0%" // % в конце не распарсится

        requestLinkInfo(url, HttpStatus.BAD_REQUEST)
            .andExpect(jsonPath("$.validation_result.errors[0].code")
                .value("DefectIds.INVALID_VALUE"))
    }

    @Test
    fun getLinkInfo_correctStoreUrl() {
        doReturn(RedirectCheckResult.createSuccessResult(STORE_URL_FOR_APP_ID, "play.google.com"))
            .`when`(bannerUrlCheckService).getRedirect(STORE_URL_FOR_APP_ID)

        val result = requestLinkInfo(URLEncoder.encode(STORE_URL_FOR_APP_ID, StandardCharsets.UTF_8.name()))
            .andReturn()
            .response
            .contentAsString
        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)
        assertThat(actual).isNotNull
        assertThat(actual.appInfo!!.url).isEqualTo(STORE_URL_FOR_APP_ID)
        assertThat(actual.appInfo!!.appId).isEqualTo(APP_ID)
        assertThat(actual.appInfo!!.region).isEqualTo("RU")
        assertThat(actual.appInfo!!.language).isEqualTo("ru")
        assertThat(actual.appInfo!!.platform).isEqualTo(Platform.ANDROID)
    }

    @Test
    fun getLinkInfo_incorrectStoreUrl() {
        doReturn(RedirectCheckResult.createSuccessResult(GOOGLE_PLAY_BAD_URL, "play.google.com"))
            .`when`(bannerUrlCheckService).getRedirect(GOOGLE_PLAY_BAD_URL)
        doReturn(RedirectCheckResult.createSuccessResult(ITUNES_BAD_URL, "apps.apple.com"))
            .`when`(bannerUrlCheckService).getRedirect(ITUNES_BAD_URL)

        requestLinkInfo(URLEncoder.encode(GOOGLE_PLAY_BAD_URL, StandardCharsets.UTF_8.name()), HttpStatus.BAD_REQUEST)
        requestLinkInfo(URLEncoder.encode(ITUNES_BAD_URL, StandardCharsets.UTF_8.name()), HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getLinkInfo_googlePlayDoubledParam() {
        val url = "https://play.google.com/store/apps/details?id=ru.yandex.taxi&hl=en&gl=US&gl=BY"
        doReturn(RedirectCheckResult.createSuccessResult(url, "play.google.com"))
            .`when`(bannerUrlCheckService)
            .getRedirect(url)

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()), HttpStatus.BAD_REQUEST)
            .andExpect(jsonPath("$.validation_result.errors[0].code")
                .value("ParseAppInfoUrlDefectIds.Gen.MULTIPLE_PARAMS"))
    }

    @Test
    fun getLinkInfo_faultyUrl() {
        val url = "https://play.google.com/store/apps/details?id=ru.yandex.tax"
        doReturn(RedirectCheckResult.createSuccessResult(url, "play.google.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        val error = UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR)
        val vr = ValidationResult.success<List<String>, Defect<*>>(listOf(url))
        val mergedResult = ValidationResult.mergeSuccessfulAndInvalidItems(vr, listOf(error)) { null }

        doReturn(MassResult.successfulMassAction(mergedResult, vr))
            .`when`(bannerUrlCheckService).checkUrls(listOf(url))

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()), HttpStatus.BAD_REQUEST)
            .andExpect(jsonPath("$.validation_result.errors[0].code")
                .value("ParseAppInfoUrlDefectIds.Gen.INVALID_URL"))
    }

    @Test
    fun getLinkInfo_urlWithPercent() {
        val url = "https://5pmedicina.ru/news/novosti/detskiy-massazh-so-skidkoy-25%25"
        doReturn(RedirectCheckResult.createSuccessResult(url, "5pmedicina.ru"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        requestLinkInfo(url, HttpStatus.OK)
            .andExpect(jsonPath("$.marketplace").value(false))
            .andExpect(jsonPath("$.zen").value(false))
            .andExpect(jsonPath("$.app_info").value(nullValue()))
    }

    @Test
    fun getLinkInfo_cyrillicUrl() {
        val url = "https://xn--b1amnebsh.xn--p1ai"
        doReturn(RedirectCheckResult.createSuccessResult(url, "новости.рф"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()), HttpStatus.OK)
            .andExpect(jsonPath("$.marketplace").value(false))
            .andExpect(jsonPath("$.zen").value(false))
            .andExpect(jsonPath("$.app_info").value(nullValue()))
    }

    @Test
    fun getLinkInfo_correctTrackingUrl() {
        val encodedUrl = UrlUtils.encodeUrlQueryIfCan(URLDecoder.decode(VALID_TRACKING_URL, StandardCharsets.UTF_8))
        doReturn(RedirectCheckResult.createSuccessResult(VALID_REDIRECT_URL, "apps.apple.com"))
            .`when`(bannerUrlCheckService).getRedirect(encodedUrl)

        val result = requestLinkInfo(URLEncoder.encode(VALID_TRACKING_URL, StandardCharsets.UTF_8.name()))
            .andReturn()
            .response
            .contentAsString

        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)

        SoftAssertions.assertSoftly {
            assertThat(actual).isNotNull
            assertThat(actual.appInfo).isNotNull
            assertThat(actual.appInfo!!.url).isEqualTo(VALID_REDIRECT_URL)
            assertThat(actual.appInfo!!.appId).isEqualTo(VALID_APP_ID)
            assertThat(actual.appInfo!!.region).isEqualTo("RU")
            assertThat(actual.appInfo!!.language).isEqualTo("ru")
            assertThat(actual.appInfo!!.platform).isEqualTo(Platform.IOS)
            assertThat(actual.isMarketplace).isFalse
            assertThat(actual.isEcom).isFalse
            assertThat(actual.isZen).isFalse
        }
    }

    @Test
    fun getLinkInfo_correctTrackingUrl_WithoutFeature() {
        val encodedUrl = UrlUtils.encodeUrlQueryIfCan(URLDecoder.decode(VALID_TRACKING_URL, StandardCharsets.UTF_8))
        doReturn(RedirectCheckResult.createSuccessResult(VALID_TRACKING_URL, "app.adjust.com"))
            .`when`(bannerUrlCheckService).getRedirect(encodedUrl)

        val response = mapOf(
            "Url" to VALID_TRACKING_URL,
            "ResultUrl" to VALID_REDIRECT_URL,
            "UrlTrace" to null
        )
        val rotorResponse = jacksonObjectMapper().convertValue(response, RotorResponse::class.java)
        doReturn(rotorResponse)
            .`when`(rotorTrackingUrlIosClient).get(VALID_TRACKING_URL)

        val result = requestLinkInfo(URLEncoder.encode(VALID_TRACKING_URL, StandardCharsets.UTF_8.name()))
            .andReturn()
            .response
            .contentAsString

        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)

        SoftAssertions.assertSoftly {
            assertThat(actual).isNotNull
            assertThat(actual.appInfo).isEqualTo(null)
            assertThat(actual.isMarketplace).isFalse
            assertThat(actual.isEcom).isFalse
            assertThat(actual.isZen).isFalse
        }
    }

    @Test
    fun getLinkInfo_correctTrackingUrl_WithRotorFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.VALIDATE_TRACKING_URLS_GOROTOR, true)
        val encodedUrl = UrlUtils.encodeUrlQueryIfCan(URLDecoder.decode(VALID_TRACKING_URL, StandardCharsets.UTF_8))
        doReturn(RedirectCheckResult.createSuccessResult(VALID_TRACKING_URL, "app.adjust.com"))
            .`when`(bannerUrlCheckService).getRedirect(encodedUrl)

        val response = mapOf(
            "Url" to VALID_TRACKING_URL,
            "ResultUrl" to VALID_REDIRECT_URL,
            "UrlTrace" to null
        )
        val rotorResponse = jacksonObjectMapper().convertValue(response, RotorResponse::class.java)
        doReturn(rotorResponse)
            .`when`(rotorTrackingUrlIosClient).get(encodedUrl)

        val result = requestLinkInfo(URLEncoder.encode(VALID_TRACKING_URL, StandardCharsets.UTF_8.name()), platform = Platform.IOS)
            .andReturn()
            .response
            .contentAsString

        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)

        SoftAssertions.assertSoftly {
            assertThat(actual).isNotNull
            assertThat(actual.appInfo).isNotNull
            assertThat(actual.appInfo!!.url).isEqualTo(VALID_REDIRECT_URL)
            assertThat(actual.appInfo!!.appId).isEqualTo(VALID_APP_ID)
            assertThat(actual.appInfo!!.region).isEqualTo("RU")
            assertThat(actual.appInfo!!.language).isEqualTo("ru")
            assertThat(actual.appInfo!!.platform).isEqualTo(Platform.IOS)
            assertThat(actual.isMarketplace).isFalse
            assertThat(actual.isEcom).isFalse
            assertThat(actual.isZen).isFalse
        }
    }


    @Test
    fun getLinkInfo_randomUrl_WithRotorFeature() {
        val url = "https://licenzi.ru"
        val encodedUrl = UrlUtils.encodeUrlQueryIfCan(URLDecoder.decode(url, StandardCharsets.UTF_8))
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.VALIDATE_TRACKING_URLS_GOROTOR, true)
        doReturn(RedirectCheckResult.createSuccessResult(url, "app.adjust.com"))
            .`when`(bannerUrlCheckService).getRedirect(url)

        val response = mapOf(
            "Url" to url,
            "ResultUrl" to url,
            "UrlTrace" to listOf(url),
            "Trace" to jacksonObjectMapper().convertValue(mapOf(
                "LogEvents" to listOf(jacksonObjectMapper().convertValue(mapOf(
                    "JobResource" to jacksonObjectMapper().convertValue(mapOf(
                        "Url" to url
                    ), RotorJobResourceResponse::class.java)
                ), RotorLogEventResponse::class.java), jacksonObjectMapper().convertValue(mapOf(
                    "JobResource" to jacksonObjectMapper().convertValue(mapOf(
                        "Url" to "https://licenzi_ru.regruproxy.ru/wp-content/uploads/2022/05/%D1%86%D0%B9.png"
                    ), RotorJobResourceResponse::class.java)
                ), RotorLogEventResponse::class.java), jacksonObjectMapper().convertValue(mapOf(
                    "JobResource" to jacksonObjectMapper().convertValue(mapOf(
                        "Url" to null
                    ), RotorJobResourceResponse::class.java)
                ), RotorLogEventResponse::class.java))
            ), RotorTraceResponse::class.java)
        )
        val rotorResponse = jacksonObjectMapper().convertValue(response, RotorResponse::class.java)
        doReturn(rotorResponse)
            .`when`(rotorTrackingUrlIosClient).get(encodedUrl)

        val result = requestLinkInfo(URLEncoder.encode(url, StandardCharsets.UTF_8.name()))
            .andReturn()
            .response
            .contentAsString

        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)

        SoftAssertions.assertSoftly {
            assertThat(actual).isNotNull
            assertThat(actual.appInfo).isEqualTo(null)
            assertThat(actual.isMarketplace).isFalse
            assertThat(actual.isEcom).isFalse
            assertThat(actual.isZen).isFalse
        }
    }

    @Test
    fun getLinkInfo_correctTrackingUrl_WithRotorNullResult() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.VALIDATE_TRACKING_URLS_GOROTOR, true)
        val encodedUrl = UrlUtils.encodeUrlQueryIfCan(URLDecoder.decode(VALID_TRACKING_URL, StandardCharsets.UTF_8))
        doReturn(RedirectCheckResult.createSuccessResult(VALID_TRACKING_URL, "app.adjust.com"))
            .`when`(bannerUrlCheckService)
            .getRedirect(encodedUrl)

        doReturn(null)
            .`when`(rotorTrackingUrlIosClient)
            .get(encodedUrl)

        val result = requestLinkInfo(
            url = URLEncoder.encode(VALID_TRACKING_URL, StandardCharsets.UTF_8.name()),
            platform = Platform.IOS
        )
            .andReturn()
            .response
            .contentAsString

        val actual = fromJsonIgnoringErrors(result, LinkInfo::class.java)

        SoftAssertions.assertSoftly {
            assertThat(actual).isNotNull
            assertThat(actual.appInfo).isEqualTo(null)
            assertThat(actual.isMarketplace).isFalse
            assertThat(actual.isEcom).isFalse
            assertThat(actual.isZen).isFalse
        }
    }

    private fun requestLinkInfo(
        url: String,
        expectedStatus: HttpStatus = HttpStatus.OK,
        platform: Platform = Platform.ANDROID,
    ): ResultActions {
        return getRequest(
            "/uac/linkinfo/",
            expectedStatus.value(),
            mapOf(
                "ulogin" to clientInfo.login,
                "url" to url,
                "platform" to platform
            )
        ).andExpect(jsonPath("$").exists())
    }
}
