package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.section.EngineTestMultiAssembler

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.04.2022
 */
class MultiResolverEngineTest : AbstractEngineTest() {

    @Test
    fun testMultipleResolver() {
        templatorMocker.mockPageResponse("/engine/multi/screenMulti.json")
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiFirst.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)

        assertScreen("/engine/multi/screenMultiResult.json", getScreenAny())
    }

    @Test
    fun testMultiplePartialResolver() {
        templatorMocker.mockPageResponse("/engine/multi/screenPartial.json")
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiFirst.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)

        assertScreen("/engine/multi/screenPartialResult.json", getScreenAny())
    }

    @Test
    fun testMultipleUnknown() {
        templatorMocker.mockPageResponse("/engine/multi/screenUnknown.json")
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiFirst.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", "thirdResolver")

        assertScreen("/engine/multi/screenUnknownResult.json", getScreenAny())
    }

    @Test
    fun testMultipleDuplicate() {
        templatorMocker.mockPageResponse("/engine/multi/screenDuplicate.json")
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiFirst.json", EngineTestMultiAssembler.RES_FIRST)
        fapiMocker.mockFapiResponse("/engine/multi/resolverMultiSecond.json", EngineTestMultiAssembler.RES_SECOND)

        assertScreen("/engine/multi/screenDuplicateResult.json", getScreenAny())
    }
}