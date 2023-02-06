package ru.yandex.market.mapi.controller.divkit

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.mock.FapiMocker
import ru.yandex.market.mapi.mock.TemplatorMocker

class ControllerDivkitTest : AbstractMapiTest(){
    @Autowired
    lateinit var templatorMocker: TemplatorMocker

    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Test
    fun testScreenWithOneDivkitTemplatedSection() {
        templatorMocker.mockPageResponse("/controller/divkit/testDivkitTemplatedSection.json")

        fapiMocker.mockFapiResponse("/controller/divkit/testDivkitTemplatedSectionResolverData.json", "resolveNames")

        assertJson(mvcCall(MockMvcRequestBuilders.get("/api/screen/main")), "/controller/divkit/testDivkitTemplatedSectionResult.json")
    }

    @Test
    fun testScreenWithTwoDivkitTemplatedSections() {
        templatorMocker.mockPageResponse("/controller/divkit/testTwoDivkitTemplatedSections.json")

        fapiMocker.mockFapiResponse("/controller/divkit/testDivkitTemplatedSectionResolverData.json", "resolveNames")

        assertJson(mvcCall(MockMvcRequestBuilders.get("/api/screen/main")), "/controller/divkit/testTwoDivkitTemplatedSectionsResult.json")
    }

    @Test
    fun testScreenWithReplacedWithDivkitSection() {
        templatorMocker.mockPageResponse("/engine/divkit/testSectionToBeReplacedWithDivkit.json")

        fapiMocker.mockFapiResponse("/controller/divkit/testDivkitTemplatedSectionResolverData.json", "resolveNames")

        assertJson(mvcCall(MockMvcRequestBuilders.get("/api/screen/main")), "/controller/divkit/testReplacedWithDivkitSectionResult.json")
    }
}
