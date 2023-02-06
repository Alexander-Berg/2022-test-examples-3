package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.mapi.engine.EngineContextPreparer.Companion.YA_PLUS_BATCH

/**
 * @author Ilya Kislitsyn / ilyakis@ / 21.03.2022
 */
class PlusEngineTest : AbstractEngineTest() {
    @BeforeEach
    fun initPlusParams() {
        mockOauth( "test")
    }

    @Test
    fun testPlusEngineCorrect() {
        templatorMocker.mockPageResponse("/engine/plus/screenResponseRaw.json")
        fapiMocker.mockFapiBatchResponse("/engine/plus/plusResolverCorrect.json", YA_PLUS_BATCH)
        assertScreenWithInteractions("/engine/plus/screenResponseCorrect.json", getScreenForPlusTest())
    }

    @Test
    fun testPlusEngineEmpty() {
        templatorMocker.mockPageResponse("/engine/plus/screenResponseRaw.json")
        fapiMocker.mockFapiBatchResponse("/engine/plus/plusResolverEmpty.json", YA_PLUS_BATCH)
        assertScreenWithInteractions("/engine/plus/screenResponseEmpty.json", getScreenForPlusTest())
    }

    @Test
    fun testPlusEngineInvalid() {
        templatorMocker.mockPageResponse("/engine/plus/screenResponseRaw.json")
        fapiMocker.mockFapiBatchResponse("/engine/plus/plusResolverInvalid.json", YA_PLUS_BATCH)
        assertScreenWithInteractions("/engine/plus/screenResponseInvalid.json", getScreenForPlusTest())
    }

    @Test
    fun testPlusEnginePartial() {
        templatorMocker.mockPageResponse("/engine/plus/screenResponseRaw.json")
        fapiMocker.mockFapiBatchResponse("/engine/plus/plusResolverPartial.json", YA_PLUS_BATCH)
        assertScreenWithInteractions("/engine/plus/screenResponsePartial.json", getScreenForPlusTest())
    }

    private fun getScreenForPlusTest(): ScreenResponse {
        val response = mapiScreenProcessor.getScreen(ScreenRequest().apply {
            cmsPageType = "any"
            requestPlusInfo = true
        })
        response.sections.forEach { section ->
            section.actions = null
        }
        return response
    }
}