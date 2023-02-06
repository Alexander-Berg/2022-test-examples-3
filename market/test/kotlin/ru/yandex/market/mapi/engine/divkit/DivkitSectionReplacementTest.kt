package ru.yandex.market.mapi.engine.divkit

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.engine.AbstractEngineTest

class DivkitSectionReplacementTest: AbstractEngineTest() {

    @Test
    fun testSectionShouldBeReplacedWithDivkitSection() {
        templatorMocker.mockPageResponse("/engine/divkit/testSectionToBeReplacedWithDivkit.json", "any")
        fapiMocker.mockFapiResponse("/controller/divkit/testDivkitTemplatedSectionResolverData.json", "resolveNames")

        val screen = getScreenAny()
        assertScreen("/engine/divkit/divkitSectionResult.json", screen)
    }

    @Test
    fun testSectionShouldNotBeReplacedWithDivkitSectionDueToSnippetError() {
        templatorMocker.mockPageResponse("/engine/divkit/testSectionToBeReplacedWithDivkitButWrongAssembler.json", "any")

        val screen = getScreenAny()
        assertScreen("/engine/divkit/divkitErrorSectionResult.json", screen)
    }
}
