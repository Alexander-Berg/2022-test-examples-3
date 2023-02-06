package ru.yandex.market.mapi.controller.screen

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.controller.AbstractApiControllerTest
import ru.yandex.market.mapi.core.MapiConstants
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
class MainScreenControllerTest : AbstractApiControllerTest() {

    @Test
    fun testInvalid() {
        mvcCall(
            MockMvcRequestBuilders.patch("/api/screen/main"),
            expected = BAD_4XX,
            expectedType = null
        )
    }

    @Test
    fun testMainPageExpectToFail() {
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(MockMvcRequestBuilders.get("/api/screen/main"))
        }

        try {
            verifySingleCall(CmsPageConstants.MAIN) {
                throw IllegalStateException("Expected - ensures that verifying works well")
            }
            fail("something is wrong - verification should fail")
        } catch (cause: IllegalStateException) {
            // this is fine
        }
    }

    @Test
    fun testMainPageSimple() {
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(MockMvcRequestBuilders.get("/api/screen/main"))
        }
        verifySingleCall(CmsPageConstants.MAIN) { request ->
            assertEquals(null, request.pageToken)
            assertEquals(MainScreenController.FIRST_PAGE_SIZE_FOR_MAIN, request.pageSize)
            assertEquals(null, request.sectionIds)
        }
    }

    @Test
    fun testMainPageSimplePost() {
        // should also work well
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(MockMvcRequestBuilders.post("/api/screen/main"))
        }
        verifySingleCall(CmsPageConstants.MAIN) { request ->
            assertEquals(null, request.pageToken)
            assertEquals(MainScreenController.FIRST_PAGE_SIZE_FOR_MAIN, request.pageSize)
            assertEquals(null, request.sectionIds)
        }
    }

    @Test
    fun testMainPageWithPaging() {
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(
                MockMvcRequestBuilders.get("/api/screen/main")
                    .param(MapiConstants.PAGE_TOKEN, "21;1")
                    .param(MapiConstants.PAGE_SIZE, "12")
            )
        }
        verifySingleCall(CmsPageConstants.MAIN) { request ->
            assertEquals("21;1", request.pageToken)
            assertEquals(12, request.pageSize)
        }
    }

    @Test
    fun testMainPageWithWidgetId() {
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(
                MockMvcRequestBuilders.get("/api/screen/main")
                    .param(MapiConstants.WIDGET_ID, "123")
                    .param(MapiConstants.WIDGET_ID, "456")
            )
        }
        verifySingleCall(CmsPageConstants.MAIN) { request ->
            assertEquals(setOf("123", "456"), request.sectionIds)
        }
    }

    @Test
    fun testMainPageWithWidgetIdCommaSeparated() {
        checkApiCallNewFormat(CmsPageConstants.MAIN) {
            mvcCall(
                MockMvcRequestBuilders.get("/api/screen/main")
                    .param(MapiConstants.WIDGET_ID, "123, 456")
            )
        }
        verifySingleCall(CmsPageConstants.MAIN) { request ->
            assertEquals(setOf("123", "456"), request.sectionIds)
        }
    }
}