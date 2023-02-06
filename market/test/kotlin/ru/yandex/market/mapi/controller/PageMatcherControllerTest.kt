package ru.yandex.market.mapi.controller

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.AbstractMapiTest
import java.nio.charset.StandardCharsets

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.01.2022
 */
class PageMatcherControllerTest : AbstractMapiTest() {
    private val SEPARATOR = "\t"
    private val PAGE_TYPE = "http"

    @Test
    fun testPageMatcherResultContains() {
        val response: String = getPageMatcher()

        assertItem(response, "main_screen_by_get", "GET:/api/screen/main")
        assertItem(response, "read_pagematch_by_get", "GET:/pagematch")
        assertItem(response, "ping", "GET:/ping")
        assertItem(response, "monitoring", "GET:/monitoring")

        // special test controller
        assertItem(response, "test_method_by_get", "GET:/abc")
        assertItem(response, "test_method_by_post", "POST:/abc")
        assertItem(response, "test_method_by_head", "HEAD:/abc")
        assertItem(response, "test_method1_by_get", "GET:/abc2")
        assertItem(response, "test_method_by_patch", "PATCH:/abc3")
    }

    private fun assertItem(response:String, operationId: String, path: String) {
        assertTrue(
            response.contains(
                listOf(operationId, path, PAGE_TYPE).joinToString(SEPARATOR)
            )
        )
    }

    private fun getPageMatcher(): String {
        return mvcCall(
            MockMvcRequestBuilders.get("/pagematch"),
            expectedType = MediaType("text", "tab-separated-values", StandardCharsets.UTF_8)
        )
    }
}