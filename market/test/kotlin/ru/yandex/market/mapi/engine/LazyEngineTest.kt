package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.action.CommonActions
import ru.yandex.market.mapi.core.model.action.ListAction
import ru.yandex.market.mapi.core.model.action.section.ReplaceSectionAction
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.util.mockFlags

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.05.2022
 */
class LazyEngineTest : AbstractEngineTest() {

    @Test
    fun testLazyLoad() {
        mockFlags(MapiHeaders.FLAG_INT_HIDE_ANALYTICS)

        // load page with lazy sections
        // fetch replacement for sections with onShow=replace actions

        templatorMocker.mockPageResponse("/engine/lazy/lazyScreen.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/resolverTestData2.json", "resolveOther")

        val screen = getScreenAny()
        assertScreen("/engine/lazy/lazyScreenResult.json", screen, keepActions = true)

        val sectionsToRefresh = screen.sections
            .filter { section ->
                val actions = section.actions ?: return@filter false
                val onShow = actions[CommonActions.ON_SHOW] ?: return@filter false

                if (onShow is ReplaceSectionAction) {
                    return@filter true
                }

                if (onShow is ListAction) {
                    return@filter onShow.actions.find { x -> x is ReplaceSectionAction } != null
                }

                return@filter false
            }

        val body = MapiScreenRequestBody<Any>().apply {
            sections = sectionsToRefresh.mapNotNull { x -> SectionToRefresh.simple(x.rawSection) }
        }

        val reload = getScreenAny(body)
        assertScreen("/engine/lazy/lazyScreenResultReload.json", reload, keepActions = true)
    }
}