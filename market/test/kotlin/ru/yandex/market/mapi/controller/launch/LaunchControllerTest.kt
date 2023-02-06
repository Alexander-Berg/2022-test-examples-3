package ru.yandex.market.mapi.controller.launch

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.mock.FapiMocker

/**
 * Test for checking /api/launch controller functionality
 */
class LaunchControllerTest : AbstractMapiTest() {
    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Test
    fun testOutdated() {
        fapiMocker.mockFapiResponse(file = "/controller/launch/resolveAppForceUpdateOutdated.json",
            name = "resolveAppForceUpdate")
        assertJson(result = mvcCall(MockMvcRequestBuilders.post("/api/launch")),
            expected = "/controller/launch/launchAppForceUpdateOutdated.json")
    }

    @Test
    fun testError() {
        fapiMocker.mockFapiResponse(file = "/controller/launch/resolveAppForceUpdateError.json",
            name = "resolveAppForceUpdate")
        assertJson(result = mvcCall(MockMvcRequestBuilders.post("/api/launch")),
            expected = "/controller/launch/launchAppForceUpdateError.json")
    }

    @Test
    fun testWrongMethod() {
        mvcCall(
            MockMvcRequestBuilders.patch("/api/launch"),
            expected = BAD_4XX,
            expectedType = null
        )
    }
}
