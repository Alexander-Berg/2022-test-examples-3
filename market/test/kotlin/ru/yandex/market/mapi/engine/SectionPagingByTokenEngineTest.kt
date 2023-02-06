package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.util.daoJsonTyped
import ru.yandex.market.mapi.core.util.optInt
import ru.yandex.market.mapi.core.util.optStr
import ru.yandex.market.mapi.engine.pager.SectionPager
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * @author: Anastasia Fakhrieva | afakhrieva@
 * Date: 12.07.2022
 */
class SectionPagingByTokenEngineTest: AbstractSectionPagingTest() {

    @Test
    fun testTokenPagingSimple() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_token/screenWithPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageWhite1.json", checker = { resolver ->
            resolver.resolver == "resolvePrime"
                && resolver.params?.optInt("page") == 1
                && resolver.params?.optStr("color") == "white"
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageRed2.json", checker = { resolver ->
            resolver.resolver == "resolvePrime"
                && resolver.params?.optInt("page") == 2
                && resolver.params?.optStr("color") == "red"
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageBlue3.json", checker = { resolver ->
            resolver.resolver == "resolvePrime"
                && resolver.params?.optInt("page") == 3
                && resolver.params?.optStr("color") == "blue"
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageBlue3.json", "resolveOther")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_token/screenWithPagedSectionResult1.json", screen, keepActions = true)

        // check second page
        val pagedSectionId = "11"
        val screen2 = getScreenAny(body = buildPagingBody(screen, pagedSectionId))
        assertScreen("/engine/section/paged_by_token/screenWithPagedSectionResult2.json", screen2, keepActions = true)

        // check last page
        val screen3 = getScreenAny(body = buildPagingBody(screen2, pagedSectionId))
        assertScreen("/engine/section/paged_by_token/screenWithPagedSectionResult3.json", screen3, keepActions = true)

        // check there is no more paging action
        assertNull(getPagingAction(screen3, pagedSectionId))
    }

    @Test
    fun testComplexPaging() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_token/screenWithComplexPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageWhite1.json", checker = { resolver ->
            resolver.resolver == "resolvePrime"
                && resolver.params?.optInt("page") == 1
                && resolver.params?.optStr("color") == "white"
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageRed2.json", checker = { resolver ->
            resolver.resolver == "resolvePrime"
                && resolver.params?.optInt("page") == 2
                && resolver.params?.optStr("color") == "red"
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageBlue3.json", "resolveOther")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_token/screenWithComplexPagedSectionResult1.json", screen, keepActions = true)

        // check second page
        val pagedSectionId = "11"
        val screen2 = getScreenAny(body = buildPagingBody(screen, pagedSectionId))
        assertScreen("/engine/section/paged_by_token/screenWithComplexPagedSectionResult2.json", screen2, keepActions = true)

        // check there is no more paging action
        assertNotNull(getPagingAction(screen2, pagedSectionId))
    }

    @Test
    fun testBrokenPager() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_token/screenWithBrokenPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageWhite1.json", "resolvePrime")

        assertScreen(
            "/engine/section/paged_by_token/screenWithBrokenPagedSectionResult.json",
            getScreenAny(),
            keepActions = true
        )
    }

    @Test
    fun testBrokenInvalidPageToken() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_token/screenWithPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageWhite1.json", "resolvePrime")

        val screen = getScreenAny()
        val body = buildPagingBody(screen, "11")
        body.sections?.firstOrNull()?.refreshParams = daoJsonTyped(
            SectionPager.PARAM_TOKEN to "invalid"
        )

        assertScreen(
            "/engine/section/paged_by_token/screenWithInvalidPageToken.json",
            getScreenAny(body = body),
            keepActions = true
        )
    }

    @Test
    fun testSectionParams() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_token/screenWithPagedSectionWithSectionParams.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_token/resolvePagedPageWhite1.json")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_token/screenWithPagedSectionWithSectionParamsResult.json", screen, keepActions = true)
    }
}
