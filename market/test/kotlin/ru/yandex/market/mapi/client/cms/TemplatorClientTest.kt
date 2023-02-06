package ru.yandex.market.mapi.client.cms

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import ru.yandex.market.mapi.client.AbstractClientTest
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MapiContextRw
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.UserPassportInfo
import ru.yandex.market.mapi.core.util.mockExpInfo
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.mapi.core.util.mockMapiContext
import ru.yandex.market.mapi.core.util.mockRegion
import ru.yandex.market.request.trace.Module

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.01.2022
 */
class TemplatorClientTest : AbstractClientTest() {
    private lateinit var templatorClient: TemplatorClient

    @BeforeEach
    fun setup() {
        templatorClient = TemplatorClient(mockedRetrofit(Module.TEMPLATOR))
    }

    @Test
    fun testSimpleTemplatorCall() {
        mockClientResponse("/client/cms/templatorResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorParsedScreen.json"
        )
    }

    @Test
    fun testDebugRequests() {
        mockFlags(MapiHeaders.FLAG_INT_DEBUG_REQUESTS)
        mockClientResponse("/client/cms/templatorResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorParsedScreenWithDebugRequests.json"
        )
    }

    @Test
    fun testTemplatorReturnedNonArraySections() {
        mockClientResponse("/client/cms/templatorNonArrayWidgetResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorNonArrayWidgetParsedScreen.json"
        )
    }

    @Test
    fun testStaticRequestParams() {
        mockClientResponse("/client/cms/templatorNonArrayWidgetResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorNonArrayWidgetParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/tarantino/getcontextpage")
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
                "ignore_cgi_params" to "uuid%2Cpuid%2Cclient%2Cdeviceid%2Cidfa%2Cgaid"
            )
        }
    }

    @Test
    fun testRequestWithBranch() {
        (MapiContext.get() as MapiContextRw).cmsBranch = "someBranch"

        mockClientResponse("/client/cms/templatorNonArrayWidgetResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorNonArrayWidgetParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertRequest(HttpMethod.POST, "/tarantino/getcontextpage")
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
                "templator_content" to "preview",
                "ds" to "someBranch",
                "ignore_cgi_params" to "uuid%2Cpuid%2Cclient%2Cdeviceid%2Cidfa%2Cgaid",
            )
        }
    }

    @Test
    fun testRearrAndRegionRequestParams() {
        mockRegion(213)
        mockExpInfo(
            UserExpInfo(
                version = null,
                uaasRearrs = linkedSetOf("rearr1=1"),
            )
        )

        mockClientResponse("/client/cms/templatorNonArrayWidgetResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorNonArrayWidgetParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
                "rearr-factors" to "",
                "region" to "213",
                "rearr-factors" to "rearr1%3D1%3Bmarket_support_market_sku_in_product_redirects%3D1%3Bmarket_white_cpa_on_blue%3D2%3Bmarket_enable_foodtech_offers%3Deda_retail",
                "ignore_cgi_params" to "uuid%2Cpuid%2Cclient%2Cdeviceid%2Cidfa%2Cgaid",
            )
        }
    }

    @Test
    fun testTwoRearrsRequestParam() {
        mockExpInfo(
            UserExpInfo(
                version = null,
                uaasRearrs = linkedSetOf("rearr1=1", "rearr2=1"),
            )
        )

        mockClientResponse("/client/cms/templatorNonArrayWidgetResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorNonArrayWidgetParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
                "rearr-factors" to "rearr1%3D1%3Brearr2%3D1%3Bmarket_support_market_sku_in_product_redirects%3D1%3Bmarket_white_cpa_on_blue%3D2%3Bmarket_enable_foodtech_offers%3Deda_retail",
                "ignore_cgi_params" to "uuid%2Cpuid%2Cclient%2Cdeviceid%2Cidfa%2Cgaid",
            )
        }
    }

    @Test
    fun testCmsPageTemplateSimple() {
        mockExpInfo(
            UserExpInfo(
                version = null,
                uaasRearrs = linkedSetOf("rearr1=1", "rearr2=1"),
            )
        )

        mockClientResponse("/client/cms/templatorResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplateSimple("any"),
            "/client/cms/templatorParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
            )
        }
    }

    @Test
    fun testCgiParams() {
        mockMapiContext { context ->
            context.uuid = "some-uuid"
            context.appPlatform = "ANDROID"
            context.deviceId = "some-device-id"
            context.advGaid = "some-gaid"
            context.advIdfa = "some-idfa"
            context.oauthInfo = UserPassportInfo(
                isValid = true,
                error = null,
                login = null,
                uid = 42,
                userTicket = null,
                isYandexoid = false
            )
        }

        mockClientResponse("/client/cms/templatorResponse.json")

        assertResponse(
            templatorClient.getCmsPageTemplate("any"),
            "/client/cms/templatorParsedScreen.json"
        )

        verifyClientCall { _, request ->
            request.assertQuery(
                "type" to "any",
                "device" to "phone",
                "format" to "json",
                "zoom" to "full",
                "domain" to "ru",
                "ignore_cgi_params" to "uuid%2Cpuid%2Cclient%2Cdeviceid%2Cidfa%2Cgaid",
                "uuid" to "some-uuid",
                "puid" to "42",
                "deviceid" to "some-device-id",
                "gaid" to "some-gaid",
                "idfa" to "some-idfa",
                "client" to "ANDROID"
            )
        }
    }
}
