package ru.yandex.market.mapi.engine.degradation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.model.section.EngineTestSection
import ru.yandex.market.mapi.core.util.daoJson
import ru.yandex.market.mapi.engine.AbstractEngineTest
import ru.yandex.market.mapi.mock.InfraConfigMocker
import kotlin.test.assertEquals

class SectionDegradationServiceTest : AbstractEngineTest() {

    @Autowired
    private lateinit var sectionDegradationService: SectionDegradationService

    @Autowired
    private lateinit var infraConfigMocker: InfraConfigMocker

    @BeforeEach
    fun setup() {
        infraConfigMocker.setSectionsDegradationEnabled(true)
    }

    @Test
    fun testFilterNoSections() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponse.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/degradation/mapiResponseNoDegradation.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testDegradationDisabled() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponse.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        infraConfigMocker.setSectionsDegradationEnabled(false)
        infraConfigMocker.setSectionDegradationEnabled("111240983", true)

        assertScreen(
            "/engine/degradation/mapiResponseNoDegradation.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testFilter1Section() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponse.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        infraConfigMocker.setSectionDegradationEnabled("111240983", true)

        assertScreen(
            "/engine/degradation/mapiResponseDegradation1Section.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testFilter2Sections() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponse.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        infraConfigMocker.setSectionDegradationEnabled("111240982", true)
        infraConfigMocker.setSectionDegradationEnabled("111240984", true)

        assertScreen(
            "/engine/degradation/mapiResponseDegradation2Sections.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testFilterNoSectionsWithNonUniqueIds() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponseNonUniqueIds.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        assertScreen(
            "/engine/degradation/mapiResponseNoDegradationNonUniqueIds.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testFilter2SectionsWithNonUniqueIds() {
        templatorMocker.mockPageResponse("/engine/degradation/cmsResponseNonUniqueIds.json")
        fapiMocker.mockFapiResponse("/engine/resolverTestData.json")

        infraConfigMocker.setSectionDegradationEnabled("111240982", true)

        assertScreen(
            "/engine/degradation/mapiResponseDegradation2SectionsNonUniqueIds.json",
            getScreenAny()
        )

        fapiMocker.verifyCall(0)
    }

    @Test
    fun testSavingSectionOrder() {
        val initialSections = listOf(
            EngineTestSection().also { it.id = "1" },
            EngineTestSection().also {
                it.id = "null"
                it.testField = "first null"
            },
            EngineTestSection().also { it.id = "3" },
            EngineTestSection().also {
                it.id = "null"
                it.testField = "second null"
            },
        )
        val screenResponse = ScreenResponse(debug = daoJson(), sections = initialSections)

        sectionDegradationService.applySectionDegradation(screenResponse)

        //1
        assertEquals("1", screenResponse.sections[0].id)
        //2
        assertEquals("null", screenResponse.sections[1].id)
        val testSection1 = screenResponse.sections[1] as EngineTestSection
        assertEquals("first null", testSection1.testField)
        //3
        assertEquals("3", screenResponse.sections[2].id)
        //4
        assertEquals("null", screenResponse.sections[3].id)
        val testSection2 = screenResponse.sections[3] as EngineTestSection
        assertEquals("second null", testSection2.testField)
    }
}
