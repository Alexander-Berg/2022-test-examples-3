package ru.yandex.market.mapi.engine.pumpkin

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.pumpkin.PumpkinConstants
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.mapi.db.PumpkinRepository
import ru.yandex.market.mapi.engine.AbstractEngineTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MainScreenPumpkinBuilderTest : AbstractEngineTest() {

    @Autowired
    private lateinit var pumpkinRepository: PumpkinRepository

    @Autowired
    private lateinit var pumpkinBuilder: MainScreenPumpkinBuilder

    @Test
    fun testBuildingSimplePumpkin() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json")
        pumpkinBuilder.buildPumpkin()

        assertScreen(
            "/engine/basicCmsTestPageOk.json",
            getBuiltPumpkin(),
            cleanSnippetType = false
        )

        verifyNoMoreInteractions(pumpkinRepository)
    }

    @Test
    fun testDontRebuildPumpkinWhenCriticalErrors() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json")
        pumpkinBuilder.buildPumpkin()
        verify(pumpkinRepository, times(1)).upsertValue(any(), any())

        templatorMocker.mockPageResponse("/engine/basicCmsTestPageInvalidContent.json")
        pumpkinBuilder.buildPumpkin()

        // new pumpkin was not set
        verifyNoMoreInteractions(pumpkinRepository)
    }

    @Test
    fun testPartiallyBuildPumpkinWhenSectionHasEmptyContent() {
        mockFlags(MapiHeaders.FLAG_INT_HIDE_ANALYTICS)
        templatorMocker.mockPageResponse("/engine/pumpkin/hiddenSection.json")
        pumpkinBuilder.buildPumpkin()

        assertScreen(
            "/engine/pumpkin/hiddenSectionPumpkin.json",
            getBuiltPumpkin()
        )
    }

    @Test
    fun testPartiallyBuildPumpkinWhenSectionHasErrors() {
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        pumpkinBuilder.buildPumpkin()

        assertScreen(
            "/engine/pumpkin/sectionWithErrorsPumpkin.json",
            getBuiltPumpkin()
        )
    }

    @Test
    fun testBuildPumpkinWithShowConditions() {
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithConditions.json")
        pumpkinBuilder.buildPumpkin()

        assertScreen(
            "/engine/pumpkin/sectionWithConditionsPumpkin.json",
            getBuiltPumpkin(),
            cleanSnippetType = false
        )
    }

    fun getBuiltPumpkin(): ScreenResponse {
        val idCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<String>()
        verify(pumpkinRepository, times(1)).upsertValue(idCaptor.capture(), valueCaptor.capture())

        assertEquals(PumpkinConstants.PUMPKIN_MAIN, idCaptor.firstValue)
        val actualResult = valueCaptor.firstValue
        assertNotNull(actualResult)

        return JsonHelper.parse(actualResult)
    }
}
