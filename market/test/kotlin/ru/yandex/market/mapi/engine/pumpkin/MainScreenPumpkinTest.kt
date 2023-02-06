package ru.yandex.market.mapi.engine.pumpkin

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.SectionToRefresh
import ru.yandex.market.mapi.core.pumpkin.PumpkinConstants
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.mockNoOauth
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.mapi.db.PumpkinRepository
import ru.yandex.market.mapi.engine.AbstractEngineTest
import ru.yandex.market.mapi.mock.BlackboxMocker
import ru.yandex.market.mapi.mock.InfraConfigMocker
import kotlin.test.assertEquals

class MainScreenPumpkinTest : AbstractEngineTest() {

    @Autowired
    private lateinit var pumpkinStorage: PumpkinStorage

    @Autowired
    private lateinit var pumpkinRepository: PumpkinRepository

    @Autowired
    private lateinit var blackboxMocker: BlackboxMocker

    @Autowired
    private lateinit var infraConfigMocker: InfraConfigMocker

    private fun mockPumpkin(file: String? = null) {
        val pumpkinSource = file ?: "/engine/basicCmsTestPageOk.json"

        // set pumpkin to repo
        whenever(pumpkinRepository.getAll()).thenReturn(
            mapOf(
                PumpkinConstants.PUMPKIN_MAIN to pumpkinSource.asResource()
            )
        )

        // refresh storage
        pumpkinStorage.refreshCache()
    }

    @Test
    fun testUidPumpkinDisabled() {
        mockPumpkin()

        // call for uid
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        blackboxMocker.mockOauth()
        mockOauth("oauth_token")

        assertScreen(
            "/engine/pumpkin/sectionWithErrorsResponse.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = PumpkinConstants.PUMPKIN_MAIN
            }
        )
    }

    @Test
    fun testNoUidPumpkinDisabled() {
        mockPumpkin()

        // request page
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        mockNoOauth()

        assertScreen(
            "/engine/pumpkin/sectionWithErrorsResponse.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = PumpkinConstants.PUMPKIN_MAIN
            }
        )
    }

    @Test
    fun testPumpkinStrategyIsNoDefined() {
        mockPumpkin()

        // enable pumpkin
        infraConfigMocker.setPumpkinEnabled(
            PumpkinService.buildNoUidEnabledProperty(PumpkinConstants.PUMPKIN_MAIN),
            true
        )

        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        mockNoOauth()

        assertScreen(
            "/engine/pumpkin/sectionWithErrorsResponse.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = null
            }
        )
    }

    @Test
    fun testPumpkinLoadedSuccessfully() {
        mockPumpkin()

        // enable pumpkin
        infraConfigMocker.setPumpkinEnabled(
            PumpkinService.buildNoUidEnabledProperty(PumpkinConstants.PUMPKIN_MAIN),
            true
        )

        // request page
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        mockNoOauth()

        assertScreen(
            "/engine/basicCmsTestPageOk.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = PumpkinConstants.PUMPKIN_MAIN
            },
            cleanSnippetType = false
        )
    }

    @Test
    fun testPumpkinWithSectionRefresh() {
        // build pumpkin
        mockPumpkin()

        // enable pumpkin
        infraConfigMocker.setPumpkinEnabled(
            PumpkinService.buildNoUidEnabledProperty(PumpkinConstants.PUMPKIN_MAIN),
            true
        )

        // request page
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json", "pumpkin")
        mockNoOauth()

        // prepare some response (was generated before pumpkin, has raw data)
        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json", "other")
        val someResponse = getScreen { cmsPageType = "other" }

        // refresh first section
        val sectionsToRefresh =
            someResponse.sections.subList(0, 1).mapNotNull { SectionToRefresh.simple(it.rawSection) }
        assertEquals(1, sectionsToRefresh.size)

        val body = MapiScreenRequestBody<Any>().apply {
            sections = sectionsToRefresh
        }

        assertScreen(
            "/engine/pumpkin/noResponse.json",
            getScreen(body) {
                cmsPageType = "pumpkin"
                pumpkinId = PumpkinConstants.PUMPKIN_MAIN
            },
            cleanSnippetType = false
        )
    }

    @Test
    fun testDbHasNoPumpkin() {
        // enable pumpkin
        infraConfigMocker.setPumpkinEnabled(
            PumpkinService.buildNoUidEnabledProperty(PumpkinConstants.PUMPKIN_MAIN),
            true
        )

        // request page
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        mockNoOauth()

        assertScreen(
            "/engine/pumpkin/sectionWithErrorsResponse.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = null
            }
        )
    }

    @Test
    fun testPumpkinWithShowConditions() {
        // build pumpkin
        mockPumpkin("/engine/pumpkin/sectionWithConditionsPumpkin.json")

        // enable pumpkin
        infraConfigMocker.setPumpkinEnabled(
            PumpkinService.buildNoUidEnabledProperty(PumpkinConstants.PUMPKIN_MAIN),
            true
        )

        // request page
        templatorMocker.mockPageResponse("/engine/pumpkin/sectionWithErrors.json")
        mockNoOauth()

        assertScreen(
            "/engine/pumpkin/sectionWithConditionsResponse.json",
            getScreen {
                cmsPageType = "any"
                pumpkinId = PumpkinConstants.PUMPKIN_MAIN
            }
        )
    }
}
