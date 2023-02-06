package ru.yandex.market.mapi.controller.screen

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.controller.AbstractApiControllerTest
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * @author Arsen Salimov / maetimo@ / 25.07.2022
 */
class CheckoutScreenControllerTest : AbstractApiControllerTest() {

    @Test
    fun testInvalid() {
        mvcCall(
            MockMvcRequestBuilders.patch("/api/screen/checkout"),
            expected = BAD_4XX,
            expectedType = null
        )
    }

    @Test
    fun testCheckoutPageExpectToFail() {
        checkApiCallNewFormat(CmsPageConstants.CHECKOUT) {
            mvcCall(MockMvcRequestBuilders.post("/api/screen/checkout"))
        }

        try {
            verifySingleCall(CmsPageConstants.CHECKOUT) {
                throw IllegalStateException("Expected - ensures that verifying works well")
            }
            fail("something is wrong - verification should fail")
        } catch (cause: IllegalStateException) {
            // this is fine
        }
    }

    @Test
    fun testCheckoutPageSimple() {
        checkApiCallNewFormat(CmsPageConstants.CHECKOUT) {
            mvcCall(MockMvcRequestBuilders.post("/api/screen/checkout"))
        }
        verifySingleCall(CmsPageConstants.CHECKOUT) { request ->
            assertEquals(null, request.pageToken)
            assertEquals(null, request.sectionIds)
        }
    }

    @Test
    fun testCheckoutPageSimplePost() {
        // should also work well
        checkApiCallNewFormat(CmsPageConstants.CHECKOUT) {
            mvcCall(MockMvcRequestBuilders.post("/api/screen/checkout"))
        }
        verifySingleCall(CmsPageConstants.CHECKOUT) { request ->
            assertEquals(null, request.pageToken)
            assertEquals(null, request.sectionIds)
        }
    }
}
