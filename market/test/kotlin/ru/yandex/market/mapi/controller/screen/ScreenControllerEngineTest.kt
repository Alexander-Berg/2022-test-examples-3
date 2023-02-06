package ru.yandex.market.mapi.controller.screen

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.mock.FapiMocker
import ru.yandex.market.mapi.mock.TemplatorMocker

/**
 * Tests to check contract between screen-controllers and engine.
 */
class ScreenControllerEngineTest : AbstractMapiTest() {
    @Autowired
    lateinit var templatorMocker: TemplatorMocker

    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Test
    fun testInvalid() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        val response = mvcCall(patch("/api/screen/main"), expected = BAD_4XX)
        val expectedJson = """
            {"message":"Request method 'PATCH' not supported","code":"METHOD_NOT_SUPPORTED","status":405}
        """.trimIndent()
        JSONAssert.assertEquals(expectedJson, response, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    fun testMock() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(mvcCall(get("/api/screen/main")), "/controller/basicScreenWithResolverResut.json", "Response")
    }

    @Test
    fun testMockPost() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPageWithResolver.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(mvcCall(post("/api/screen/main")), "/controller/basicScreenWithResolverResut.json", "Response")
    }

    @Test
    fun testSwitchSectionScreen() {
        templatorMocker.mockPageResponse("/controller/switchScreen.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertJson(mvcCall(get("/api/screen/main")), "/controller/switchScreenResult.json")
    }

}
