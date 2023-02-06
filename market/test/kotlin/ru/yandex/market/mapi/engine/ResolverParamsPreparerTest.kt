package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ru.yandex.market.mapi.core.model.Location
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.core.util.mockMapiContext

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.01.2022
 */
class ResolverParamsPreparerTest {
    private val log = LoggerFactory.getLogger(javaClass)
    private val preparer = ResolverParamsPreparer()

    @BeforeEach
    fun initContext() {
        mockMapiContext { context ->
            context.location = Location.parse("13.34134;41.562562")
            context.lavkaOfferId = "2lkdjoiwejr892"
            context.taxiUserId = "5oiejf9849824f"
            context.deviceId = null
            context.advGaid = "gaid:323e293e023e"
        }
    }

    @Test
    fun testParamsPrepareDeals() {
        val node = JsonHelper.parseTree(
            """
            {
            "useCartPriceDropMark": false,
            "showOffersWithoutDeliveryInRegion": true,
            "showDigitalDsbsGoods": true,
            "pageSize": 30,
            "showPreorder": true,
            "cartSnapshot": "%%PARAMS.cartSnapshot%%",
            "myExtensionParam": "%%PARAMS.myExtensionParam%%",
            "billingZone": "wishlist"
            }
        """.trimIndent()
        )

        val expected = """
            {
            "useCartPriceDropMark": false,
            "showOffersWithoutDeliveryInRegion": true,
            "showDigitalDsbsGoods": true,
            "pageSize": 30,
            "showPreorder": true,
            "billingZone": "wishlist"
            }
        """

        assertJson(preparer.processParameters(node, ScreenRequest()), expected, isExpectedInFile = false)
    }

    @Test
    fun testParamsPrepareLavka() {
        val node = JsonHelper.parseTree(
            """
            {
                "position": "%%PARAMS.position%%",
                "offerId": "%%PARAMS.offerId%%",
                "taxiUserId": "%%PARAMS.taxiUserId%%",
                "deviceId": "%%PARAMS.deviceId%%",
                "complementForUser": true,
                "products": "%%PARAMS.products%%",
                "cart": "%%PARAMS.cart%%"
            }
        """.trimIndent()
        )

        val expected = """
            {
                "position": {"location": [41.562562, 13.34134]},
                "offerId": "2lkdjoiwejr892",
                "taxiUserId": "5oiejf9849824f",
                "complementForUser": true
            }
        """

        assertJson(preparer.processParameters(node, ScreenRequest()), expected, isExpectedInFile = false)
    }

    @Test
    fun testParamsPrepareComplex() {
        val node = JsonHelper.parseTree(
            """
        {
            "djPlace": "morda_promo_thematics_product_block",
            "billingZone": "default",
            "page": 1,
            "numdoc": 20,
            "hid": null,
            "hyperid": null,
            "topic": null,
            "range": "1",
            "gaid": "%%PARAMS.gaid%%",
            "gaid_skip": "%%PARAMS.gaid_skip%%",
            "cartSnapshot": "%%PARAMS.cartSnapshot%%",
            "recomContext": null,
            "widget_position": null,
            "viewUniqueId": "%%PARAMS.viewUniqueId%%",
            "showPreorder": true,
            "rawParams": {
                "parentPromoIds": [
                    "SP#200"
                ],
                "shopPromoIds": [],
                "nid": [],
                "fromCMSFlagForRawParams": true,
                "supplierIds": [
                    "%%PARAMS.gaid%%",
                    {
                        "gaid": "%%PARAMS.gaid%%",
                        "unknown": "%%PARAMS.unknown%%"
                    },
                    "%%PARAMS.unknown%%",
                    "%%PARAMS.gaid%%"
                ],
                "discountFrom": 5,
                "warehouseId": null
            }
        }
        """.trimIndent()
        )

        val expected = """
        {
            "djPlace": "morda_promo_thematics_product_block",
            "billingZone": "default",
            "page": 1,
            "numdoc": 20,
            "range": "1",
            "gaid": "gaid:323e293e023e",
            "showPreorder": true,
            "rawParams": {
                "parentPromoIds": [
                    "SP#200"
                ],
                "shopPromoIds": [],
                "nid": [],
                "fromCMSFlagForRawParams": true,
                "supplierIds": [
                    "gaid:323e293e023e",
                    {
                        "gaid": "gaid:323e293e023e"
                    },
                    "gaid:323e293e023e"
                ],
                "discountFrom": 5
            }
        }
        """

        assertJson(preparer.processParameters(node, ScreenRequest()), expected, isExpectedInFile = false)
    }

    @Test
    fun testParamsPrepareWithExtensions() {
        val node = JsonHelper.parseTree(
            """
            {
            "myExtensionParam": "%%PARAMS.myExtensionParam%%"
            }
        """.trimIndent()
        )

        val expected = """
            {
            "myExtensionParam": "addedWithExtension"
            }
        """

        val request = ScreenRequest()
        request.customResolverParams = mutableMapOf(
            "myExtensionParam" to "addedWithExtension"
        )

        assertJson(preparer.processParameters(node, request), expected, isExpectedInFile = false)
    }

    @Test
    fun testParamPrepareWithRefreshParams() {
        val node = JsonHelper.parseTree(
            """
            {
            "param": "%%PARAMS.param%%",
            "pageSize": "%%PARAMS.orders.pageSize%%",
            "myExtensionParam1": "%%PARAMS.myExtensionParam1%%",
            "myExtensionParam2": "%%PARAMS.myExtensionParam2%%"
            }
        """.trimIndent()
        )

        val refreshParams = mapOf(
            "param" to "param_value",
            "orders.pageSize" to "12",
            "myExtensionParam1" to "addedWithRefresh1"
        )

        val request = ScreenRequest()
        request.customResolverParams = mutableMapOf(
            "myExtensionParam1" to "addedWithExtension1",
            "myExtensionParam2" to "addedWithExtension2"
        )

        val expected = """
            {
            "param": "param_value",
            "pageSize": "12",
            "myExtensionParam1": "addedWithRefresh1",
            "myExtensionParam2": "addedWithExtension2"
            }
        """

        assertJson(preparer.processParameters(node, request, refreshParams), expected, isExpectedInFile = false)
    }
}
