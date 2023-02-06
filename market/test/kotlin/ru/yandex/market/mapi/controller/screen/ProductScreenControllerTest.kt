package ru.yandex.market.mapi.controller.screen

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.controller.AbstractApiControllerTest
import ru.yandex.market.mapi.controller.screen.ProductScreenController.ProductScreenRequest
import ru.yandex.market.mapi.core.MapiConstants
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.05.2022
 */
class ProductScreenControllerTest : AbstractApiControllerTest() {
    @Test
    fun testProductPageSimple() {
        checkApiCallNewFormat(CmsPageConstants.PRODUCT) {
            mvcCall(
                MockMvcRequestBuilders.post("/api/screen/product")
                    .param(MapiConstants.PRODUCT_ID, "123")
                    .content("{}")
            )
        }
        verifySingleCall(CmsPageConstants.PRODUCT) { request ->
            assertEquals(
                mutableMapOf<String, Any?>(
                    "productId" to 123L,
                    "productIdString" to "123",
                    "productIds" to listOf("123"),
                ), request.customResolverParams
            )
        }
    }

    @Test
    fun testProductPageSimpleBadParams() {
        mockAnyProcessorResult()

        try {
            mvcCall(
                MockMvcRequestBuilders.post("/api/screen/product")
                    .content("{}")
            )
            fail("should fail above")
        } catch (cause: RuntimeException) {
            assertEquals(cause.cause?.cause?.message, "Invalid params: no product, sku, offer")
        }
    }

    @Test
    fun testProductPageComplex() {
        checkApiCallNewFormat(CmsPageConstants.PRODUCT) {
            mvcCall(
                MockMvcRequestBuilders.post("/api/screen/product")
                    .param(MapiConstants.PRODUCT_ID, "123")
                    .param(MapiConstants.SKU_ID, "asdasd")
                    .param(MapiConstants.OFFER_ID, "qweqwe"),
                body = simpleRequest(ProductScreenRequest().also { request ->
                    request.cpa = "1"
                    request.cpc = "0"
                })
            )
        }

        verifySingleCall(CmsPageConstants.PRODUCT) { request ->
            assertResolverParams(
                mutableMapOf(
                    "skuId" to "asdasd",
                    "skuIds" to listOf("asdasd"),
                    "productId" to 123L,
                    "productIdString" to "123",
                    "productIds" to listOf("123"),
                    "offerId" to "qweqwe",
                    "cpa" to "1",
                    "cpc" to "0",
                ), request
            )
        }
    }
}
