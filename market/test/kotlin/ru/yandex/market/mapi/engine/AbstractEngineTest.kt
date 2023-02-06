package ru.yandex.market.mapi.engine

import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.MapiConstants
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.model.response.MapiScreenRequestBody
import ru.yandex.market.mapi.core.model.screen.ScreenRequest
import ru.yandex.market.mapi.core.model.screen.ScreenResponse
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.core.util.mockMapiContext
import ru.yandex.market.mapi.mock.FapiMocker
import ru.yandex.market.mapi.mock.TemplatorMocker
import ru.yandex.market.mapi.util.MapiErrorTraceWriter
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 09.03.2022
 */
abstract class AbstractEngineTest : AbstractMapiTest() {
    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    lateinit var mapiScreenProcessor: MapiScreenProcessor

    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Autowired
    lateinit var templatorMocker: TemplatorMocker

    @BeforeEach
    fun prepareTests() {
        prepareMapiContext()
    }

    fun getScreenAny(body: MapiScreenRequestBody<*>? = null): ScreenResponse {
        return getScreen(body) {
            cmsPageType = "any"
        }
    }

    fun getScreen(
        body: MapiScreenRequestBody<*>? = null,
        requestConfig: ScreenRequest.() -> Unit = {}
    ): ScreenResponse {
        return mapiScreenProcessor.getScreen(
            ScreenRequest(
                pageToken = null,
                pageSize = MapiConstants.DEF_SCREEN_PAGE_SIZE,
                sectionIds = null
            ).also(requestConfig),
            body
        )
    }

    fun prepareMapiContext() {
        mockMapiContext { context ->
            context.oauth = null
            context.ip = "mocked_ip"
            context.secHeaders = mapOf("sec_header" to "sec_value", "sec_header2" to "sec_value2")
            context.appPlatform = "ISO"
            context.appVersionRaw = "1.42"
        }
    }

    protected fun assertScreen(
        expected: String,
        screen: ScreenResponse,
        cleanSnippetType: Boolean = true,
        keepActions: Boolean = false
    ) {
        // not required in most test, just skip
        screen.debug.remove("rearrFlags")

        screen.analyticParams = null
        screen.sections.forEach { section ->
            //remove interactions to simplify checks
            if (!keepActions) {
                section.actions = null
            }

            // remove to simplify tests
            if (cleanSnippetType) {
                section.content?.forEach { snippet ->
                    snippet.internalType = null
                    snippet.snippetId = ""
                }
            }
        }

        assertJson(screen, expected, "Result json")
    }

    protected fun assertScreenWithInteractions(expected: String, screen: ScreenResponse) {
        // not required in most test, just skip
        screen.debug.remove("rearrFlags")

        screen.sections.forEach { section ->
            // remove to simplify tests
            section.content?.forEach { snippet ->
                snippet.internalType = null
            }
        }

        assertJson(screen, expected, "Result json")
    }

    protected fun assertErrorTrace(expected: String, isInFile: Boolean = true) {
        val context = MapiContext.get()
        val writer = MapiErrorTraceWriter()

        val result = context.traceErrors.joinToString("\n") {
            writer.buildTskv(context, "testId", it)
        }

        log.info("Result trace errors: \n$result")

        val expectedString = if (isInFile) {
            expected.asResource()
        } else expected

        assertEquals(
            expectedString.trim('\n', '\t', ' ')
                .replace("_CRTIME_", (context.crTime / 1000).toString()),
            result
        )
    }
}
