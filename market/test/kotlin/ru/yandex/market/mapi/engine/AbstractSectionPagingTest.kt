package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.BeforeEach
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.action.CommonActions
import ru.yandex.market.mapi.core.model.action.section.MergeSectionAction
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.util.mockFlags
import kotlin.test.assertNotNull

abstract class AbstractSectionPagingTest: AbstractEngineTest() {
    @BeforeEach
    fun init() {
        mockFlags(MapiHeaders.FLAG_INT_HIDE_ANALYTICS)
    }

    protected fun getPagingAction(screen: ScreenResponse, sectionId: String): MergeSectionAction? {
        val section = screen.sections.find { it.id == sectionId }
        assertNotNull(section)

        return section.actions?.get(CommonActions.ON_LOAD_MORE) as? MergeSectionAction?
    }

    protected fun buildPagingBody(screen: ScreenResponse, sectionId: String): MapiScreenRequestBody<Any> {
        val loadMoreAction = getPagingAction(screen, sectionId)
        assertNotNull(loadMoreAction)

        val refreshSections = screen.sections.mapNotNull { section ->
            if (loadMoreAction.sectionId != section.id) return@mapNotNull null
            SectionToRefresh().apply {
                raw = section.rawSection
                refreshParams = loadMoreAction.params
            }
        }

        return MapiScreenRequestBody<Any>().also { body ->
            body.sections = refreshSections
            body.context = screen.context
        }
    }
}
