package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.util.daoJsonTyped
import ru.yandex.market.mapi.core.util.optInt
import ru.yandex.market.mapi.engine.pager.SectionPager
import kotlin.test.assertNull

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.05.2022
 */
class SectionPagingByPageNumEngineTest : AbstractSectionPagingTest() {

    @Test
    fun testSectionPagingSimple() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json", checker = { resolver ->
            resolver.resolver == "resolvePrime" && resolver.params?.optInt("startpage") == 1
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage2.json", checker = { resolver ->
            resolver.resolver == "resolvePrime" && resolver.params?.optInt("startpage") == 2
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage3.json", checker = { resolver ->
            resolver.resolver == "resolvePrime" && resolver.params?.optInt("startpage") == 3
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage3.json", "resolveOther")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_page_num/screenWithPagedSectionResult1.json", screen, keepActions = true)

        // check second page
        val pagedSectionId = "11"
        val screen2 = getScreenAny(body = buildPagingBody(screen, pagedSectionId))
        assertScreen("/engine/section/paged_by_page_num/screenWithPagedSectionResult2.json", screen2, keepActions = true)

        // check last page
        val screen3 = getScreenAny(body = buildPagingBody(screen2, pagedSectionId))
        assertScreen("/engine/section/paged_by_page_num/screenWithPagedSectionResult3.json", screen3, keepActions = true)

        // check there is no more paging action
        assertNull(getPagingAction(screen3, pagedSectionId))
    }

    @Test
    fun testComplexPaging() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithComplexPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json", checker = { resolver ->
            resolver.resolver == "resolvePrime" && resolver.params?.optInt("startpage") == 1
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage3.json", checker = { resolver ->
            resolver.resolver == "resolvePrime" && resolver.params?.optInt("startpage") == 2
        })
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage2.json", "resolveOther")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_page_num/screenWithComplexPagedSectionResult1.json", screen, keepActions = true)

        // check second page
        val pagedSectionId = "11"
        val screen2 = getScreenAny(body = buildPagingBody(screen, pagedSectionId))
        assertScreen("/engine/section/paged_by_page_num/screenWithComplexPagedSectionResult2.json", screen2, keepActions = true)

        // check there is no more paging action
        assertNull(getPagingAction(screen2, pagedSectionId))
    }

    @Test
    fun testBrokenPager() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithBrokenPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json", "resolvePrime")

        assertScreen(
            "/engine/section/paged_by_page_num/screenWithBrokenPagedSectionResult.json",
            getScreenAny(),
            keepActions = true
        )
    }

    @Test
    fun testBrokenInvalidPageToken() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithPagedSection.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json", "resolvePrime")

        val screen = getScreenAny()
        val body = buildPagingBody(screen, "11")
        body.sections?.firstOrNull()?.refreshParams = daoJsonTyped(
            SectionPager.PARAM_TOKEN to "invalid"
        )

        assertScreen(
            "/engine/section/paged_by_page_num/screenWithInvalidPageToken.json",
            getScreenAny(body = body),
            keepActions = true
        )
    }

    @Test
    fun testPagerWithAlias() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithAlias.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json", "resolvePrime")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage2.json", "resolveOther")

        assertScreen(
            "/engine/section/paged_by_page_num/screenWithAliasResult.json",
            getScreenAny(),
            keepActions = true
        )
    }

    @Test
    fun testSectionParams() {
        templatorMocker.mockPageResponse("/engine/section/paged_by_page_num/screenWithPagedSectionWithSectionParams.json", "any")
        fapiMocker.mockFapiResponse("/engine/section/paged_by_page_num/resolvePagedPage1.json")

        val screen = getScreenAny()
        assertScreen("/engine/section/paged_by_page_num/screenWithPagedSectionWithSectionParamsResult.json", screen, keepActions = true)
    }
}
